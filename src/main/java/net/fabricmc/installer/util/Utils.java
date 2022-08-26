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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		Path dir;

		if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS && System.getenv("APPDATA") != null) {
			dir = Paths.get(System.getenv("APPDATA")).resolve(".minecraft");
		} else {
			String home = System.getProperty("user.home", ".");
			Path homeDir = Paths.get(home);

			if (OperatingSystem.CURRENT == OperatingSystem.MACOS) {
				dir = homeDir.resolve("Library").resolve("Application Support").resolve("minecraft");
			} else {
				dir = homeDir.resolve(".minecraft");
			}
		}

		return dir.toAbsolutePath().normalize();
	}

	public static String readString(URL url) throws IOException {
		try (InputStream is = openUrl(url)) {
			return readString(is);
		}
	}

	public static String readString(Path path) throws IOException {
		return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
	}

	public static String readString(InputStream is) throws IOException {
		byte[] data = new byte[Math.max(1000, is.available())];
		int offset = 0;
		int len;

		while ((len = is.read(data, offset, data.length - offset)) >= 0) {
			offset += len;

			if (offset == data.length) {
				int next = is.read();
				if (next < 0) break;

				data = Arrays.copyOf(data, data.length * 2);
				data[offset++] = (byte) next;
			}
		}

		return new String(data, 0, offset, StandardCharsets.UTF_8);
	}

	public static void writeToFile(Path path, String string) throws IOException {
		Files.write(path, string.getBytes(StandardCharsets.UTF_8));
	}

	public static void downloadFile(URL url, Path path) throws IOException {
		try (InputStream in = openUrl(url)) {
			Files.createDirectories(path.getParent());
			Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
		} catch (Throwable t) {
			try {
				Files.deleteIfExists(path);
			} catch (Throwable t2) {
				t.addSuppressed(t2);
			}

			throw t;
		}
	}

	private static final int HTTP_TIMEOUT_MS = 8000;

	private static InputStream openUrl(URL url) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setConnectTimeout(HTTP_TIMEOUT_MS);
		conn.setReadTimeout(HTTP_TIMEOUT_MS);
		conn.connect();

		int responseCode = conn.getResponseCode();
		if (responseCode < 200 || responseCode >= 300) throw new IOException("HTTP request to "+url+" failed: "+responseCode);

		return conn.getInputStream();
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

	public static String sha1String(Path path) throws IOException {
		return bytesToHex(sha1(path));
	}

	public static byte[] sha1(Path path) throws IOException {
		MessageDigest digest = sha1Digest();

		try (InputStream is = Files.newInputStream(path)) {
			byte[] buffer = new byte[64 * 1024];
			int len;

			while ((len = is.read(buffer)) >= 0) {
				digest.update(buffer, 0, len);
			}
		}

		return digest.digest();
	}

	private static MessageDigest sha1Digest() {
		try {
			return MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Something has gone really wrong", e);
		}
	}

	public static String bytesToHex(byte[] bytes) {
		StringBuilder output = new StringBuilder();

		for (byte b : bytes) {
			output.append(String.format(Locale.ENGLISH, "%02x", b));
		}

		return output.toString();
	}

	/**
	 * Simple semver-like version comparison.
	 *
	 * @return <0,0,>0 if versionA is less/same/greater than versionB
	 */
	public static int compareVersions(String versionA, String versionB) {
		Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)*)(?:-([^+]+))?(?:\\+.*)?");
		Matcher matcherA = pattern.matcher(versionA);
		Matcher matcherB = pattern.matcher(versionB);
		if (!matcherA.matches() || !matcherB.matches()) return versionA.compareTo(versionB);

		int cmp = compareVersionGroups(matcherA.group(1), matcherB.group(1)); // compare version core
		if (cmp != 0) return cmp;

		boolean aHasPreRelease = matcherA.group(2) != null;
		boolean bHasPreRelease = matcherB.group(2) != null;

		if (aHasPreRelease != bHasPreRelease) { // compare pre-release presence
			return aHasPreRelease ? -1 : 1;
		}

		if (aHasPreRelease) {
			cmp = compareVersionGroups(matcherA.group(2), matcherB.group(2)); // compare pre-release
			if (cmp != 0) return cmp;
		}

		return 0;
	}

	private static int compareVersionGroups(String groupA, String groupB) {
		String[] partsA = groupA.split("\\.");
		String[] partsB = groupB.split("\\.");

		for (int i = 0; i < Math.min(partsA.length, partsB.length); i++) {
			String partA = partsA[i];
			String partB = partsB[i];

			try {
				int a = Integer.parseInt(partA);

				try {
					int b = Integer.parseInt(partB);
					int cmp = Integer.compare(a, b); // both numeric, compare int value
					if (cmp != 0) return cmp;
				} catch (NumberFormatException e) {
					return -1; // only a numeric
				}
			} catch (NumberFormatException e) {
				try {
					Integer.parseInt(partB);
					return 1; // only b numeric
				} catch (NumberFormatException e2) {
					// ignore
				}
			}

			int cmp = partA.compareTo(partB); // neither numeric, compare lexicographically
			if (cmp != 0) return cmp;
		}

		return Integer.compare(partsA.length, partsB.length); // compare part count
	}
}
