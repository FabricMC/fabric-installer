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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import mjson.Json;

public class MetaHandler extends CompletableHandler<List<MetaHandler.GameVersion>> {
	private final String name;
	private final String metaPath;
	private List<GameVersion> versions;

	public MetaHandler(String name, String path) {
		this.name = name;
		this.metaPath = path;
	}

	public String getName() {
		return name;
	}

	public void load() throws IOException {
		Json json = FabricService.queryMetaJson(metaPath);

		this.versions = json.asJsonList()
				.stream()
				.map(GameVersion::new)
				.collect(Collectors.toList());

		complete(versions);
	}

	public List<GameVersion> getVersions() {
		return Collections.unmodifiableList(versions);
	}

	public GameVersion getLatestVersion(boolean snapshot) {
		if (versions.isEmpty()) throw new RuntimeException("no versions available at "+ metaPath);

		if (!snapshot) {
			for (GameVersion version : versions) {
				if (version.isStable()) return version;
			}

			// nothing found, fall back to snapshot versions
		}

		return versions.get(0);
	}

	public GameVersion parseVersion(String value, boolean snapshot) {
		if (value == null || value.isEmpty() || value.equalsIgnoreCase("latest")) {
			return getLatestVersion(snapshot);
		} else {
			for (GameVersion version : versions) {
				if (version.version.equals(value)) {
					return version;
				}
			}

			return null;
		}
	}

	public static final class GameVersion {
		final String version;
		final boolean stable;

		public GameVersion(Json json) {
			version = json.at("version").asString();
			stable = json.at("stable").asBoolean();
		}

		public String getVersion() {
			return version;
		}

		public boolean isStable() {
			return stable;
		}
	}
}
