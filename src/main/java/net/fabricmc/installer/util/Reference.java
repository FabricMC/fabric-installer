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

	public static final String DEFAULT_MAVEN_SERVER = "https://maven.fabricmc.net/";

	private static final FabricServices[] FABRIC_SERVICES = {
			new FabricServices(
					"https://meta.fabricmc.net/", DEFAULT_MAVEN_SERVER
			),
			// Do not use these fallback servers to interact with our web services. They can and will be unavailable at times, with limited throughput.
			new FabricServices(
					"https://meta2.fabricmc.net/", "https://maven2.fabricmc.net/"
			),
			new FabricServices(
					"https://meta3.fabricmc.net/", "https://maven3.fabricmc.net/"
			)
	};

	private static int activeService = 0;

	public static FabricServices getActiveService() {
		return FABRIC_SERVICES[activeService];
	}

	public static String getMetaServerEndpoint(String path) {
		return getActiveService().getMetaUrl() + path;
	}

	public static String getExperimentalVersionsManifestUrl() {
		return getActiveService().getMavenUrl() + "net/minecraft/experimental_versions.json";
	}

	public static boolean switchToNextFallback() {
		if (activeService == FABRIC_SERVICES.length - 1) {
			// Nothing else to fallback to
			return false;
		}

		activeService++;
		System.out.println("Switching to fallback service");
		System.out.println(getActiveService());
		return true;
	}
}
