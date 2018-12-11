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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ClientInstaller {

	public static void install(File mcDir, String mappingsVersion, String loaderVersion, IInstallerProgress progress) throws IOException {
		System.out.println("Installing " + mappingsVersion + " with fabric " + loaderVersion);
		String[] split = mappingsVersion.split("\\.");

		String profileName = String.format("%s-%s-%s", Reference.LOADER_NAME, loaderVersion, mappingsVersion);

		String url = String.format("%s/%s/%s/%s/%3$s-%4$s.json", Reference.MAVEN_SERVER_URL, Reference.PACKAGE, Reference.LOADER_NAME, loaderVersion);
		String fabricInstallMeta = IOUtils.toString(new URL(url), "UTF-8");
		JsonObject installMeta = Utils.GSON.fromJson(fabricInstallMeta, JsonObject.class);

		MinecraftLaunchJson launchJson = new MinecraftLaunchJson(installMeta);
		launchJson.id = profileName;
		launchJson.inheritsFrom = split[0];

		//Adds loader and the mappings
		launchJson.libraries.add(new MinecraftLaunchJson.Library(Reference.PACKAGE.replaceAll("/", ".") + ":" + Reference.MAPPINGS_NAME + ":" + mappingsVersion, Reference.MAVEN_SERVER_URL));
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
		FileUtils.touch(dummyJar);

		FileUtils.writeStringToFile(profileJson, Utils.GSON.toJson(launchJson), StandardCharsets.UTF_8);

		progress.updateProgress(Translator.getString("install.success"), 100);
	}
}
