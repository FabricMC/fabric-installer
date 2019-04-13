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

package net.fabricmc.installer.server;

import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.MinecraftLaunchJson;
import net.fabricmc.installer.util.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ServerInstaller {

	public static void install(File dir, String loaderVersion, InstallerProgress progress) throws IOException {
		progress.updateProgress(String.format("Installing fabric server %s", loaderVersion));
		File libsDir = new File(dir, "libraries");
		progress.updateProgress("Downloading required files");
		MinecraftLaunchJson meta = Utils.getLaunchMeta(loaderVersion);

		//We add fabric-loader as a lib so it can be downloaded and loaded in the same way as the other libs
		meta.libraries.add(new MinecraftLaunchJson.Library("net.fabricmc:fabric-loader:" + loaderVersion, "https://maven.fabricmc.net/"));

		for (MinecraftLaunchJson.Library library : meta.libraries) {
			progress.updateProgress("Downloading library " + library.name);
			File libraryFile = new File(libsDir, library.getFileName());
			Utils.downloadFile(new URL(library.getURL()), libraryFile);
		}

		progress.updateProgress("Generating server launch jar");
		File launchJar = new File(dir, "fabric-server-launch.jar");
		makeLaunchJar(launchJar, meta);

		progress.updateProgress("Done, start server by running " + launchJar.getName());
	}

	private static void makeLaunchJar(File file, MinecraftLaunchJson meta) throws IOException {
		if (file.exists()) {
			file.delete();
		}
		FileOutputStream outputStream = new FileOutputStream(file);
		ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

		{
			zipOutputStream.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));

			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(new Attributes.Name("Manifest-Version"), "1.0");
			manifest.getMainAttributes().put(new Attributes.Name("Main-Class"), meta.mainClassServer);
			manifest.getMainAttributes().put(new Attributes.Name("Class-Path"), meta.libraries.stream().map(library -> "libraries/" + library.getFileName().replaceAll("\\\\", "/")).collect(Collectors.joining(" ")));
			manifest.write(zipOutputStream);

			zipOutputStream.closeEntry();
		}

		zipOutputStream.close();
		outputStream.close();
	}

}
