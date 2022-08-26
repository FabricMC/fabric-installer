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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import mjson.Json;

public class LauncherMeta {
	private static LauncherMeta launcherMeta = null;

	public static LauncherMeta getLauncherMeta() throws IOException {
		if (launcherMeta == null) {
			launcherMeta = load();
		}

		return launcherMeta;
	}

	private static LauncherMeta load() throws IOException {
		List<Version> versions = new ArrayList<>();
		versions.addAll(getVersionsFromUrl(Reference.MINECRAFT_LAUNCHER_MANIFEST));
		versions.addAll(getVersionsFromUrl(Reference.EXPERIMENTAL_LAUNCHER_MANIFEST));

		return new LauncherMeta(versions);
	}

	private static List<Version> getVersionsFromUrl(String url) throws IOException {
		Json json = FabricService.queryJsonSubstitutedMaven(url);

		List<Version> versions = json.at("versions").asJsonList()
				.stream()
				.map(Version::new)
				.collect(Collectors.toList());

		return versions;
	}

	public final List<Version> versions;

	public LauncherMeta(List<Version> versions) {
		this.versions = versions;
	}

	public static class Version {
		public final String id;
		public final String url;

		private VersionMeta versionMeta = null;

		public Version(Json json) {
			this.id = json.at("id").asString();
			this.url = json.at("url").asString();
		}

		public VersionMeta getVersionMeta() throws IOException {
			if (versionMeta == null) {
				Json json = FabricService.queryJsonSubstitutedMaven(url);
				versionMeta = new VersionMeta(json);
			}

			return versionMeta;
		}
	}

	public Version getVersion(String version) {
		return versions.stream().filter(v -> v.id.equals(version)).findFirst().orElse(null);
	}
}
