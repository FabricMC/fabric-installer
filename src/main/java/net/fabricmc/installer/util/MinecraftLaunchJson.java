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

package net.fabricmc.installer.util;

import mjson.Json;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MinecraftLaunchJson {

	public String id;
	public String inheritsFrom;
	public String releaseTime = Utils.ISO_8601.format(new Date());
	public String time = releaseTime;
	public String type = "release";
	public String mainClass;
	public transient String mainClassServer;
	public Arguments arguments = new Arguments();
	public List<Library> libraries = new ArrayList<>();

	//Used for reading the fabric-launch.json and populating the minecraft format
	public MinecraftLaunchJson(Json jsonObject) {

		if (!jsonObject.at("mainClass").isObject()) {
			mainClass = jsonObject.at("mainClass").asString();
		} else {
			mainClass = jsonObject.at("mainClass").at("client").asString();
			//Done like this as this object is written to a vanilla profile json
			mainClassServer = jsonObject.at("mainClass").at("server").asString();
		}

		if (jsonObject.has("launchwrapper")) {
			String clientTweaker = jsonObject.at("launchwrapper").at("tweakers").at("client").at(0).asString();

			arguments.game.add("--tweakClass");
			arguments.game.add(clientTweaker);
		}

		String[] validSides = new String[]{"common", "server"};
		Json librariesObject = jsonObject.at("libraries");
		for (String side : validSides) {
			Json librariesArray = librariesObject.at(side);
			librariesArray.asJsonList().forEach(jsonElement -> libraries.add(new Library(jsonElement)));
		}
	}

	public static class Library {

		public String name;
		public String url;

		public Library(String name, String url) {
			this.name = name;
			this.url = url;
		}

		private Library(Json json) {
			name = json.at("name").asString();
			if (json.has("url")) {
				url = json.at("url").asString();
			}
		}

		public String getURL() {
			String path;
			String[] parts = this.name.split(":", 3);
			path = parts[0].replace(".", "/") + "/" + parts[1] + "/" + parts[2] + "/" + parts[1] + "-" + parts[2] + ".jar";
			return url + path;
		}

		public String getPath() {
			String[] parts = this.name.split(":", 3);
			String path = parts[0].replace(".", File.separator) + File.separator + parts[1] + File.separator + parts[2] + File.separator + parts[1] + "-" + parts[2] + ".jar";
			return path.replaceAll(" ", "_");
		}

		public File getFile(File baseDir) {
			return new File(baseDir, getPath());
		}

		public String getFileName() {
			String path = getPath();
			return path.substring(path.lastIndexOf("\\") + 1);
		}
	}

	public static class Arguments {

		public List<String> game = new ArrayList<>();
	}
}
