package net.fabricmc.installer.installer;

import com.google.common.io.Resources;
import net.fabricmc.installer.util.IInstallerProgress;
import net.fabricmc.installer.util.Translator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by modmuss50 on 15/05/2017.
 */
public class MultiMCInstaller {

	public static void install(File mcDir, String version, IInstallerProgress progress) throws Exception {
		File instancesDir = new File(mcDir, "instances");
		if (!instancesDir.exists()) {
			throw new FileNotFoundException(Translator.getString("install.multimc.notFound"));
		}
		progress.updateProgress(Translator.getString("install.multimc.findInstances"), 10);
		String mcVer = version.split("-")[0];
		List<File> validInstances = new ArrayList<>();
		for (File instanceDir : instancesDir.listFiles()) {
			if (instanceDir.isDirectory()) {
				if (isValidInstance(instanceDir, mcVer)) {
					validInstances.add(instanceDir);
				}
			}
		}
		if (validInstances.isEmpty()) {
			throw new Exception(Translator.getString("install.multimc.noInstances").replace("[MCVER]", mcVer));
		}
		List<String> instanceNames = new ArrayList<>();
		for (File instance : validInstances) {
			instanceNames.add(instance.getName());
		}
		String instanceName = (String) JOptionPane.showInputDialog(null, Translator.getString("install.multimc.selectInstance"),
			Translator.getString("install.multimc.selectInstance"), JOptionPane.QUESTION_MESSAGE, null,
			instanceNames.toArray(),
			instanceNames.get(0));
		if (instanceName == null) {
			progress.updateProgress(Translator.getString("install.multimc.canceled"), 100);
			return;
		}
		progress.updateProgress(Translator.getString("install.multimc.installingInto").replace("[NAME]", instanceName), 25);
		File instnaceDir = null;
		for (File instance : validInstances) {
			if (instance.getName().equals(instanceName)) {
				instnaceDir = instance;
			}
		}
		if (instnaceDir == null) {
			throw new FileNotFoundException("Could not find " + instanceName);
		}
		File patchesDir = new File(instnaceDir, "patches");
		if (!patchesDir.exists()) {
			patchesDir.mkdir();
		}
		File fabricJar = new File(patchesDir, "Fabric-" + version + ".jar");
		if (!fabricJar.exists()) {
			progress.updateProgress(Translator.getString("install.client.downloadFabric"), 30);
			FileUtils.copyURLToFile(new URL("http://maven.fabricmc.net/net/fabricmc/fabric-base/" + version + "/fabric-base-" + version + ".jar"), fabricJar);
		}
		progress.updateProgress(Translator.getString("install.multimc.createJson"), 70);
		File fabricJson = new File(patchesDir, "fabric.json");
		if (fabricJson.exists()) {
			fabricJson.delete();
		}
		String json = readBaseJson();
		json = json.replaceAll("%VERSION%", version);

		ZipFile fabricZip = new ZipFile(fabricJar);
		ZipEntry dependenciesEntry = fabricZip.getEntry("dependencies.json");
		String fabricDeps = IOUtils.toString(fabricZip.getInputStream(dependenciesEntry), Charset.defaultCharset());
		json = json.replace("%DEPS%", stripDepsJson(fabricDeps.replace("\n", "")));
		FileUtils.writeStringToFile(fabricJson, json, Charset.defaultCharset());
		fabricZip.close();
		progress.updateProgress(Translator.getString("install.success"), 100);
	}

	private static boolean isValidInstance(File instanceDir, String requiredVersion) throws IOException {
		File instanceConfig = new File(instanceDir, "instance.cfg");
		if (!instanceConfig.exists()) {
			return false;
		}
		List<String> lines = FileUtils.readLines(instanceConfig, Charset.defaultCharset());
		String mcVersion = findIntendedVersion(lines);
		if (mcVersion.equals(requiredVersion)) {
			return true;
		}
		return false;
	}

	private static String findIntendedVersion(List<String> input) {
		for (String line : input) {
			if (line.startsWith("IntendedVersion=")) {
				return line.replace("IntendedVersion=", "");
			}
		}
		return "Unknown";
	}

	private static String readBaseJson() throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String string = Resources.toString(classLoader.getResource("multimcPatch.json"), StandardCharsets.UTF_8);
		return string;
	}

	private static String stripDepsJson(String input) {
		Pattern pattern = Pattern.compile("\\[(.*?)\\]");
		Matcher matcher = pattern.matcher(input);
		matcher.find();
		return matcher.group(1);
	}
}
