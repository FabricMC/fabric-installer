/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.installer.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import mjson.Json;

import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Library;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

public class ServerInstaller {
	private static final String servicesDir = "META-INF/services/";
	private static final String manifestPath = "META-INF/MANIFEST.MF";
	public static final String DEFAULT_LAUNCH_JAR_NAME = "fabric-server-launch.jar";

	public static void install(Path dir, String loaderVersion, String gameVersion, InstallerProgress progress) throws IOException {
		Path launchJar = dir.resolve(DEFAULT_LAUNCH_JAR_NAME);
		install(dir, loaderVersion, gameVersion, progress, launchJar);
	}

	public static void install(Path dir, String loaderVersion, String gameVersion, InstallerProgress progress, Path launchJar) throws IOException {
		progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.installing.server")).format(new Object[]{String.format("%s(%s)", loaderVersion, gameVersion)}));

		Files.createDirectories(dir);

		Path libsDir = dir.resolve(".fabric-installer").resolve("libraries");
		Files.createDirectories(libsDir);

		progress.updateProgress(Utils.BUNDLE.getString("progress.download.libraries"));

		URL profileUrl = new URL(Reference.getMetaServerEndpoint(String.format("v2/versions/loader/%s/%s/server/json", gameVersion, loaderVersion)));
		Json json = Json.read(Utils.readTextFile(profileUrl));

		List<Path> libraryFiles = new ArrayList<>();
		String mainClassManifest = null;

		for (Json libraryJson : json.at("libraries").asJsonList()) {
			Library library = new Library(libraryJson);

			progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.download.library.entry")).format(new Object[]{library.name}));
			Path libraryFile = libsDir.resolve(library.getFileName());
			Utils.downloadFile(new URL(library.getURL()), libraryFile);
			libraryFiles.add(libraryFile);

			if (library.name.matches("net\\.fabricmc:fabric-loader:.*")) {
				JarFile jarFile = new JarFile(libraryFile.toFile());
				Manifest manifest = jarFile.getManifest();
				mainClassManifest = manifest.getMainAttributes().getValue("Main-Class");
			}
		}

		progress.updateProgress(Utils.BUNDLE.getString("progress.generating.launch.jar"));

		final String DEFAULT_MAIN_CLASS_MANIFEST = "net.fabricmc.loader.launch.server.FabricServerLauncher";
		mainClassManifest = (mainClassManifest == null) ? DEFAULT_MAIN_CLASS_MANIFEST : mainClassManifest;

		String mainClassMeta = json.at("mainClass").asString();
		makeLaunchJar(launchJar, mainClassMeta, mainClassManifest, libraryFiles, progress);
	}

	private static void makeLaunchJar(Path file, String launchMainClass, String jarMainClass, List<Path> libraryFiles, InstallerProgress progress) throws IOException {
		Files.deleteIfExists(file);

		OutputStream outputStream = Files.newOutputStream(file);
		ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

		Set<String> addedEntries = new HashSet<>();

		{
			addedEntries.add(manifestPath);
			zipOutputStream.putNextEntry(new ZipEntry(manifestPath));

			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(new Attributes.Name("Manifest-Version"), "1.0");
			manifest.getMainAttributes().put(new Attributes.Name("Main-Class"), jarMainClass);
			manifest.write(zipOutputStream);

			zipOutputStream.closeEntry();

			addedEntries.add("fabric-server-launch.properties");
			zipOutputStream.putNextEntry(new ZipEntry("fabric-server-launch.properties"));
			zipOutputStream.write(("launch.mainClass=" + launchMainClass + "\n").getBytes(StandardCharsets.UTF_8));
			zipOutputStream.closeEntry();

			Map<String, Set<String>> services = new HashMap<>();
			byte[] buffer = new byte[32768];

			for (Path f : libraryFiles) {
				progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.generating.launch.jar.library")).format(new Object[]{f.getFileName().toString()}));

				// read service definitions (merging them), copy other files
				try (InputStream is = Files.newInputStream(f);
						JarInputStream jis = new JarInputStream(is)) {
					JarEntry entry;

					while ((entry = jis.getNextJarEntry()) != null) {
						if (entry.isDirectory()) continue;

						String name = entry.getName();

						if (name.startsWith(servicesDir) && name.indexOf('/', servicesDir.length()) < 0) { // service definition file
							parseServiceDefinition(name, jis, services);
						} else if (!addedEntries.add(name)) {
							System.out.printf("duplicate file: %s%n", name);
						} else {
							JarEntry newEntry = new JarEntry(name);
							zipOutputStream.putNextEntry(newEntry);

							int r;

							while ((r = jis.read(buffer, 0, buffer.length)) >= 0) {
								zipOutputStream.write(buffer, 0, r);
							}

							zipOutputStream.closeEntry();
						}
					}
				}
			}

			// write service definitions
			for (Map.Entry<String, Set<String>> entry : services.entrySet()) {
				JarEntry newEntry = new JarEntry(entry.getKey());
				zipOutputStream.putNextEntry(newEntry);

				writeServiceDefinition(entry.getValue(), zipOutputStream);

				zipOutputStream.closeEntry();
			}
		}

		zipOutputStream.close();
		outputStream.close();
	}

	private static void parseServiceDefinition(String name, InputStream rawIs, Map<String, Set<String>> services) throws IOException {
		Collection<String> out = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(rawIs, StandardCharsets.UTF_8));
		String line;

		while ((line = reader.readLine()) != null) {
			int pos = line.indexOf('#');
			if (pos >= 0) line = line.substring(0, pos);
			line = line.trim();

			if (!line.isEmpty()) {
				if (out == null) out = services.computeIfAbsent(name, ignore -> new LinkedHashSet<>());

				out.add(line);
			}
		}
	}

	private static void writeServiceDefinition(Collection<String> defs, OutputStream os) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

		for (String def : defs) {
			writer.write(def);
			writer.write('\n');
		}

		writer.flush();
	}
}
