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

package net.fabricmc.installer.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class NativesHelper {
	private static final String OS_ID = getOS() + "-" + System.getProperty("os.arch").toLowerCase(Locale.ROOT);
	private static final Map<String, String> NATIVES_MAP = getNativesMap();

	private static boolean loaded = false;

	static {
		System.out.println("OS_ID: " + OS_ID);
	}

	private static Map<String, String> getNativesMap() {
		Map<String, String> natives = new HashMap<>();

		natives.put("windows-arm64", "natives/windows-ARM64.dll");
		natives.put("windows-win32", "natives/windows-Win32.dll");
		natives.put("windows-amd64", "natives/windows-x64.dll");

		natives.put("macos-x86_64", "natives/macos-x86_64_arm64.dylib");
		natives.put("macos-aarch64", "natives/macos-x86_64_arm64.dylib");

		return natives;
	}

	private static String getOS() {
		String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

		if (osName.contains("win")) {
			return "windows";
		} else if (osName.contains("mac")) {
			return "macos";
		} else {
			return "linux";
		}
	}

	public static boolean loadSafelyIfCompatible() {
		if (isCompatible()) {
			try {
				loadNatives();
				return true;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	private static boolean isCompatible() {
		return NATIVES_MAP.containsKey(OS_ID);
	}

	private static void loadNatives() throws IOException {
		if (loaded) {
			return;
		}

		String nativeName = NATIVES_MAP.get(OS_ID);
		Path nativePath = Files.createTempFile("fabric-installer-native", null);

		try (InputStream is = NativesHelper.class.getClassLoader().getResourceAsStream(nativeName)) {
			Objects.requireNonNull(is, "Could not load: " + nativeName);
			Files.copy(is, nativePath, StandardCopyOption.REPLACE_EXISTING);
		}

		System.load(nativePath.toString());

		loaded = true;
	}
}
