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

package net.fabricmc.installer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipError;

import net.fabricmc.installer.server.MinecraftServerDownloader;
import net.fabricmc.installer.server.ServerInstaller;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Utils;

public final class ServerLauncher {
	private static final String INSTALL_CONFIG_NAME = "install.properties";
	private static final Path DATA_DIR = Paths.get(".fabric", "server");

	public static void main(String[] args) throws Throwable {
		LaunchData launchData;

		try {
			launchData = initialise();
		} catch (IOException e) {
			throw new RuntimeException("Failed to setup fabric server", e);
		}

		Objects.requireNonNull(launchData, "launchData is null, cannot proceed");

		// Set the game jar path to bypass loader's own lookup
		System.setProperty("fabric.gameJarPath", launchData.serverJar.toAbsolutePath().toString());

		@SuppressWarnings("resource")
		URLClassLoader launchClassLoader = new URLClassLoader(new URL[]{launchData.launchJar.toUri().toURL()});

		// Use method handle to keep the stacktrace clean
		MethodHandle handle = MethodHandles.publicLookup().findStatic(launchClassLoader.loadClass(launchData.mainClass), "main", MethodType.methodType(void.class, String[].class));
		handle.invokeExact(args);
	}

	// Validates and downloads/installs the server if required
	private static LaunchData initialise() throws IOException {
		Properties properties = readProperties();

		String customLoaderPath = System.getProperty("fabric.customLoaderPath"); // intended for testing and development
		LoaderVersion loaderVersion;

		if (customLoaderPath == null) {
			loaderVersion = new LoaderVersion(Objects.requireNonNull(properties.getProperty("fabric-loader-version"), "no loader-version specified in " + INSTALL_CONFIG_NAME));
		} else {
			loaderVersion = new LoaderVersion(Paths.get(customLoaderPath));
		}

		String gameVersion = Objects.requireNonNull(properties.getProperty("game-version"), "no game-version specified in " + INSTALL_CONFIG_NAME);

		// 0.12 or higher is required
		validateLoaderVersion(loaderVersion);

		Path baseDir = Paths.get(".").toAbsolutePath().normalize();
		Path dataDir = baseDir.resolve(DATA_DIR);

		// Vanilla server jar
		String customServerJar = System.getProperty("fabric.installer.server.gameJar", null);
		Path serverJar = customServerJar == null ? dataDir.resolve(String.format("%s-server.jar", gameVersion)) : Paths.get(customServerJar);
		// Includes the mc version as this jar contains intermediary
		Path serverLaunchJar = dataDir.resolve(String.format("fabric-loader-server-%s-minecraft-%s.jar", loaderVersion.name, gameVersion));

		if (!Files.exists(serverJar)) {
			InstallerProgress.CONSOLE.updateProgress(Utils.BUNDLE.getString("progress.download.minecraft"));
			MinecraftServerDownloader downloader = new MinecraftServerDownloader(gameVersion);
			downloader.downloadMinecraftServer(serverJar);
		}

		if (Files.exists(serverLaunchJar)) { // install exists, verify libs exist and determine main class
			try {
				List<Path> classPath = new ArrayList<>();
				String mainClass = readManifest(serverLaunchJar, classPath);
				boolean allPresent = true;

				for (Path file : classPath) {
					if (!Files.exists(file)) {
						allPresent = false;
						break;
					}
				}

				if (allPresent) {
					// All seems good, no need to reinstall
					return new LaunchData(serverJar, serverLaunchJar, mainClass);
				} else {
					System.err.println("Detected incomplete install, reinstalling");
				}
			} catch (IOException | ZipError e) {
				// Wont throw here, will try to reinstall
				System.err.println("Failed to analyze or verify existing install: " + e.getMessage());
			}
		}

		Files.createDirectories(dataDir);
		ServerInstaller.install(baseDir, loaderVersion, gameVersion, InstallerProgress.CONSOLE, serverLaunchJar);

		String mainClass = readManifest(serverLaunchJar, null);

		return new LaunchData(serverJar, serverLaunchJar, mainClass);
	}

	private static Properties readProperties() throws IOException {
		Properties properties = new Properties();

		URL config = getConfigFromResources();

		if (config == null) {
			throw new RuntimeException("Jar does not contain unattended install.properties file");
		}

		try (InputStreamReader reader = new InputStreamReader(config.openStream(), StandardCharsets.UTF_8)) {
			properties.load(reader);
		} catch (IOException e) {
			throw new IOException("Failed to read " + INSTALL_CONFIG_NAME, e);
		}

		return properties;
	}

	// Find the mainclass of a jar file
	private static String readManifest(Path path, List<Path> classPathOut) throws IOException {
		try (JarFile jarFile = new JarFile(path.toFile())) {
			Manifest manifest = jarFile.getManifest();
			String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);

			if (mainClass == null) {
				throw new IOException("Jar does not have a Main-Class attribute");
			}

			if (classPathOut != null) {
				String cp = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);

				StringTokenizer tokenizer = new StringTokenizer(cp);
				URL baseUrl = path.toUri().toURL();

				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					URL url = new URL(baseUrl, token);

					try {
						classPathOut.add(Paths.get(url.toURI()));
					} catch (URISyntaxException e) {
						throw new IOException(String.format("invalid class path entry in %s manifest: %s", path, token));
					}
				}
			}

			return mainClass;
		}
	}

	private static void validateLoaderVersion(LoaderVersion loaderVersion) {
		if (Utils.compareVersions(loaderVersion.name, "0.12") < 0) { // loader version below 0.12
			throw new UnsupportedOperationException("Fabric loader 0.12 or higher is required for unattended server installs. Please use a newer fabric loader version, or the full installer.");
		}
	}

	private static URL getConfigFromResources() {
		return ServerLauncher.class.getClassLoader().getResource(INSTALL_CONFIG_NAME);
	}

	private static class LaunchData {
		final Path serverJar;
		final Path launchJar;
		final String mainClass;

		private LaunchData(Path serverJar, Path launchJar, String mainClass) {
			this.serverJar = Objects.requireNonNull(serverJar, "serverJar");
			this.launchJar = Objects.requireNonNull(launchJar, "launchJar");
			this.mainClass = Objects.requireNonNull(mainClass, "mainClass");
		}
	}
}
