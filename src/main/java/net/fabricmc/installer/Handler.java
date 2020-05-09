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

package net.fabricmc.installer;

import net.fabricmc.installer.util.ArgumentParser;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Utils;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.io.IOException;
import java.util.function.Consumer;

public abstract class Handler implements InstallerProgress {

	public JButton buttonInstall;

	public JComboBox<String> gameVersionComboBox;
	public JComboBox<String> loaderVersionComboBox;
	public JTextField installLocation;
	public JButton selectFolderButton;
	public JLabel statusLabel;

	public JCheckBox snapshotCheckBox;

	private JPanel pane;

	public abstract String name();

	public abstract void install();

	public abstract void installCli(ArgumentParser args) throws Exception;

	public abstract String cliHelp();

	//this isnt great, but works
	public abstract void setupPane1(JPanel pane, InstallerGui installerGui);

	public abstract void setupPane2(JPanel pane, InstallerGui installerGui);

	public JPanel makePanel(InstallerGui installerGui) {
		pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));

		setupPane1(pane, installerGui);

		addRow(pane, jPanel -> {
			jPanel.add(new JLabel(Utils.BUNDLE.getString("prompt.game.version")));
			jPanel.add(gameVersionComboBox = new JComboBox<>());
			jPanel.add(snapshotCheckBox = new JCheckBox(Utils.BUNDLE.getString("option.show.snapshots")));
			snapshotCheckBox.setSelected(false);
			snapshotCheckBox.addActionListener(e -> {
				if (Main.GAME_VERSION_META.isComplete()) {
					updateGameVersions();
				}
			});
		});

		Main.GAME_VERSION_META.onComplete(versions -> {
			updateGameVersions();
		});

		addRow(pane, jPanel -> {
			jPanel.add(new JLabel(Utils.BUNDLE.getString("prompt.loader.version")));
			jPanel.add(loaderVersionComboBox = new JComboBox<>());
		});

		addRow(pane, jPanel -> {
			jPanel.add(new JLabel(Utils.BUNDLE.getString("prompt.select.location")));
			jPanel.add(installLocation = new JTextField());
			jPanel.add(selectFolderButton = new JButton());

			selectFolderButton.setText("...");
			selectFolderButton.addActionListener(e -> InstallerGui.selectInstallLocation(() -> installLocation.getText(), s -> installLocation.setText(s)));
		});

		setupPane2(pane, installerGui);

		addRow(pane, jPanel -> {
			jPanel.add(statusLabel = new JLabel());
			statusLabel.setText(Utils.BUNDLE.getString("prompt.loading.versions"));
		});

		addRow(pane, jPanel -> {
			jPanel.add(buttonInstall = new JButton(Utils.BUNDLE.getString("prompt.install")));
			buttonInstall.addActionListener(e -> {
				buttonInstall.setEnabled(false);
				install();
			});
		});

		Main.LOADER_META.onComplete(versions -> {
			int stableIndex = -1;
			for (int i = 0; i < versions.size(); i++) {
				MetaHandler.GameVersion version = versions.get(i);
				loaderVersionComboBox.addItem(version.getVersion());
				if(version.isStable()){
					stableIndex = i;
				}
			}
			//If no stable versions are found, default to the latest version
			if(stableIndex == -1){
				stableIndex = 0;
			}
			loaderVersionComboBox.setSelectedIndex(stableIndex);
			statusLabel.setText(Utils.BUNDLE.getString("prompt.ready.install"));
		});

		return pane;
	}

	private void updateGameVersions() {
		gameVersionComboBox.removeAllItems();
		for (MetaHandler.GameVersion version : Main.GAME_VERSION_META.getVersions()) {
			if (!snapshotCheckBox.isSelected() && !version.isStable()) {
				continue;
			}
			gameVersionComboBox.addItem(version.getVersion());
		}
		gameVersionComboBox.setSelectedIndex(0);
	}

	@Override
	public void updateProgress(String text) {
		statusLabel.setText(text);
		statusLabel.setForeground(UIManager.getColor("Label.foreground"));
	}

	private void appendException(StringBuilder errorMessage, String prefix, String name, Throwable e) {
		String prefixAppend = "  ";

		errorMessage.append(prefix).append(name).append(": ").append(e.getLocalizedMessage()).append('\n');
		for (StackTraceElement traceElement : e.getStackTrace()) {
			errorMessage.append(prefix).append("- ").append(traceElement).append('\n');
		}

		if (e.getCause() != null) {
			appendException(errorMessage, prefix + prefixAppend, Utils.BUNDLE.getString("prompt.exception.caused.by"), e.getCause());
		}

		for (Throwable ec : e.getSuppressed()) {
			appendException(errorMessage, prefix + prefixAppend, Utils.BUNDLE.getString("prompt.exception.suppressed"), ec);
		}
	}

	@Override
	public void error(Exception e) {
		StringBuilder errorMessage = new StringBuilder();
		appendException(errorMessage, "", Utils.BUNDLE.getString("prompt.exception"), e);

		System.err.println(errorMessage);

		statusLabel.setText(e.getLocalizedMessage());
		statusLabel.setForeground(Color.RED);

		JOptionPane.showMessageDialog(
				pane,
				errorMessage,
				Utils.BUNDLE.getString("prompt.exception.occurrence"),
				JOptionPane.ERROR_MESSAGE
		);
	}

	protected void addRow(Container parent, Consumer<JPanel> consumer) {
		JPanel panel = new JPanel(new FlowLayout());
		consumer.accept(panel);
		parent.add(panel);
	}

	protected String getGameVersion(ArgumentParser args) {
		return args.getOrDefault("mcversion", () -> {
			System.out.println("Using latest game version");
			try {
				Main.GAME_VERSION_META.load();
			} catch (IOException e) {
				throw new RuntimeException("Failed to load latest versions", e);
			}
			return Main.GAME_VERSION_META.getLatestVersion(args.has("snapshot")).getVersion();
		});
	}

	protected String getLoaderVersion(ArgumentParser args) {
		return args.getOrDefault("loader", () -> {
			System.out.println("Using latest loader version");
			try {
				Main.LOADER_META.load();
			} catch (IOException e) {
				throw new RuntimeException("Failed to load latest versions", e);
			}
			return Main.LOADER_META.getLatestVersion(false).getVersion();
		});
	}

}
