package net.fabricmc.installer;

import com.google.gson.*;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.mapping.MappingsEnigmaReader;
import cuchaz.enigma.throwables.MappingParseException;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class ClientInstaller {

	public static void install(File mcDir, String version, File file, GuiController controller) throws IOException, MappingParseException {
		controller.progressBar.setProgress(0);
		controller.installButton.setDisable(true);
		String[] split = version.split("-");
		if (isValidInstallLocation(mcDir, split[0]).isPresent()) {
			throw new RuntimeException(isValidInstallLocation(mcDir, split[0]).get());
		}
		File fabricData = new File(mcDir, "fabricData");
		if (file == null) {
			File fabricJar = new File(fabricData, version + ".jar");
			if (fabricJar.exists()) {
				file = fabricJar;
			}
			if (file == null) {
				controller.setText("Downloading fabric...");
				FileUtils.copyURLToFile(new URL("http://maven.fabricmc.net/net/fabricmc/fabric-base/" + version + "/fabric-base-" + version + ".jar"), fabricJar);
				file = fabricJar;
			}
		}
		JarFile jarFile = new JarFile(file);
		Attributes attributes = jarFile.getManifest().getMainAttributes();

		String id = "fabric-" + attributes.getValue("FabricVersion");
		System.out.println("Installing " + id);
		File versionsFolder = new File(mcDir, "versions");
		File fabricVersionFolder = new File(versionsFolder, id);
		File mcVersionFolder = new File(versionsFolder, split[0]);
		File fabricJsonFile = new File(fabricVersionFolder, id + ".json");
		File fabricJarFile = new File(fabricVersionFolder, id + ".jar");
		File mcJsonFile = new File(mcVersionFolder, split[0] + ".json");
		File mcJarFile = new File(mcVersionFolder, split[0] + ".jar");
		if (fabricVersionFolder.exists()) {
			controller.setText("Removing old fabric version");
			FileUtils.deleteDirectory(fabricVersionFolder);
		}
		fabricVersionFolder.mkdirs();

		FileUtils.copyFile(mcJsonFile, fabricJsonFile);
		controller.progressBar.setProgress(0.2);

		controller.setText("Creating version Json File");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonElement jsonElement = gson.fromJson(new FileReader(fabricJsonFile), JsonElement.class);
		JsonObject jsonObject = jsonElement.getAsJsonObject();
		jsonObject.remove("id");
		jsonObject.addProperty("id", id);

		jsonObject.remove("mainClass");
		jsonObject.addProperty("mainClass", "net.minecraft.launchwrapper.Launch");

		jsonObject.remove("downloads");

		String args = jsonObject.get("minecraftArguments").getAsString();
		jsonObject.remove("minecraftArguments");
		jsonObject.addProperty("minecraftArguments", args + " --tweakClass net.fabricmc.base.launch.FabricClientTweaker");

		JsonArray librarys = jsonObject.getAsJsonArray("libraries");
		JsonObject object = new JsonObject();
		object.addProperty("name", "net.minecraft:launchwrapper:1.11");
		librarys.add(object);

		object = new JsonObject();
		object.addProperty("name", "org.ow2.asm:asm-all:5.0.3");
		librarys.add(object);

		addDep("net.fabricmc:fabric-base:" + attributes.getValue("FabricVersion"), "http://maven.fabricmc.net/", librarys);

		addDep("org.spongepowered:mixin:0.5.11-SNAPSHOT", "http://repo.spongepowered.org/maven", librarys);

		object = new JsonObject();
		object.addProperty("name", "org.spongepowered:mixin:0.5.11-SNAPSHOT");
		JsonObject downloads = new JsonObject();
		JsonObject artifact = new JsonObject();
		artifact.addProperty("url", "https://repo.spongepowered.org/maven/org/spongepowered/mixin/0.5.11-SNAPSHOT/mixin-0.5.11-20160705.154945-3.jar");
		artifact.addProperty("sha1", "451a47ea3e41b5aa68189761b9343189a969d64f");
		downloads.add("artifact", artifact);
		object.add("downloads", downloads);

		librarys.add(object);

		FileUtils.write(fabricJsonFile, gson.toJson(jsonElement), "UTF-8");
		controller.progressBar.setProgress(0.4);
		controller.setText("Creating temporary directory for files");
		File tempWorkDir = new File(fabricVersionFolder, "temp");
		if (tempWorkDir.exists()) {
			FileUtils.deleteDirectory(tempWorkDir);
		}
		controller.setText("Extracting Mappings");
		ZipUtil.unpack(file, tempWorkDir, name -> {
			if (name.startsWith("pomf-" + split[0])) {
				return name;
			} else {
				return null;
			}
		});

		File mappingsDir = new File(tempWorkDir, "pomf-" + split[0] + File.separator + "mappings");
		File tempAssests = new File(tempWorkDir, "assets");
		controller.progressBar.setProgress(0.6);
		controller.setText("Deobfuscating minecraft jar file");
		Deobfuscator deobfuscator = new Deobfuscator(new JarFile(mcJarFile));
		controller.progressBar.setProgress(0.65);
		deobfuscator.setMappings(new MappingsEnigmaReader().read(mappingsDir));
		controller.progressBar.setProgress(0.7);
		deobfuscator.writeJar(fabricJarFile, new ProgressListener());
		controller.progressBar.setProgress(0.8);
		//cleans jar file and copys needed things back in
		controller.setText("Cleaning minecraft jar file");
		ZipUtil.unpack(mcJarFile, tempAssests, name -> {
			if (name.startsWith("assets") || name.startsWith("log4j2.xml")) {
				return name;
			} else {
				return null;
			}
		});
		ZipUtil.unpack(fabricJarFile, tempAssests);
		ZipUtil.pack(tempAssests, fabricJarFile);

		controller.progressBar.setProgress(0.9);

		controller.setText("Removing temp directory");
		FileUtils.deleteDirectory(tempWorkDir);
		controller.progressBar.setProgress(1);
		controller.setText("Done!");
		controller.installButton.setDisable(false);
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

	public static class ProgressListener implements Deobfuscator.ProgressListener {
		@Override
		public void init(int i, String s) {

		}

		@Override
		public void onProgress(int i, String s) {

		}
	}

}
