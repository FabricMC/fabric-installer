package net.fabricmc.installer;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class ClientInstaller {

	public static void install(File mcDir, String version, File modJar) throws IOException {
		String[] split = version.split("-");
		if (isValidInstallLocation(mcDir, split[0]).isPresent()) {
			throw new RuntimeException(isValidInstallLocation(mcDir, split[0]).get());
		}
		File versionsFolder = new File(mcDir, "versions");
		File fabricVersionFolder = new File(versionsFolder, "fabric-" + version);
		File mcVersionFolder = new File(versionsFolder, split[0]);
		File fabricJsonFile = new File(fabricVersionFolder, "fabric-" + version + ".json");
		File fabricJarFile = new File(fabricVersionFolder, "fabric-" + version + ".jar");
		File mcJarFile = new File(mcVersionFolder, split[0] + ".jar");
		if (fabricVersionFolder.exists()) {
			FileUtils.deleteDirectory(fabricVersionFolder);
		}
		fabricVersionFolder.mkdir();

		FileUtils.copyFile(mcJarFile, fabricJarFile);

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		//TODO download from web
		URL jsonFile = classLoader.getResource("fabric-16w32b-test.json");
		FileUtils.copyURLToFile(jsonFile, fabricJsonFile);

	}

	public static Optional<String> isValidInstallLocation(File mcDir, String mcVer) {
		if (!mcDir.isDirectory()) {
			return Optional.of(mcDir.getName() + " is not a directory");
		}
		File versionsFolder = new File(mcDir, "versions");
		if (!versionsFolder.exists() || !versionsFolder.isDirectory()) {
			return Optional.of("Please launch vanilla Minecraft version " + mcVer);
		}
		File versionFolder = new File(versionsFolder, mcVer);
		if (!versionsFolder.exists() || !versionsFolder.isDirectory()) {
			return Optional.of("Please launch vanilla Minecraft version " + mcVer);
		}

		File mcJsonFile = new File(versionFolder, mcVer + ".json");
		File mcJarFile = new File(versionFolder, mcVer + ".jar");
		if (!mcJsonFile.exists() || !mcJarFile.exists()) {
			return Optional.of("Please launch vanilla Minecraft version " + mcVer);
		}

		//All is ok
		return Optional.empty();
	}

}
