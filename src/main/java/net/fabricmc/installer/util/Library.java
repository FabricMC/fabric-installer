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

import java.io.File;
import java.nio.file.Path;

import mjson.Json;

public class Library {
	public final String name;
	public final String url;
	public final Path inputPath;

	public Library(String name, String url, Path inputPath) {
		this.name = name;
		this.url = url;
		this.inputPath = inputPath;
	}

	public Library(Json json) {
		name = json.at("name").asString();
		url = json.at("url").asString();
		inputPath = null;
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
