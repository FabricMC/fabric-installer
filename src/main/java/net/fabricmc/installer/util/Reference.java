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

public class Reference {
	public static final String LOADER_NAME = "fabric-loader";

	public static final String FABRIC_API_URL = "https://www.curseforge.com/minecraft/mc-mods/fabric-api/";
	public static final String SERVER_LAUNCHER_URL = "https://fabricmc.net/use/server/";
	public static final String MINECRAFT_LAUNCHER_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
	public static final String EXPERIMENTAL_LAUNCHER_MANIFEST = "https://maven.fabricmc.net/net/minecraft/experimental_versions.json";

	// regular meta and maven servers

	static final String DEFAULT_META_SERVER = "https://meta.fabricmc.net/";
	static final String DEFAULT_MAVEN_SERVER = "https://maven.fabricmc.net/";

	// Do not use these fallback servers to interact with our web services. They can and will be unavailable at times and only support limited throughput.
	// It is important that they only get used in order and after the regular servers.

	private static final String[] FALLBACK_META_SERVERS = {
			"https://meta2.fabricmc.net/",
			"https://meta3.fabricmc.net/"
	};
	private static final String[] FALLBACK_MAVEN_SERVERS = {
			"https://maven2.fabricmc.net/",
			"https://maven3.fabricmc.net/"
	};

	static final FabricService[] FABRIC_SERVICES = getServices();

	private static FabricService[] getServices() {
		String customMeta = System.getProperty("fabric.metaUrl");
		String customMaven = System.getProperty("fabric.mavenUrl");
		String defaultMeta = customMeta != null ? customMeta : DEFAULT_META_SERVER;
		String defaultMaven = customMaven != null ? customMaven : DEFAULT_MAVEN_SERVER;
		int metaCount = customMeta != null ? 1 : 1 + FALLBACK_META_SERVERS.length; // only use fallbacks without custom url
		int mavenCount = customMaven != null ? 1 : 1 + FALLBACK_MAVEN_SERVERS.length;

		FabricService[] ret = new FabricService[Math.max(metaCount, mavenCount)];

		for (int i = 0; i < ret.length; i++) {
			String meta = i == 0 || i >= metaCount ? defaultMeta : FALLBACK_META_SERVERS[i - 1];
			String maven = i == 0 || i >= mavenCount ? defaultMaven : FALLBACK_MAVEN_SERVERS[i - 1];

			ret[i] = new FabricService(meta, maven);
		}

		return ret;
	}
}
