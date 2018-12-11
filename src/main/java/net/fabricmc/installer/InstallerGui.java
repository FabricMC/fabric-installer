/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
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
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
	private JProgressBar progressBar;
	private JLabel statusLabel;

	public InstallerGui() throws XmlPullParserException, IOException {
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
	}

	public void install() {
		String mappingsVersion = (String) mappingVersionComboBox.getSelectedItem();
		String loaderVersion = (String) loaderVersionComboBox.getSelectedItem();
		System.out.println(Translator.INSTANCE.getString("gui.installing"));
		new Thread(() -> {
			try {
				updateProgress(Translator.INSTANCE.getString("gui.installing") + ": " + loaderVersion, 0);
				File mcPath = new File(installLocation.getText());
				if (!mcPath.exists()) {
					throw new RuntimeException(Translator.INSTANCE.getString("install.client.error.noLauncher"));
				}
				ClientInstaller.install(mcPath, mappingsVersion, loaderVersion, this);
			} catch (Exception e) {
				e.printStackTrace();
				error(e.getLocalizedMessage());
			}
		}).start();
	}

	public void selectInstallLocation() {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(installLocation.getText()));
		chooser.setDialogTitle(Translator.INSTANCE.getString("gui.selectInstallLocation"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			installLocation.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	@Override
	public void updateProgress(String text, int percentage) {
		statusLabel.setText(text);
		statusLabel.setForeground(Color.BLACK);
		progressBar.setValue(percentage);
	}

	@Override
	public void error(String error) {
		statusLabel.setText(error);
		statusLabel.setForeground(Color.RED);
	}

	public static void start()
		throws XmlPullParserException, IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException,
		IllegalAccessException {
		//This will make people happy
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		InstallerGui dialog = new InstallerGui();
		dialog.pack();
		dialog.setTitle(Translator.INSTANCE.getString("fabric.installer.name"));
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	private void initComponents() {
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 10, 5, 10));

		addRow(jPanel -> {
			jPanel.add(new JLabel(Translator.INSTANCE.getString("gui.version.mappings")));
			jPanel.add(mappingVersionComboBox = new JComboBox<>());
		});

		addRow(jPanel -> {
			jPanel.add(new JLabel(Translator.INSTANCE.getString("gui.version.loader")));
			jPanel.add(loaderVersionComboBox = new JComboBox<>());
		});

		addRow(jPanel -> {
			jPanel.add(new JLabel(Translator.INSTANCE.getString("gui.selectInstallLocation")));
			jPanel.add(installLocation = new JTextField());
			jPanel.add(selectFolderButton = new JButton());

			selectFolderButton.setText("...");
			selectFolderButton.addActionListener(e -> selectInstallLocation());
			installLocation.setText(Utils.findDefaultInstallDir().getAbsolutePath());
		});

		addRow(jPanel -> {
			jPanel.add(statusLabel = new JLabel());
			jPanel.add(progressBar = new JProgressBar());
			progressBar.setMaximum(100);

			//Forces the progress bar to expand to fit the width
			jPanel.setLayout(new GridLayout(0, 1));
		});

		addRow(jPanel -> {
			jPanel.add(buttonInstall = new JButton(Translator.INSTANCE.getString("gui.install")));
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
