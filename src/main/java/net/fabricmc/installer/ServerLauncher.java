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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipError;

import net.fabricmc.installer.server.MinecraftServerDownloader;
import net.fabricmc.installer.server.ServerInstaller;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Utils;

public final class ServerLauncher {
	private static final String INSTALL_CONFIG_NAME = "install.properties";
	private static final Path SERVER_DIR = Paths.get(".fabric", "server").toAbsolutePath();

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

		URLClassLoader launchClassLoader = new URLClassLoader(new URL[]{launchData.launchJar.toUri().toURL()});

		// Use method handle to keep the stacktrace clean
		MethodHandle handle = MethodHandles.publicLookup().findStatic(launchClassLoader.loadClass(launchData.mainClass), "main", MethodType.methodType(void.class, String[].class));
		handle.invokeExact(args);
	}

	// Validates and downloads/installs the server if required
	private static LaunchData initialise() throws IOException {
		Properties properties = readProperties();

		LoaderVersion loaderVersion = new LoaderVersion(Objects.requireNonNull(properties.getProperty("fabric-loader-version"), "no loader-version specified in " + INSTALL_CONFIG_NAME));
		String gameVersion = Objects.requireNonNull(properties.getProperty("game-version"), "no game-version specified in " + INSTALL_CONFIG_NAME);

		// 0.12 or higher is required
		validateLoaderVersion(loaderVersion);

		// Vanilla server jar
		Path serverJar = SERVER_DIR.resolve(String.format("%s-server.jar", gameVersion));
		// Includes the mc version as this jar contains intermediary
		Path serverLaunchJar = SERVER_DIR.resolve(String.format("fabric-loader-server-%s-minecraft-%s.jar", loaderVersion.name, gameVersion));

		if (Files.exists(serverJar) && Files.exists(serverLaunchJar)) {
			try {
				String mainClass = readMainClass(serverLaunchJar);
				// All seems good, no need to reinstall
				return new LaunchData(serverJar, serverLaunchJar, mainClass);
			} catch (IOException | ZipError e) {
				// Wont throw here, will try to reinstall
				System.err.println("Failed to read main class from server launch jar: " + e.getMessage());
			}
		}

		Files.createDirectories(SERVER_DIR);
		ServerInstaller.install(SERVER_DIR, loaderVersion, gameVersion, InstallerProgress.CONSOLE, serverLaunchJar);

		InstallerProgress.CONSOLE.updateProgress(Utils.BUNDLE.getString("progress.download.minecraft"));
		MinecraftServerDownloader downloader = new MinecraftServerDownloader(gameVersion);
		downloader.downloadMinecraftServer(serverJar);

		String mainClass = readMainClass(serverLaunchJar);

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
	private static String readMainClass(Path path) throws IOException {
		try (JarFile jarFile = new JarFile(path.toFile())) {
			Manifest manifest = jarFile.getManifest();
			String mainClass = manifest.getMainAttributes().getValue("Main-Class");

			if (mainClass == null) {
				throw new IOException("Jar does not have a Main-Class attribute");
			}

			return mainClass;
		}
	}

	private static void validateLoaderVersion(LoaderVersion loaderVersion) {
		String[] versionSplit = loaderVersion.name.split("\\.");

		// future 1.x versions
		if (Integer.parseInt(versionSplit[0]) > 0) {
			return;
		}

		// 0.12.x or newer
		if (Integer.parseInt(versionSplit[1]) >= 12) {
			return;
		}

		throw new UnsupportedOperationException("Fabric loader 0.12 or higher is required for unattended server installs. Please use a newer fabric loader version, or the full installer.");
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
