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

package net.fabricmc.installer.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import net.fabricmc.installer.installer.ClientInstaller;
import net.fabricmc.installer.installer.LocalVersionInstaller;
import net.fabricmc.installer.installer.MultiMCInstaller;
import net.fabricmc.installer.installer.ServerInstaller;
import net.fabricmc.installer.util.IInstallerProgress;
import net.fabricmc.installer.util.Translator;
import net.fabricmc.installer.util.VersionInfo;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class MainGui extends JFrame implements IInstallerProgress {
	private JPanel contentPane;
	private JButton buttonInstall;
	private JComboBox versionComboBox;
	private JRadioButton clientRadioButton;
	private JRadioButton multimcRadioButton;
	private JRadioButton serverRadioButton;
	private JTextField installLocation;
	private JButton selectFolderButton;
	private JProgressBar progressBar;
	private JLabel statusLabel;
	public static String LOCAL_VERSION_STRING = "Local Jar file";

	public MainGui() throws ParserConfigurationException, XmlPullParserException, SAXException, IOException {
		initComponents();
		setContentPane(contentPane);
		getRootPane().setDefaultButton(buttonInstall);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		setIconImage(Toolkit.getDefaultToolkit().getImage(classLoader.getResource("icon.png")));
		buttonInstall.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				install();
			}
		});
		selectFolderButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectInstallLocation();
			}
		});
		VersionInfo.load();
		VersionInfo.versions.add(LOCAL_VERSION_STRING);
		for (String str : VersionInfo.versions) {
			versionComboBox.addItem(str);
		}
		versionComboBox.setSelectedIndex(0);
		clientRadioButton.setSelected(true);
		progressBar.setMaximum(100);

		String home = System.getProperty("user.home", ".");
		String os = System.getProperty("os.name").toLowerCase();
		File mcDefaultInstallLoc;
		File homeDir = new File(home);

		if (os.contains("win") && System.getenv("APPDATA") != null) {
			mcDefaultInstallLoc = new File(System.getenv("APPDATA"), ".minecraft");
		} else if (os.contains("mac")) {

			mcDefaultInstallLoc = new File(homeDir, "Library" + File.separator + "Application Support" + File.separator + "minecraft");
		} else {
			mcDefaultInstallLoc = new File(homeDir, ".minecraft");
		}

		installLocation.setText(mcDefaultInstallLoc.getAbsolutePath());

	}

	public void install() {
		if (clientRadioButton.isSelected()) {
			String version = (String) versionComboBox.getSelectedItem();
			System.out.println(Translator.getString("gui.installing") + ": " + version);
			if (version.equals(LOCAL_VERSION_STRING)) {
				new Thread(() -> {
					try {
						LocalVersionInstaller.install(new File(installLocation.getText()), this);

					} catch (Exception e) {
						e.printStackTrace();
						error(e.getLocalizedMessage());
					}
				}).start();
				return;
			}
			String[] split = version.split("-");
			new Thread(() -> {
				Optional<String> stringOptional = ClientInstaller.isValidInstallLocation(new File(installLocation.getText()), split[0]);
				if (stringOptional.isPresent()) {
					error(stringOptional.get());
				} else {
					try {
						updateProgress(Translator.getString("gui.installing") + ": " + version, 0);
						ClientInstaller.install(new File(installLocation.getText()), version, this);
					} catch (IOException e) {
						e.printStackTrace();
						error(e.getLocalizedMessage());
					}

				}
			}).start();
		} else if (serverRadioButton.isSelected()) {
			String version = (String) versionComboBox.getSelectedItem();
			System.out.println(Translator.getString("gui.installing") + ": " + version);
			if (version.equals(LOCAL_VERSION_STRING)) {
				new Thread(() -> {
					try {
						LocalVersionInstaller.installServer(new File(installLocation.getText()), this);

					} catch (Exception e) {
						e.printStackTrace();
						error(e.getLocalizedMessage());
					}
				}).start();
				return;
			}
			new Thread(() -> {

				updateProgress(Translator.getString("gui.installing") + ": " + version, 0);
				try {
					ServerInstaller.install(new File(installLocation.getText()), version, this);
				} catch (Exception e) {
					e.printStackTrace();
					error(e.getMessage());
				}

			}).start();
		} else if(multimcRadioButton.isSelected()){
			String version = (String) versionComboBox.getSelectedItem();
			if(version.equals(LOCAL_VERSION_STRING)){
				error(Translator.getString("install.multimc.local"));
				return;
			}
			System.out.println(Translator.getString("gui.installing") + ": " + version);
			new Thread(() -> {
				updateProgress(Translator.getString("gui.installing") + ": " + version, 0);
				try {
					MultiMCInstaller.install(new File(installLocation.getText()), version, this);
				} catch (Exception e) {
					e.printStackTrace();
					error(e.getMessage());
				}
			}).start();
		}
	}

	public void selectInstallLocation() {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(installLocation.getText()));
		chooser.setDialogTitle(Translator.getString("gui.selectInstallLocation"));
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
		throws ParserConfigurationException, XmlPullParserException, SAXException, IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException,
		IllegalAccessException {
		//This will make people happy
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		MainGui dialog = new MainGui();
		dialog.pack();
		dialog.setTitle(Translator.getString("fabric.installer.name"));
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	private void initComponents() {
		contentPane = new JPanel();
		contentPane.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
		final JPanel panel1 = new JPanel();
		panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
		final Spacer spacer1 = new Spacer();
		panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
		panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		buttonInstall = new JButton();
		buttonInstall.setText(Translator.getString("gui.install"));
		panel2.add(buttonInstall, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JPanel panel3 = new JPanel();
		panel3.setLayout(new GridLayoutManager(4, 4, new Insets(0, 0, 0, 0), -1, -1));
		contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
		final JLabel label1 = new JLabel();
		label1.setText(Translator.getString("fabric.installer.name"));
		panel3.add(label1, new GridConstraints(0, 0, 1, 4, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label2 = new JLabel();
		label2.setText(Translator.getString("gui.selectSide"));
		panel3.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		clientRadioButton = new JRadioButton();
		clientRadioButton.setText(Translator.getString("gui.client"));
		panel3.add(clientRadioButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		serverRadioButton = new JRadioButton();
		serverRadioButton.setText(Translator.getString("gui.server"));
		panel3.add(serverRadioButton, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
		multimcRadioButton = new JRadioButton();
		multimcRadioButton.setText(Translator.getString("gui.multimc"));
		multimcRadioButton.setEnabled(false);
		panel3.add(multimcRadioButton, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label3 = new JLabel();
		label3.setText(Translator.getString("gui.selectInstallLocation"));
		panel3.add(label3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		installLocation = new JTextField();
		panel3.add(installLocation, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
		selectFolderButton = new JButton();
		selectFolderButton.setText("...");
		panel3.add(selectFolderButton, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		final JLabel label4 = new JLabel();
		label4.setText(Translator.getString("gui.selectVersion"));
		panel3.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		versionComboBox = new JComboBox();
		panel3.add(versionComboBox, new GridConstraints(1, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		progressBar = new JProgressBar();
		contentPane.add(progressBar, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		statusLabel = new JLabel();
		statusLabel.setText(Translator.getString("gui.status.ready"));
		contentPane.add(statusLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
		ButtonGroup buttonGroup;
		buttonGroup = new ButtonGroup();
		buttonGroup.add(clientRadioButton);
		buttonGroup.add(serverRadioButton);
		buttonGroup.add(multimcRadioButton);
	}

}
