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

import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MetaHandler extends CompletableHandler<List<MetaHandler.GameVersion>> {

	private final String metaUrl;
	private List<GameVersion> versions;

	public MetaHandler(String url) {
		this.metaUrl = url;
	}

	public void load() throws IOException {
		URL url = new URL(metaUrl);
		URLConnection conn = url.openConnection();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
			String json = reader.lines().collect(Collectors.joining("\n"));
			Type type = new TypeToken<ArrayList<GameVersion>>() {}.getType();
			versions = Utils.GSON.fromJson(json, type);
			complete(versions);
		}
	}

	public List<GameVersion> getVersions() {
		return Collections.unmodifiableList(versions);
	}

	public GameVersion getLatestVersion(boolean snapshot){
		if(snapshot){
			return versions.get(0);
		} else {
			return versions.stream()
				.filter(GameVersion::isStable).findFirst().orElse(null);
		}
	}

	public static class GameVersion {
		String version;
		boolean stable;

		public String getVersion() {
			return version;
		}

		public boolean isStable() {
			return stable;
		}
	}

}
