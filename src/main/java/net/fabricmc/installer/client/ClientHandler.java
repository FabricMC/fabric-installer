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
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;

public class ClientHandler extends Handler {

	private JCheckBox createProfile;

	@Override
	public String name() {
		return "Client";
	}

	@Override
	public void install() {
		String gameVersion = (String) gameVersionComboBox.getSelectedItem();
		String loaderVersion = (String) loaderVersionComboBox.getSelectedItem();
		System.out.println("Installing");
		new Thread(() -> {
			try {
				updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.installing")).format(new Object[]{loaderVersion}));
				File mcPath = new File(installLocation.getText());
				if (!mcPath.exists()) {
					throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.directory"));
				}
				String profileName = ClientInstaller.install(mcPath, gameVersion, loaderVersion, this);
				if (createProfile.isSelected()) {
					ProfileInstaller.setupProfile(mcPath, profileName, gameVersion);
				}
				SwingUtilities.invokeLater(() -> showInstalledMessage(loaderVersion, gameVersion));
			} catch (Exception e) {
				error(e);
			}
			buttonInstall.setEnabled(true);
		}).start();
	}

	private void showInstalledMessage(String loaderVersion, String gameVersion) {
		JEditorPane pane = new JEditorPane("text/html", "<html><body style=\"" + buildEditorPaneStyle() + "\">" + new MessageFormat(Utils.BUNDLE.getString("prompt.install.successful")).format(new Object[]{loaderVersion, gameVersion, Reference.fabricApiUrl}) + "</body></html>");
		pane.setEditable(false);
		pane.addHyperlinkListener(e -> {
			try {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} else {
						throw new UnsupportedOperationException("Failed to open " + e.getURL().toString());
					}
				}
			} catch (Throwable throwable) {
				error(throwable);
			}
		});
		JOptionPane.showMessageDialog(null, pane, Utils.BUNDLE.getString("prompt.install.successful.title"), JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public void installCli(ArgumentParser args) throws Exception {
		File file = new File(args.get("dir"));
		if (!file.exists()) {
			throw new FileNotFoundException("Launcher directory not found at " + file.getAbsolutePath());
		}

		String gameVersion = getGameVersion(args);
		String loaderVersion = getLoaderVersion(args);

		String profileName = ClientInstaller.install(file, gameVersion, loaderVersion, InstallerProgress.CONSOLE);
		if (args.has("noprofile")) {
			return;
		}
		ProfileInstaller.setupProfile(file, profileName, gameVersion);
	}

	@Override
	public String cliHelp() {
		return "-dir <install dir, required> -mcversion <minecraft version, default latest> -loader <loader version, default latest>";
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
