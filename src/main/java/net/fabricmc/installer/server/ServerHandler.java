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

import net.fabricmc.installer.Handler;
import net.fabricmc.installer.InstallerGui;
import net.fabricmc.installer.util.ArgumentParser;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.LauncherMeta;
import net.fabricmc.installer.util.Utils;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ServerHandler extends Handler {

	@Override
	public String name() {
		return "Server";
	}

	@Override
	public void install() {
		String gameVersion = (String) gameVersionComboBox.getSelectedItem();
		String loaderVersion = (String) loaderVersionComboBox.getSelectedItem();
		new Thread(() -> {
			try {
				ServerInstaller.install(new File(installLocation.getText()), loaderVersion, gameVersion, this);
				ServerPostInstallDialog.show(this);
			} catch (Exception e) {
				error(e);
			}
			buttonInstall.setEnabled(true);
		}).start();
	}

	@Override
	public void installCli(ArgumentParser args) throws Exception {
		File file = new File(args.getOrDefault("dir", () -> "."));
		if (!file.exists()) {
			throw new FileNotFoundException("Server directory not found at " + file.getAbsolutePath());
		}
		String loaderVersion = getLoaderVersion(args);
		String gameVersion = getGameVersion(args);
		ServerInstaller.install(file.getAbsoluteFile(), loaderVersion, gameVersion, InstallerProgress.CONSOLE);

		if(args.has("downloadMinecraft")){
			File serverJar = new File(file, "server.jar");
			File serverJarTmp = new File(file, "server.jar.tmp");
			Files.deleteIfExists(serverJar.toPath());
			InstallerProgress.CONSOLE.updateProgress(Utils.BUNDLE.getString("progress.download.minecraft"));
			Utils.downloadFile(new URL(LauncherMeta.getLauncherMeta().getVersion(gameVersion).getVersionMeta().downloads.get("server").url), serverJarTmp);
			Files.move(serverJarTmp.toPath(), serverJar.toPath(), StandardCopyOption.REPLACE_EXISTING);
			InstallerProgress.CONSOLE.updateProgress(Utils.BUNDLE.getString("progress.done"));
		}
	}

	@Override
	public String cliHelp() {
		return "-dir <install dir, default current dir> -mcversion <minecraft version, default latest> -loader <loader version, default latest> -downloadMinecraft";
	}

	@Override
	public void setupPane1(JPanel pane, InstallerGui installerGui) {

	}

	@Override
	public void setupPane2(JPanel pane, InstallerGui installerGui) {
		installLocation.setText(new File("").getAbsolutePath());
	}

}
