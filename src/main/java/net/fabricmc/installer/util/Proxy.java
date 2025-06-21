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

import java.net.URL;
import java.net.MalformedURLException;

public class Proxy {
	public static void setProxyFromEnv() {
		setProxy("http", System.getenv("HTTP_PROXY"));
		setProxy("https", System.getenv("HTTPS_PROXY"));
	}

	public static void setProxy(String proto, String proxy) {
		if (proxy == null || proxy.isEmpty()) {
			return;
		}

		try {
			final URL url = new URL(proxy);
			int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
			System.setProperty(proto + ".proxyHost", url.getHost());
			System.setProperty(proto + ".proxyPort", Integer.toString(port));
		} catch (MalformedURLException e) {
			System.err.println("Failed to setup " + proto + " proxy:" + e.getMessage());
		}
	}
}
