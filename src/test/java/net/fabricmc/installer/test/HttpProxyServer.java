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

package net.fabricmc.installer.test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A very simple HTTP(S) proxy server that listens on port 8888.
 * Do not use this in production; it is for testing purposes only.
 */
public class HttpProxyServer {
	public static void main(String[] args) throws Exception {
		try (ServerSocket server = new ServerSocket(8888)) {
			ExecutorService pool = Executors.newCachedThreadPool();

			while (true) {
				Socket client = server.accept();
				pool.submit(() -> handle(client));
			}
		}
	}

	static void handle(Socket client) {
		System.out.println("New connection from " + client.getInetAddress() + ":" + client.getPort());

		try {
			InputStream in = client.getInputStream();
			OutputStream out = client.getOutputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = reader.readLine();

			if (line == null || !line.startsWith("CONNECT")) {
				client.close();
				return;
			}

			String[] parts = line.split(" ");
			String[] hostPort = parts[1].split(":");
			String host = hostPort[0];
			int port = Integer.parseInt(hostPort[1]);

			while (!reader.readLine().isEmpty()) {
				// Read the remaining headers
			}

			Socket remote = new Socket(host, port);
			out.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.UTF_8));
			forward(client, remote);
		} catch (Exception e) {
			try {
				client.close();
			} catch (Exception ignored) {
				// Do nothing
			}
		}
	}

	static void forward(Socket a, Socket b) {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		pool.submit(() -> copy(a, b));
		pool.submit(() -> copy(b, a));
	}

	static void copy(Socket inSock, Socket outSock) {
		try {
			InputStream in = inSock.getInputStream();
			OutputStream out = outSock.getOutputStream();
			byte[] buf = new byte[8192];
			int len;

			while ((len = in.read(buf)) != -1) {
				out.write(buf, 0, len);
				out.flush();
			}
		} catch (Exception ignored) {
			// Ignored
		} finally {
			try {
				inSock.close();
			} catch (Exception ignored) {
				// Ignored
			}

			try {
				outSock.close();
			} catch (Exception ignored) {
				// Ignored
			}
		}
	}
}
