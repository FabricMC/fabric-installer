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

package net.fabricmc.installer;

import com.google.gson.JsonObject;
import net.fabricmc.installer.util.MinecraftLaunchJson;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

import java.io.IOException;
import java.net.URL;

public class LoaderUtils {

	public static MinecraftLaunchJson getLaunchMeta(String loaderVersion) throws IOException {
		String url = String.format("%s/%s/%s/%s/%3$s-%4$s.json", Reference.MAVEN_SERVER_URL, Reference.PACKAGE, Reference.LOADER_NAME, loaderVersion);
		String fabricInstallMeta = Utils.getUrl(new URL(url));
		JsonObject installMeta = Utils.GSON.fromJson(fabricInstallMeta, JsonObject.class);
		return new MinecraftLaunchJson(installMeta);
	}

}
