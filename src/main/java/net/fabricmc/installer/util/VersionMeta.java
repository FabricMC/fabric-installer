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

import java.util.HashMap;
import java.util.Map;

public class VersionMeta {

	public final String id;
	public final Map<String, Download> downloads;

	public VersionMeta(Json json) {
		id = json.at("id").asString();
		downloads = new HashMap<>();
		for (Map.Entry<String, Json> entry : json.at("downloads").asJsonMap().entrySet()) {
			downloads.put(entry.getKey(), new Download(entry.getValue()));
		}
	}

	public static class Download {
		public final String sha1;
		public final long size;
		public final String url;

		public Download(Json json) {
			sha1 = json.at("sha1").asString();
			size = json.at("size").asLong();
			url = json.at("url").asString();
		}
	}


}
