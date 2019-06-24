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

import com.google.gson.JsonObject;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class ProfileInstaller {

	public static void setupProfile(File path, String name, String gameVersion) throws IOException {
		File launcherProfiles = new File(path, "launcher_profiles.json");
		if (!launcherProfiles.exists()) {
			System.out.println("Could not find launcher_profiles");
			return;
		}

		System.out.println("Creating profile");

		String json = Utils.readFile(launcherProfiles);
		JsonObject jsonObject = Utils.GSON.fromJson(json, JsonObject.class);
		JsonObject profiles = jsonObject.getAsJsonObject("profiles");
		String profileName = Reference.LOADER_NAME + "-" + gameVersion;

		JsonObject profile;
		if (profiles.has(profileName)) {
			profile = profiles.get(profileName).getAsJsonObject();
		} else {
			profile = createProfile(profileName);
		}

		profile.addProperty("lastVersionId", name);
		profiles.add(profileName, profile);

		Utils.writeToFile(launcherProfiles, Utils.GSON.toJson(jsonObject));

	}

	private static JsonObject createProfile(String name) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("name", name);
		jsonObject.addProperty("type", "custom");
		jsonObject.addProperty("created", Utils.ISO_8601.format(new Date()));
		jsonObject.addProperty("lastUsed", Utils.ISO_8601.format(new Date()));
		jsonObject.addProperty("icon", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAMAAAD04JH5AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAMAUExURQAAADg0KoB6bZqSfq6mlLyynMa8pdvQtAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAKBnqZkAAAEAdFJOU////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////wBT9wclAAAACXBIWXMAAC4iAAAuIgGq4t2SAAAAB3RJTUUH4godCS074WsPjgAAABh0RVh0U29mdHdhcmUAcGFpbnQubmV0IDQuMS42/U4J6AAAAShJREFUeF7t1sFuwjAQBmEMLbz/E6f/YWR7JYeUgNQeZo62tfudoly2D9R6HLyQAAECBLwDYGlrjx4HiTeHCRAgQMBHAPcejsRV4vVOAgQIEPA6gLGJVfOy7x5XiavEhJIAAQIE/BbAiMQ3L7FgjjcJR+IqcZWYmgQIECDgOYDniRElHEvJksJBSYAAAQJWAK4Sw47CUWJnYla69jhIAgQIEPCPABwkxpYYmzhYx84SU0vsTAIECBBw4aS1rx7DdsIxx01iZ2JqYtNOAgQIEDAAtx6OxNidWDXHrMT8wwQIECBg9UuGI+FI7Cyx9MzakQABAgQMwIhhCUfCkViaeHRm7UiAAAECVoARCxKOxMEcr08lQIAAAc8BI1aVuHovAQIECPhjwLb9AB2oYv/9NJFkAAAAAElFTkSuQmCC");
		return jsonObject;
	}

}
