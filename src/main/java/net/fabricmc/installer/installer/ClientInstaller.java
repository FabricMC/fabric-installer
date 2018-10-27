/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
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

package net.fabricmc.installer.installer;

import com.google.gson.*;
import net.fabricmc.installer.util.*;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class ClientInstaller {

	public static void install(File mcDir, String version, IInstallerProgress progress) throws IOException {
		String[] split = version.split("-");
		if (isValidInstallLocation(mcDir, split[0]).isPresent()) {
			throw new RuntimeException(isValidInstallLocation(mcDir, split[0]).get());
		}
		File fabricData = new File(mcDir, "fabricData");
		File fabricJar = new File(fabricData, version + ".jar");
		if (!fabricJar.exists()) {
			progress.updateProgress(Translator.getString("install.client.downloadFabric"), 10);
			FileUtils.copyURLToFile(new URL(MavenHandler.getPath(Reference.MAVEN_SERVER_URL, Reference.PACKAGE_FABRIC, Reference.NAME_FABRIC_LOADER, version)), fabricJar);
		}
		JarFile jarFile = new JarFile(fabricJar);
		Attributes attributes = jarFile.getManifest().getMainAttributes();
		String mcVersion = attributes.getValue("MinecraftVersion");
		install(mcDir, mcVersion, progress, fabricJar);
		FileUtils.deleteDirectory(fabricData);
	}

	public static void install(File mcDir, String version, IInstallerProgress progress, File fabricJar) throws IOException {
		progress.updateProgress(Translator.getString("gui.installing") + ": " + version, 0);
		JarFile jarFile = new JarFile(fabricJar);
		Attributes attributes = jarFile.getManifest().getMainAttributes();

		String id = "fabric-" + attributes.getValue("FabricVersion");

		System.out.println(Translator.getString("gui.installing") + " " + id);
		File versionsFolder = new File(mcDir, "versions");
		File fabricVersionFolder = new File(versionsFolder, id);
		File mcVersionFolder = new File(versionsFolder, version);
		File fabricJsonFile = new File(fabricVersionFolder, id + ".json");

		File tempWorkDir = new File(fabricVersionFolder, "temp");
		ZipUtil.unpack(fabricJar, tempWorkDir, name -> {
			if (name.startsWith(Reference.INSTALLER_METADATA_FILENAME)) {
				return name;
			} else {
				return null;
			}
		});
		InstallerMetadata metadata = new InstallerMetadata(tempWorkDir);

		File mcJarFile = new File(mcVersionFolder, version + ".jar");
		if (fabricVersionFolder.exists()) {
			progress.updateProgress(Translator.getString("install.client.removeOld"), 10);
			FileUtils.deleteDirectory(fabricVersionFolder);
		}
		fabricVersionFolder.mkdirs();

		progress.updateProgress(Translator.getString("install.client.createJson"), 20);

		String mcJson = FileUtils.readFileToString(mcJarFile, Charset.defaultCharset());

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonObject versionJson = new JsonObject();
		versionJson.addProperty("id", id);
		versionJson.addProperty("type", "release");
		versionJson.addProperty("time", Utils.ISO_8601.format(fabricJar.lastModified()));
		versionJson.addProperty("releaseTime", Utils.ISO_8601.format(fabricJar.lastModified()));
		versionJson.addProperty("mainClass", metadata.getMainClass());
		versionJson.addProperty("inheritsFrom", version);

		JsonArray gameArgs = new JsonArray();
		JsonObject arguments = new JsonObject();

		List<String> metadataTweakers = metadata.getTweakers("client", "common");
		if (metadataTweakers.size() > 1) {
			throw new RuntimeException("Not supporting > 1 tweaker yet!");
		}

		metadata.getArguments("client", "common").forEach(gameArgs::add);
		gameArgs.add("--tweakClass");
		gameArgs.add(metadataTweakers.get(0));

		arguments.add("game", gameArgs);
		versionJson.add("arguments", arguments);

		JsonArray libraries = new JsonArray();

		addDep(Reference.PACKAGE_FABRIC + ":" + Reference.NAME_FABRIC_LOADER + ":" + attributes.getValue("FabricVersion"), "http://maven.modmuss50.me/", libraries);

		for (InstallerMetadata.LibraryEntry entry : metadata.getLibraries("client", "common")) {
			libraries.add(entry.toVanillaEntry());
		}

		versionJson.add("libraries", libraries);

		FileUtils.write(fabricJsonFile, gson.toJson(versionJson), "UTF-8");
		jarFile.close();
		progress.updateProgress(Translator.getString("install.client.cleanDir"), 90);
		FileUtils.deleteDirectory(tempWorkDir);

		progress.updateProgress(Translator.getString("install.success"), 100);
	}

	public static void addDep(String dep, String maven, JsonArray jsonArray) {
		JsonObject object = new JsonObject();
		object.addProperty("name", dep);
		if (!maven.isEmpty()) {
			object.addProperty("url", maven);
		}
		jsonArray.add(object);
	}

	public static Optional<String> isValidInstallLocation(File mcDir, String mcVer) {
		if (!mcDir.isDirectory()) {
			return Optional.of(mcDir.getName() + " " + Translator.getString("install.client.error.noDir"));
		}
		File versionsFolder = new File(mcDir, "versions");
		if (!versionsFolder.exists() || !versionsFolder.isDirectory()) {
			return Optional.of(Translator.getString("install.client.error.noLauncher") + mcVer);
		}
/*		File versionFolder = new File(versionsFolder, mcVer);
		if (!versionsFolder.exists() || !versionsFolder.isDirectory()) {
			return Optional.of(Translator.getString("install.client.error.noMc") + mcVer);
		}

		File mcJsonFile = new File(versionFolder, mcVer + ".json");
		File mcJarFile = new File(versionFolder, mcVer + ".jar");
		if (!mcJsonFile.exists() || !mcJarFile.exists()) {
			return Optional.of(Translator.getString("install.client.error.noMc") + mcVer);
		} */

		return Optional.empty();
	}
}
