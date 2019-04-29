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

package net.fabricmc.installer.client;

import net.fabricmc.installer.Handler;
import net.fabricmc.installer.InstallerGui;
import net.fabricmc.installer.Main;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Utils;
import net.fabricmc.installer.util.Version;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.security.InvalidParameterException;

public class ClientHandler extends Handler {

	public JComboBox<String> mappingVersionComboBox;
	private JCheckBox createProfile;

	@Override
	public String name() {
		return "Client";
	}

	@Override
	public void install() {
		String versionStr = (String) mappingVersionComboBox.getSelectedItem();
		String loaderVersion = (String) loaderVersionComboBox.getSelectedItem();
		Version version = new Version(versionStr);
		System.out.println("Installing");
		new Thread(() -> {
			try {
				updateProgress("Installing : " + loaderVersion);
				File mcPath = new File(installLocation.getText());
				if (!mcPath.exists()) {
					throw new RuntimeException("No launcher directory found");
				}
				String profileName = ClientInstaller.install(mcPath, version, loaderVersion, this);
				if (createProfile.isSelected()) {
					ProfileInstaller.setupProfile(mcPath, profileName, version);
				}
			} catch (Exception e) {
				error(e);
			}
			buttonInstall.setEnabled(true);
		}).start();
	}

	@Override
	public void installCli(String[] args) throws Exception {
		if (args.length < 2) {
			throw new InvalidParameterException("A minecraft launcher directory must be provided");
		}
		File file = new File(args[1]);
		if (!file.exists()) {
			throw new FileNotFoundException("Launcher directory not found");
		}
		Version version = new Version(args[2]);
		String loaderVersion = args[3];
		String profileName = ClientInstaller.install(file, version, loaderVersion, InstallerProgress.CONSOLE);
		ProfileInstaller.setupProfile(file, profileName, version);
	}

	@Override
	public String cliHelp() {
		return "*launcher_dir* <mappings version> <loader version> - Installs the client profile into the minecraft directory located at the provided location";
	}

	@Override
	public void setupPane1(JPanel pane, InstallerGui installerGui) {
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
		addRow(pane, jPanel -> jPanel.add(createProfile = new JCheckBox("Create profile", true)));

		installLocation.setText(Utils.findDefaultInstallDir().getAbsolutePath());
	}

}
