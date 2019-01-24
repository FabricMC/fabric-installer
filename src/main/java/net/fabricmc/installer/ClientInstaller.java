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

package net.fabricmc.installer;

import com.google.gson.JsonObject;
import net.fabricmc.installer.util.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ClientInstaller {

	public static String install(File mcDir, Version version, String loaderVersion, IInstallerProgress progress) throws IOException {
		System.out.println("Installing " + version + " with fabric " + loaderVersion);

		String profileName = String.format("%s-%s-%s", Reference.LOADER_NAME, loaderVersion, version);

		String url = String.format("%s/%s/%s/%s/%3$s-%4$s.json", Reference.MAVEN_SERVER_URL, Reference.PACKAGE, Reference.LOADER_NAME, loaderVersion);
		String fabricInstallMeta = Utils.getUrl(new URL(url));
		JsonObject installMeta = Utils.GSON.fromJson(fabricInstallMeta, JsonObject.class);

		MinecraftLaunchJson launchJson = new MinecraftLaunchJson(installMeta);
		launchJson.id = profileName;
		launchJson.inheritsFrom = version.getMinecraftVersion();

		//Adds loader and the mappings
		launchJson.libraries.add(new MinecraftLaunchJson.Library(Reference.PACKAGE.replaceAll("/", ".") + ":" + Reference.MAPPINGS_NAME + ":" + version.toString(), Reference.MAVEN_SERVER_URL));
		launchJson.libraries.add(new MinecraftLaunchJson.Library(Reference.PACKAGE.replaceAll("/", ".") + ":" + Reference.LOADER_NAME + ":" + loaderVersion, Reference.MAVEN_SERVER_URL));

		File versionsDir = new File(mcDir, "versions");
		File profileDir = new File(versionsDir, profileName);
		File profileJson = new File(profileDir, profileName + ".json");

		if (!profileDir.exists()) {
			profileDir.mkdirs();
		}

		/*

		This is a fun meme

		The vanilla launcher assumes the profile name is the same name as a maven artifact, how ever our profile name is a combination of 2
		(mappings and loader). The launcher will also accept any jar with the same name as the profile, it doesnt care if its empty

		 */
		File dummyJar = new File(profileDir, profileName + ".jar");
		dummyJar.createNewFile();

		Utils.writeToFile(profileJson, Utils.GSON.toJson(launchJson));

		progress.updateProgress("Done");

		return profileName;
	}
}
