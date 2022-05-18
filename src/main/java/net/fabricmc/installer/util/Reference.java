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

	public static String metaServerUrl = "https://meta.fabricmc.net/";
	public static String fabricApiUrl = "https://www.curseforge.com/minecraft/mc-mods/fabric-api/";
	public static String serverLauncherUrl = "https://fabricmc.net/use/server/";
	public static String minecraftLauncherManifest = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
	public static String experimentalVersionsManifest = "https://maven.fabricmc.net/net/minecraft/experimental_versions.json";

	public static String getMetaServerEndpoint(String path) {
		return metaServerUrl + path;
	}
}
