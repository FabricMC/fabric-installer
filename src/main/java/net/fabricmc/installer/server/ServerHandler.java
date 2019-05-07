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
import net.fabricmc.installer.util.Version;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;

public class ServerHandler extends Handler {

	@Override
	public String name() {
		return "Server";
	}

	@Override
	public void install() {
		String versionStr = (String) mappingVersionComboBox.getSelectedItem();
		Version version = new Version(versionStr);
		String loaderVersion = (String) loaderVersionComboBox.getSelectedItem();
		new Thread(() -> {
			try {
				ServerInstaller.install(new File(installLocation.getText()), loaderVersion, version, this);
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
		Version version = getMappingsVersion(args);
		ServerInstaller.install(file.getAbsoluteFile(), loaderVersion, version, InstallerProgress.CONSOLE);
	}

	@Override
	public String cliHelp() {
		return "-dir <install dir, default current dir> -mappings <mappings version, default latest> -loader <loader version, default latest>";
	}

	@Override
	public void setupPane1(JPanel pane, InstallerGui installerGui) {

	}

	@Override
	public void setupPane2(JPanel pane, InstallerGui installerGui) {
		installLocation.setText(new File("").getAbsolutePath());
	}

}
