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

package net.fabricmc.installer.client;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import mjson.Json;

import net.fabricmc.installer.LoaderVersion;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Library;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

public class ClientInstaller {
	public static String install(Path mcDir, String gameVersion, LoaderVersion loaderVersion, InstallerProgress progress) throws IOException {
		System.out.println("Installing " + gameVersion + " with fabric " + loaderVersion.name);

		String profileName = String.format("%s-%s-%s", Reference.LOADER_NAME, loaderVersion.name, gameVersion);

		Path versionsDir = mcDir.resolve("versions");
		Path profileDir = versionsDir.resolve(profileName);
		Path profileJson = profileDir.resolve(profileName + ".json");

		if (!Files.exists(profileDir)) {
			Files.createDirectories(profileDir);
		}

		/*

		This is a fun meme

		The vanilla launcher assumes the profile name is the same name as a maven artifact, how ever our profile name is a combination of 2
		(mappings and loader). The launcher will also accept any jar with the same name as the profile, it doesnt care if its empty

		 */
		Path dummyJar = profileDir.resolve(profileName + ".jar");
		Files.deleteIfExists(dummyJar);
		Files.createFile(dummyJar);

		URL profileUrl = new URL(Reference.getMetaServerEndpoint(String.format("v2/versions/loader/%s/%s/profile/json", gameVersion, loaderVersion.name)));
		Utils.downloadFile(profileUrl, profileJson);

		/*
		Downloading the libraries isn't strictly necessary as the launcher will do it for us.
		Do it anyway in case the launcher fails, we know we have a working connection to maven here.
		 */
		Json json = Json.read(Utils.readString(profileJson));
		Path libsDir = mcDir.resolve("libraries");

		for (Json libraryJson : json.at("libraries").asJsonList()) {
			Library library = new Library(libraryJson);
			Path libraryFile = libsDir.resolve(library.getPath());

			progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.download.library.entry")).format(new Object[]{library.name}));
			Utils.downloadFile(new URL(library.getURL()), libraryFile);
		}

		progress.updateProgress(Utils.BUNDLE.getString("progress.done"));

		return profileName;
	}
}
