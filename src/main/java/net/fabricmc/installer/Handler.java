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
import net.fabricmc.installer.util.Utils;
import net.fabricmc.installer.util.Version;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public abstract class Handler implements InstallerProgress {

	public JButton buttonInstall;

	public JComboBox<String> mappingVersionComboBox;
	public JComboBox<String> loaderVersionComboBox;
	public JTextField installLocation;
	public JButton selectFolderButton;
	public JLabel statusLabel;

	public JCheckBox snapshotCheckBox;

	private JPanel pane;
	private final ResourceBundle bundle = Utils.BUNDLE;

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
			jPanel.add(new JLabel(installerGui.getBundle().getString("prompt.mapping.version")));
			jPanel.add(mappingVersionComboBox = new JComboBox<>());
			jPanel.add(snapshotCheckBox = new JCheckBox(installerGui.getBundle().getString("option.show.snapshots")));
			snapshotCheckBox.setSelected(false);
			snapshotCheckBox.addActionListener(e -> {
				if(Main.MAPPINGS_MAVEN.complete){
					updateMappings();
				}
			});
		});

		Main.MAPPINGS_MAVEN.onComplete(versions -> {
			updateMappings();
		});

		addRow(pane, jPanel -> {
			jPanel.add(new JLabel(installerGui.getBundle().getString("prompt.loader.version")));
			jPanel.add(loaderVersionComboBox = new JComboBox<>());
		});

		addRow(pane, jPanel -> {
			jPanel.add(new JLabel(installerGui.getBundle().getString("prompt.select.location")));
			jPanel.add(installLocation = new JTextField());
			jPanel.add(selectFolderButton = new JButton());

			selectFolderButton.setText("...");
			selectFolderButton.addActionListener(e -> installerGui.selectInstallLocation(() -> installLocation.getText(), s -> installLocation.setText(s)));
		});

		setupPane2(pane, installerGui);

		addRow(pane, jPanel -> {
			jPanel.add(statusLabel = new JLabel());
			statusLabel.setText(installerGui.getBundle().getString("prompt.loading.versions"));
		});

		addRow(pane, jPanel -> {
			jPanel.add(buttonInstall = new JButton(installerGui.getBundle().getString("prompt.install")));
			buttonInstall.addActionListener(e -> {
				buttonInstall.setEnabled(false);
				install();
			});
		});

		Main.LOADER_MAVEN.onComplete(versions -> {
			for (String str : versions) {
				loaderVersionComboBox.addItem(str);
			}
			loaderVersionComboBox.setSelectedIndex(0);
			statusLabel.setText(installerGui.getBundle().getString("prompt.ready.install"));
		});

		return pane;
	}

	private void updateMappings(){
		mappingVersionComboBox.removeAllItems();
		for (String str : Main.MAPPINGS_MAVEN.versions) {
			if(!snapshotCheckBox.isSelected() && Version.isSnapshot(str)){
				continue;
			}
			mappingVersionComboBox.addItem(str);
		}
		mappingVersionComboBox.setSelectedIndex(0);
	}

	@Override
	public void updateProgress(String text) {
		statusLabel.setText(text);
		statusLabel.setForeground(Color.BLACK);
	}

	private void appendException(StringBuilder errorMessage, String prefix, String name, Throwable e) {
		String prefixAppend = "  ";

		errorMessage.append(prefix).append(name).append(": ").append(e.getLocalizedMessage()).append('\n');
		for (StackTraceElement traceElement : e.getStackTrace()) {
			errorMessage.append(prefix).append("- ").append(traceElement).append('\n');
		}

		if (e.getCause() != null) {
			appendException(errorMessage, prefix + prefixAppend, translate("prompt.exception.caused.by"), e.getCause());
		}

		for (Throwable ec : e.getSuppressed()) {
			appendException(errorMessage, prefix + prefixAppend, translate("prompt.exception.suppressed"), ec);
		}
	}

	@Override
	public void error(Exception e) {
		StringBuilder errorMessage = new StringBuilder();
		appendException(errorMessage, "", translate("prompt.exception"), e);

		System.err.println(errorMessage);

		JOptionPane.showMessageDialog(
				pane,
				errorMessage,
				translate("prompt.exception.occurrence"),
				JOptionPane.ERROR_MESSAGE
		);

		statusLabel.setText(e.getLocalizedMessage());
		statusLabel.setForeground(Color.RED);
	}

	private String translate(String key) {
		return bundle.getString(key);
	}

	protected void addRow(Container parent, Consumer<JPanel> consumer) {
		JPanel panel = new JPanel(new FlowLayout());
		consumer.accept(panel);
		parent.add(panel);
	}

	protected Version getMappingsVersion(ArgumentParser args){
		return new Version(args.getOrDefault("mappings", () -> {
			System.out.println("Using latest mapping version");
			try {
				Main.MAPPINGS_MAVEN.load();
			} catch (IOException | XMLStreamException e) {
				throw new RuntimeException("Failed to load latest versions", e);
			}
			return Main.MAPPINGS_MAVEN.getLatestVersion(args.has("snapshot"));
		}));
	}

	protected String getLoaderVersion(ArgumentParser args){
		return args.getOrDefault("loader", () -> {
			System.out.println("Using latest loader version");
			try {
				Main.LOADER_MAVEN.load();
			} catch (IOException | XMLStreamException e) {
				throw new RuntimeException("Failed to load latest versions", e);
			}
			return Main.LOADER_MAVEN.latestVersion;
		});
	}

}
