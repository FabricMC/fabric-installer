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

package net.fabricmc.installer.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InstallerMetadata {
	private static final Gson GSON = new Gson();
	private final JsonObject root;

	public InstallerMetadata(File workDir) throws IOException  {
		File file = new File(workDir, Reference.INSTALLER_METADATA_FILENAME);
		try (FileInputStream inputStream = new FileInputStream(file)) {
			try (InputStreamReader reader = new InputStreamReader(inputStream)) {
				root = GSON.fromJson(reader, JsonObject.class);
			}
		}
	}

	public InstallerMetadata(InputStream inputStream) throws IOException  {
		try (InputStreamReader reader = new InputStreamReader(inputStream)) {
			root = GSON.fromJson(reader, JsonObject.class);
		}
	}

	public int getVersion() {
		return root.get("version").getAsInt();
	}

	public String getMainClass() {
		return root.get("mainClass").getAsString();
	}

	public List<LibraryEntry> getLibraries(String... sides) {
		return getObjectsSubKeyed(root, "libraries", sides).map((e) -> {
			JsonObject o = e.getAsJsonObject();
			return new LibraryEntry(o.get("name").getAsString(), o.has("url") ? o.get("url").getAsString() : null);
		}).collect(Collectors.toList());
	}

	public List<String> getArguments(String... sides) {
		return getObjectsSubKeyed(root, "arguments", sides).map(JsonElement::getAsString).collect(Collectors.toList());
	}

	public List<String> getTweakers(String... sides) {
		if (!root.has("launchwrapper")) {
			return Collections.emptyList();
		}

		return getObjectsSubKeyed(root.getAsJsonObject("launchwrapper"), "tweakers", sides).map(JsonElement::getAsString).collect(Collectors.toList());
	}

	private Stream<JsonElement> getObjectsSubKeyed(JsonObject baseObj, String key, String... subKeys) {
		if (!baseObj.has(key)) {
			return Stream.empty();
		}

		List<JsonElement> list = new ArrayList<>();
		JsonObject keyObj = baseObj.getAsJsonObject(key);

		for (String s : subKeys) {
			if (keyObj.has(s)) {
				JsonArray array = keyObj.getAsJsonArray(s);
				for (JsonElement element : array) {
					list.add(element);
				}
			}
		}

		return list.stream();
	}

	public static class LibraryEntry {
		private final String name, url;

		public LibraryEntry(String name, String url) {
			this.name = name;
			this.url = url != null ? url : Reference.MAVEN_DEFAULT_LIBRARY_URL;
		}

		public String getName() {
			return name;
		}

		public String getFilename() {
			String[] splitName = name.split(":");
			if (splitName.length != 3) {
				throw new RuntimeException("Unsupported dependency " + name);
			}

			return splitName[1] + "-" + splitName[2] + ".jar";
		}

		public String getFullURL() {
			String[] splitName = name.split(":");
			if (splitName.length != 3) {
				throw new RuntimeException("Unsupported dependency " + name);
			}

			return MavenHandler.getPath(getURL(), splitName[0], splitName[1], splitName[2]);
		}

		public String getURL() {
			return url;
		}

		public JsonObject toVanillaEntry() {
			JsonObject object = new JsonObject();
			object.addProperty("name", name);
			object.addProperty("url", url);
			return object;
		}
	}
}
