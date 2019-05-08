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
import net.fabricmc.installer.util.ArgumentParser;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Utils;
import net.fabricmc.installer.util.Version;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class ClientHandler extends Handler {

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
			final ResourceBundle bundle = Utils.BUNDLE;
			try {
				updateProgress(new MessageFormat(bundle.getString("progress.installing")).format(new Object[]{loaderVersion}));
				File mcPath = new File(installLocation.getText());
				if (!mcPath.exists()) {
					throw new RuntimeException(bundle.getString("progress.exception.no.launcher.directory"));
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
	public void installCli(ArgumentParser args) throws Exception {
		File file = new File(args.get("dir"));
		if (!file.exists()) {
			throw new FileNotFoundException("Launcher directory not found at " + file.getAbsolutePath());
		}

		Version mappingsVersion = getMappingsVersion(args);
		String loaderVersion = getLoaderVersion(args);

		String profileName = ClientInstaller.install(file, mappingsVersion, loaderVersion, InstallerProgress.CONSOLE);
		ProfileInstaller.setupProfile(file, profileName, mappingsVersion);
	}

	@Override
	public String cliHelp() {
		return "-dir <install dir, required> -mappings <mappings version, default latest> -loader <loader version, default latest>";
	}

	@Override
	public void setupPane1(JPanel pane, InstallerGui installerGui) {

	}

	@Override
	public void setupPane2(JPanel pane, InstallerGui installerGui) {
		addRow(pane, jPanel -> jPanel.add(createProfile = new JCheckBox(Utils.BUNDLE.getString("option.create.profile"), true)));

		installLocation.setText(Utils.findDefaultInstallDir().getAbsolutePath());
	}

}
