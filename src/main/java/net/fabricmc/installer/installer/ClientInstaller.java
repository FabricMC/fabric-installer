package net.fabricmc.installer.installer;

import com.google.gson.*;
import net.fabricmc.installer.util.IInstallerProgress;
import net.fabricmc.installer.util.Translator;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
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
			FileUtils.copyURLToFile(new URL("http://maven.modmuss50.me/net/fabricmc/fabric-base/" + version + "/fabric-base-" + version + ".jar"), fabricJar);
		}
		install(mcDir, version, progress, fabricJar);
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

		File mcJarFile = new File(mcVersionFolder, version + ".jar");
		if (fabricVersionFolder.exists()) {
			progress.updateProgress(Translator.getString("install.client.removeOld"), 10);
			FileUtils.deleteDirectory(fabricVersionFolder);
		}
		fabricVersionFolder.mkdirs();

		progress.updateProgress(Translator.getString("install.client.createJson"), 20);

		String mcJson = FileUtils.readFileToString(mcJarFile, Charset.defaultCharset());

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement jsonElement = new JsonObject();
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		jsonObject.addProperty("id", id);
		jsonObject.addProperty("type", "release");
		jsonObject.addProperty("time", "2016-10-13T15:20:52+01:00");
		jsonObject.addProperty("releaseTime", "2016-09-20T13:40:49+01:00");
		jsonObject.addProperty("mainClass", "net.minecraft.launchwrapper.Launch");
		String args = "--username ${auth_player_name} --version ${version_name} --gameDir ${game_directory} --assetsDir ${assets_root} --assetIndex ${assets_index_name} --uuid ${auth_uuid} --accessToken ${auth_access_token} --userType ${user_type} --tweakClass net.fabricmc.base.launch.FabricClientTweaker";
		jsonObject.addProperty("minecraftArguments", args);
		jsonObject.addProperty("inheritsFrom", version);
		jsonObject.addProperty("jar", version);

		JsonArray libraries = new JsonArray();

		addDep("net.fabricmc:fabric-base:" + attributes.getValue("FabricVersion"), "http://maven.modmuss50.me/", libraries);

		jsonObject.add("libraries", libraries);

		File tempWorkDir = new File(fabricVersionFolder, "temp");
		File depJson = new File(tempWorkDir, "dependencies.json");
		ZipUtil.unpack(fabricJar, tempWorkDir, name -> {
			if (name.startsWith("dependencies.json")) {
				return name;
			} else {
				return null;
			}
		});
		FileReader reader = new FileReader(depJson);
		JsonElement depElement = gson.fromJson(reader, JsonElement.class);
		JsonObject depObject = depElement.getAsJsonObject();
		libraries.addAll(depObject.getAsJsonArray("libraries"));

		FileUtils.write(fabricJsonFile, gson.toJson(jsonElement), "UTF-8");
		reader.close();
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
			return Optional.of(Translator.getString("install.client.error.noMc") + mcVer);
		}
		File versionFolder = new File(versionsFolder, mcVer);
		if (!versionsFolder.exists() || !versionsFolder.isDirectory()) {
			return Optional.of(Translator.getString("install.client.error.noMc") + mcVer);
		}

		File mcJsonFile = new File(versionFolder, mcVer + ".json");
		File mcJarFile = new File(versionFolder, mcVer + ".jar");
		if (!mcJsonFile.exists() || !mcJarFile.exists()) {
			return Optional.of(Translator.getString("install.client.error.noMc") + mcVer);
		}

		//All is ok
		return Optional.empty();
	}
}
