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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.installer.util.IInstallerProgress;
import net.fabricmc.installer.util.Translator;
import net.fabricmc.installer.util.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ServerInstaller {


	public static void install(File mcDir, String version, IInstallerProgress progress) throws IOException {
		File fabricJar = new File(mcDir, "fabric-" + version + ".jar");
		if (fabricJar.exists()) {
			fabricJar.delete();
		}

		progress.updateProgress(Translator.getString("install.server.downloadFabric"), 5);
		FileUtils.copyURLToFile(new URL("http://maven.modmuss50.me/net/fabricmc/fabric-base/" + version + "/fabric-base-" + version + ".jar"), fabricJar);
		install(mcDir, version, progress, fabricJar);
	}

	public static void install(File mcDir, String version, IInstallerProgress progress, File fabricJar) throws IOException {
		progress.updateProgress(Translator.getString("gui.installing") + ": " + version, 0);
		String[] split = version.split("-");
		String mcVer = split[0];
		String fabricVer = split[1];

		File mcJar = new File(mcDir, "minecraft_server." + mcVer + ".jar");

		if(!mcJar.exists()){
			progress.updateProgress(Translator.getString("install.server.downloadVersionList"), 10);
			JsonObject versionList = Utils.loadRemoteJSON(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json"));
			String url = null;

			for (JsonElement element : versionList.getAsJsonArray("versions")) {
				JsonObject object = element.getAsJsonObject();
				if (object.get("id").getAsString().equals(mcVer)) {
					url = object.get("url").getAsString();
					break;
				}
			}

			if (url == null) {
				throw new RuntimeException(Translator.getString("install.server.error.noVersion"));
			}

			progress.updateProgress(Translator.getString("install.server.downloadServerInfo"), 12);
			JsonObject serverInfo = Utils.loadRemoteJSON(new URL(url));
			url = serverInfo.getAsJsonObject("downloads").getAsJsonObject("server").get("url").getAsString();

			progress.updateProgress(Translator.getString("install.server.downloadServer"), 15);
			FileUtils.copyURLToFile(new URL(url), mcJar);
		}

		File libs = new File(mcDir, "libs");

		ZipFile fabricZip = new ZipFile(fabricJar);
		ZipEntry dependenciesEntry = fabricZip.getEntry("dependencies_server.txt");
		List<String> fabricDeps = IOUtils.readLines(fabricZip.getInputStream(dependenciesEntry), Charset.defaultCharset());
		for (int i = 0; i < fabricDeps.size(); i++) {
			String dep = fabricDeps.get(i);
			String[] depSplit = dep.split("/");
			File depFile = new File(libs, depSplit[depSplit.length -1]);
			if(depFile.exists()){
				depFile.delete();
			}
			progress.updateProgress("Downloading " + depFile.getName(), 20 + (i * 70 / fabricDeps.size()));
			FileUtils.copyURLToFile(new URL(dep), depFile);
		}
		
		progress.updateProgress(Translator.getString("install.success"), 100);
	}

}
