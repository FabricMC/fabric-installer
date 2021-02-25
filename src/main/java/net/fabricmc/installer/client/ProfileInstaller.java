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

import mjson.Json;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

public class ProfileInstaller {

	public static void setupProfile(Path path, String name, String gameVersion) throws IOException {
		Path launcherProfiles = path.resolve("launcher_profiles.json");
		if (!Files.exists(launcherProfiles)) {
			System.out.println("Could not find launcher_profiles");
			return;
		}

		System.out.println("Creating profile");

		Json jsonObject = Json.read(Utils.readString(launcherProfiles));

		Json profiles = jsonObject.at("profiles");
		String profileName = Reference.LOADER_NAME + "-" + gameVersion;

		Json profile;
		if (profiles.has(profileName)) {
			profile = profiles.at(profileName);
		} else {
			profile = createProfile(profileName);
		}

		profile.set("lastVersionId", name);
		profiles.set(profileName, profile);

		Utils.writeToFile(launcherProfiles, jsonObject.toString());
	}

	private static Json createProfile(String name) {
		Json jsonObject = Json.object();
		jsonObject.set("name", name);
		jsonObject.set("type", "custom");
		jsonObject.set("created", Utils.ISO_8601.format(new Date()));
		jsonObject.set("lastUsed", Utils.ISO_8601.format(new Date()));
		jsonObject.set("icon", Utils.getProfileIcon());
		return jsonObject;
	}

}
