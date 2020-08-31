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

import com.google.gson.JsonObject;
import net.fabricmc.installer.InstallerGui;
import net.fabricmc.installer.util.LauncherMeta;
import net.fabricmc.installer.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ServerPostInstallDialog extends JDialog {

	private static final int MB = 1000000;

	private JPanel panel = new JPanel();

	private ServerHandler serverHandler;
	private String minecraftVersion;
	private File installDir;
	private File minecraftJar;
	private File minecraftJarTmp;

	private JLabel serverJarLabel;
	private JButton downloadButton;
	private JButton generateButton;

	private ServerPostInstallDialog(ServerHandler handler) throws HeadlessException {
		super(InstallerGui.instance, true);
		this.serverHandler = handler;
		this.minecraftVersion = (String) handler.gameVersionComboBox.getSelectedItem();
		this.installDir = new File(handler.installLocation.getText());
		this.minecraftJar = new File(installDir, "server.jar");
		this.minecraftJarTmp = new File(installDir, "server.jar.tmp");

		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		initComponents();
		setContentPane(panel);
		setIconImage(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("icon.png")));
	}

	private void initComponents() {
		addRow(panel, panel -> panel.add(fontSize(new JLabel(Utils.BUNDLE.getString("progress.done.server")), 20)));

		addRow(panel, panel -> panel.add(fontSize(new JLabel(Utils.BUNDLE.getString("prompt.server.info.jar")), 15)));
		addRow(panel, panel -> {
			updateServerJarLabel();
			panel.add(serverJarLabel);

			downloadButton = new JButton(Utils.BUNDLE.getString("prompt.server.jar"));
			downloadButton.addActionListener(e -> doServerJarDownload());
			panel.add(downloadButton);
		});

		addRow(panel, panel -> panel.add(fontSize(new JLabel(Utils.BUNDLE.getString("prompt.server.info.command")), 15)));
		addRow(panel, panel -> {
			JTextField textField = new JTextField("java -jar fabric-server-launch.jar");
			textField.setHorizontalAlignment(JTextField.CENTER);
			panel.add(textField);


		});

		addRow(panel, panel -> {
			panel.add(new JLabel(Utils.BUNDLE.getString("prompt.server.info.scipt")));
			generateButton = new JButton(Utils.BUNDLE.getString("prompt.server.generate"));
			generateButton.addActionListener(e -> generateLaunchScripts());
			panel.add(generateButton);
		});

		addRow(panel, panel -> {
			JButton closeButton = new JButton(Utils.BUNDLE.getString("progress.done"));
			closeButton.addActionListener(e -> {
				setVisible(false);
				dispose();
			});
			panel.add(closeButton);
		});

	}

	private boolean isValidJarPresent() {
		if (!minecraftJar.exists()) {
			return false;
		}
		try (JarFile jarFile = new JarFile(minecraftJar)) {
			JarEntry versionEntry = jarFile.getJarEntry("version.json");
			if (versionEntry == null) {
				return false;
			}
			InputStream inputStream = jarFile.getInputStream(versionEntry);
			JsonObject jsonObject = Utils.GSON.fromJson(new InputStreamReader(inputStream), JsonObject.class);
			return jsonObject.get("id").getAsString().equals(minecraftVersion) || jsonObject.get("name").getAsString().equals(minecraftVersion);
		} catch (IOException e) {
			return false;
		}
	}

	private void updateServerJarLabel() {
		if (serverJarLabel == null) {
			serverJarLabel = new JLabel();
		}
		if (isValidJarPresent()) {
			serverJarLabel.setText(new MessageFormat(Utils.BUNDLE.getString("prompt.server.jar.valid")).format(new Object[]{minecraftVersion}));
			color(serverJarLabel, Color.GREEN.darker());
		} else {
			serverJarLabel.setText(new MessageFormat(Utils.BUNDLE.getString("prompt.server.jar.invalid")).format(new Object[]{minecraftVersion}));
			color(serverJarLabel, Color.RED);
		}
	}

	private void doServerJarDownload() {
		downloadButton.setEnabled(false);
		try {
			Files.deleteIfExists(minecraftJar.toPath());
			Files.deleteIfExists(minecraftJarTmp.toPath());
		} catch (IOException e) {
			color(serverJarLabel, Color.RED).setText(e.getMessage());
			serverHandler.error(e);
			return;
		}
		new Thread(() -> {
			try {
				URL url = new URL(LauncherMeta.getLauncherMeta().getVersion(minecraftVersion).getVersionMeta().downloads.get("server").url);
				HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
				int finalSize = httpConnection.getContentLength();

				BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());

				FileOutputStream outputStream = new FileOutputStream(minecraftJarTmp);
				BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, 1024);

				byte[] buffer = new byte[1024];
				long downloaded = 0;
				int len;
				while ((len = inputStream.read(buffer, 0, 1024)) >= 0) {
					downloaded += len;

					final String labelText = String.format("Downloading %d/%d MB", downloaded / MB, finalSize / MB);
					SwingUtilities.invokeLater(() -> color(serverJarLabel, Color.BLUE).setText(labelText));

					bufferedOutputStream.write(buffer, 0, len);
				}
				bufferedOutputStream.close();
				inputStream.close();

				Files.move(minecraftJarTmp.toPath(), minecraftJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

				updateServerJarLabel();
				downloadButton.setEnabled(true);

			} catch (IOException e) {
				color(serverJarLabel, Color.RED).setText(e.getMessage());
				serverHandler.error(e);
			}
		}).start();
	}

	private void generateLaunchScripts() {
		String launchCommand = "java -jar fabric-server-launch.jar";

		Map<File, String> launchScripts = new HashMap<>();
		launchScripts.put(new File(installDir, "start.bat"), launchCommand + "\npause");
		launchScripts.put(new File(installDir, "start.sh"), "#!/usr/bin/env bash\n" + launchCommand);

		boolean exists = launchScripts.entrySet().stream().anyMatch(entry -> entry.getKey().exists());
		if (exists && (JOptionPane.showConfirmDialog(this, Utils.BUNDLE.getString("prompt.server.overwrite"), "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)) {
			return;
		}

		launchScripts.forEach((file, s) -> {
			try {
				Utils.writeToFile(file, s);
				file.setExecutable(true, false);
			} catch (FileNotFoundException e) {
				serverHandler.error(e);
			}
		});
	}

	private JLabel fontSize(JLabel label, int size) {
		label.setFont(new Font(label.getFont().getName(), Font.PLAIN, size));
		return label;
	}

	private JLabel color(JLabel label, Color color) {
		label.setForeground(color);
		return label;
	}

	private void addRow(Container parent, Consumer<JPanel> consumer) {
		JPanel panel = new JPanel(new FlowLayout());
		consumer.accept(panel);
		parent.add(panel);
	}

	public static void show(ServerHandler serverHandler) throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		ServerPostInstallDialog dialog = new ServerPostInstallDialog(serverHandler);
		dialog.pack();
		dialog.setTitle(Utils.BUNDLE.getString("installer.title"));
		dialog.setLocationRelativeTo(InstallerGui.instance);
		dialog.setVisible(true);
	}
}
