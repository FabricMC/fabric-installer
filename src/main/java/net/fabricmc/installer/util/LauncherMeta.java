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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LauncherMeta {

	private static LauncherMeta launcherMeta = null;

	public static LauncherMeta getLauncherMeta() throws IOException {
		if(launcherMeta == null){
			launcherMeta = load();
		}
		return launcherMeta;
	}

	private static LauncherMeta load() throws IOException {
		URL url = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
		URLConnection conn = url.openConnection();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			return Utils.GSON.fromJson(reader, LauncherMeta.class);
		}
	}

	public List<Version> versions;

	public static class Version {
		public String id;
		public String type;
		public String url;
		public String time;
		public String releaseTime;

		private transient VersionMeta versionMeta = null;

		public VersionMeta getVersionMeta() throws IOException {
			if(versionMeta == null){
				URL url = new URL(this.url);
				URLConnection conn = url.openConnection();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
					versionMeta = Utils.GSON.fromJson(reader, VersionMeta.class);
				}
			}
			return versionMeta;
		}
	}

	public Version getVersion(String version){
		return versions.stream().filter(v -> v.id.equals(version)).findFirst().orElse(null);
	}

}
