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
import net.fabricmc.installer.Main;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Version;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class ServerHandler extends Handler {

	public JComboBox<String> mappingVersionComboBox;
	public JTextField serverJarName;

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
				ServerInstaller.install(new File(installLocation.getText()), loaderVersion, serverJarName.getText(), version, this);
			} catch (IOException e) {
				e.printStackTrace();
				error(e.getLocalizedMessage());
			}
		}).start();
	}

	@Override
	public void installCli(String[] args) throws Exception {
		String loaderVersion = null;
		if (args.length != 2) {
			System.out.println("Using latest loader version");
			Main.LOADER_MAVEN.load();
			loaderVersion = Main.LOADER_MAVEN.latestVersion;
		} else {
			loaderVersion = args[1];
		}
		Main.MAPPINGS_MAVEN.load();
		Version version = new Version(Main.MAPPINGS_MAVEN.latestVersion);
		ServerInstaller.install(new File("").getAbsoluteFile(), loaderVersion, "minecraft-server.jar", version, InstallerProgress.CONSOLE);
	}

	@Override
	public String cliHelp() {
		return "<loader_version> - installs a fabric server in the current working directory";
	}

	@Override
	public void setupPane1(JPanel pane, InstallerGui installerGui) {
		addRow(pane, jPanel -> {
			jPanel.add(new JLabel("MC Server jar"));
			jPanel.add(serverJarName = new JTextField("minecraft-server.jar"));
		});

		addRow(pane, jPanel -> {
			jPanel.add(new JLabel("Mappings version:"));
			jPanel.add(mappingVersionComboBox = new JComboBox<>());
		});

		Main.MAPPINGS_MAVEN.onComplete(versions -> {
			for (String str : versions) {
				mappingVersionComboBox.addItem(str);
			}
			mappingVersionComboBox.setSelectedIndex(0);
		});
	}

	@Override
	public void setupPane2(JPanel pane, InstallerGui installerGui) {
		installLocation.setText(new File("").getAbsolutePath());
	}

}
