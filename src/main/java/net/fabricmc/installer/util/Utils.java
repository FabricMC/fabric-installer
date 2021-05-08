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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class Utils {

	public static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("lang/installer", Locale.getDefault(), new ResourceBundle.Control() {
		@Override
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
			final String bundleName = toBundleName(baseName, locale);
			final String resourceName = toResourceName(bundleName, "properties");
			try (InputStream stream = loader.getResourceAsStream(resourceName)) {
				if (stream != null) {
					try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
						return new PropertyResourceBundle(reader);
					}
				}
			}
			return super.newBundle(baseName, locale, format, loader, reload);
		}
	});

	public static Path findDefaultInstallDir() {
		String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
		Path dir;

		if (os.contains("win") && System.getenv("APPDATA") != null) {
			dir = Paths.get(System.getenv("APPDATA")).resolve(".minecraft");
		} else {
			String home = System.getProperty("user.home", ".");
			Path homeDir = Paths.get(home);

			if (os.contains("mac")) {
				dir = homeDir.resolve("Library").resolve("Application Support").resolve("minecraft");
			} else {
				dir = homeDir.resolve(".minecraft");
			}
		}

		return dir.toAbsolutePath().normalize();
	}

	public static Reader urlReader(URL url) throws IOException {
		return new InputStreamReader(url.openStream(), StandardCharsets.UTF_8);
	}

	public static String readTextFile(URL url) throws IOException {
		try (BufferedReader reader = new BufferedReader(urlReader(url))) {
			return reader.lines().collect(Collectors.joining("\n"));
		}
	}

	public static String readString(Path path) throws IOException {
		return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
	}

	public static void writeToFile(Path path, String string) throws IOException {
		Files.write(path, string.getBytes(StandardCharsets.UTF_8));
	}

	public static void downloadFile(URL url, Path path) throws IOException {
		Files.createDirectories(path.getParent());

		try (InputStream in = url.openStream()) {
			Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	public static String getProfileIcon() {

		try (InputStream is = Utils.class.getClassLoader().getResourceAsStream("profile_icon.png")) {
			byte[] ret = new byte[4096];
			int offset = 0;
			int len;

			while ((len = is.read(ret, offset, ret.length - offset)) != -1) {
				offset += len;
				if (offset == ret.length) ret = Arrays.copyOf(ret, ret.length * 2);
			}

			return "data:image/png;base64," + Base64.getEncoder().encodeToString(Arrays.copyOf(ret, offset));

		} catch (IOException e) {
			e.printStackTrace();
		}
		return "TNT"; // Fallback to TNT icon if we cant load Fabric icon.
	}

}
