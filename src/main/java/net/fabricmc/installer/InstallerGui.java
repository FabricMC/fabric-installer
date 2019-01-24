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

import net.fabricmc.installer.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class InstallerGui extends JFrame implements IInstallerProgress {
	private JPanel contentPane;
	private JButton buttonInstall;
	private JComboBox<String> mappingVersionComboBox;
	private JComboBox<String> loaderVersionComboBox;
	private JTextField installLocation;
	private JButton selectFolderButton;
	private JLabel statusLabel;
	private JCheckBox createProfile;

	public InstallerGui() throws IOException, XMLStreamException {
		initComponents();
		setContentPane(contentPane);
		setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("icon.png")));

		MavenHandler loaderMaven = new MavenHandler();
		loaderMaven.load(Reference.MAVEN_SERVER_URL, Reference.PACKAGE, Reference.LOADER_NAME);
		for (String str : loaderMaven.versions) {
			loaderVersionComboBox.addItem(str);
		}
		loaderVersionComboBox.setSelectedIndex(0);

		MavenHandler mappingMaven = new MavenHandler();
		mappingMaven.load(Reference.MAVEN_SERVER_URL, Reference.PACKAGE, Reference.MAPPINGS_NAME);
		for (String str : mappingMaven.versions) {
			mappingVersionComboBox.addItem(str);
		}
		mappingVersionComboBox.setSelectedIndex(0);

		statusLabel.setText("Ready to install");
	}

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
				e.printStackTrace();
				error(e.getLocalizedMessage());
			}
		}).start();
	}

	public void selectInstallLocation() {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(installLocation.getText()));
		chooser.setDialogTitle("Select Install Location");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			installLocation.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	@Override
	public void updateProgress(String text) {
		statusLabel.setText(text);
		statusLabel.setForeground(Color.BLACK);
	}

	@Override
	public void error(String error) {
		statusLabel.setText(error);
		statusLabel.setForeground(Color.RED);
	}

	public static void start()
		throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException,
		IllegalAccessException, XMLStreamException {
		//This will make people happy
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		InstallerGui dialog = new InstallerGui();
		dialog.pack();
		dialog.setTitle("Fabric Installer");
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	private void initComponents() {
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 10, 5, 10));

		addRow(jPanel -> {
			jPanel.add(new JLabel("Mappings version:"));
			jPanel.add(mappingVersionComboBox = new JComboBox<>());
		});

		addRow(jPanel -> {
			jPanel.add(new JLabel("Loader Version:"));
			jPanel.add(loaderVersionComboBox = new JComboBox<>());
		});

		addRow(jPanel -> {
			jPanel.add(new JLabel("Select Install Location"));
			jPanel.add(installLocation = new JTextField());
			jPanel.add(selectFolderButton = new JButton());

			selectFolderButton.setText("...");
			selectFolderButton.addActionListener(e -> selectInstallLocation());
			installLocation.setText(Utils.findDefaultInstallDir().getAbsolutePath());
		});

		addRow(jPanel -> jPanel.add(createProfile = new JCheckBox("Create profile", true)));

		addRow(jPanel -> {
			jPanel.add(statusLabel = new JLabel());
			statusLabel.setText("Loading versions");
		});

		addRow(jPanel -> {
			jPanel.add(buttonInstall = new JButton("Install"));
			buttonInstall.addActionListener(e -> install());
			getRootPane().setDefaultButton(buttonInstall);
		});

	}

	private void addRow(Consumer<JPanel> consumer) {
		JPanel panel = new JPanel(new FlowLayout());
		consumer.accept(panel);
		contentPane.add(panel, BorderLayout.CENTER);
	}

}
