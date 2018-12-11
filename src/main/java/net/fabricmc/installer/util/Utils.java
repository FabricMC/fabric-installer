/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Utils {
	public static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static File findDefaultInstallDir() {
		String home = System.getProperty("user.home", ".");
		String os = System.getProperty("os.name").toLowerCase();
		File dir;
		File homeDir = new File(home);

		if (os.contains("win") && System.getenv("APPDATA") != null) {
			dir = new File(System.getenv("APPDATA"), ".minecraft");
		} else if (os.contains("mac")) {
			dir = new File(homeDir, "Library" + File.separator + "Application Support" + File.separator + "minecraft");
		} else {
			dir = new File(homeDir, ".minecraft");
		}
		return dir;
	}
}
