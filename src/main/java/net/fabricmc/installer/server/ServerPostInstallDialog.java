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

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import mjson.Json;

import net.fabricmc.installer.InstallerGui;
import net.fabricmc.installer.util.LauncherMeta;
import net.fabricmc.installer.util.Utils;

@SuppressWarnings("serial")
public class ServerPostInstallDialog extends JDialog {
	private static final String launchCommand = "java -Xmx2G -jar fabric-server-launch.jar nogui";
	private static final int MB = 1000000;

	private final JPanel panel = new JPanel();

	private final ServerHandler serverHandler;
	private final String minecraftVersion;
	private final Path installDir;
	private final Path minecraftJar;
	private final Path minecraftJarTmp;

	private JLabel serverJarLabel;
	private JButton downloadButton;
	private JButton generateButton;

	private ServerPostInstallDialog(ServerHandler handler) throws HeadlessException {
		super(InstallerGui.instance, true);
		this.serverHandler = handler;
		this.minecraftVersion = (String) handler.gameVersionComboBox.getSelectedItem();
		this.installDir = Paths.get(handler.installLocation.getText());
		this.minecraftJar = installDir.resolve("server.jar");
		this.minecraftJarTmp = installDir.resolve("server.jar.tmp");

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
			JTextField textField = new JTextField(launchCommand);
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
		if (!Files.exists(minecraftJar)) {
			return false;
		}

		try (JarFile jarFile = new JarFile(minecraftJar.toFile())) {
			JarEntry versionEntry = jarFile.getJarEntry("version.json");

			if (versionEntry == null) {
				return false;
			}

			InputStream inputStream = jarFile.getInputStream(versionEntry);

			String text;

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
				text = reader.lines().collect(Collectors.joining("\n"));
			}

			Json json = Json.read(text);
			String id = json.at("id").asString();
			String name = json.at("name").asString();

			return minecraftVersion.equals(id) || minecraftVersion.equals(name);
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
			Files.deleteIfExists(minecraftJar);
			Files.deleteIfExists(minecraftJarTmp);
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

				try (BufferedInputStream inputStream = new BufferedInputStream(httpConnection.getInputStream());
						OutputStream outputStream = Files.newOutputStream(minecraftJarTmp)) {
					byte[] buffer = new byte[4096];
					long downloaded = 0;
					int len;

					while ((len = inputStream.read(buffer, 0, buffer.length)) >= 0) {
						downloaded += len;

						final String labelText = new MessageFormat(Utils.BUNDLE.getString("prompt.server.downloading")).format(new Object[] {downloaded / MB, finalSize / MB});
						SwingUtilities.invokeLater(() -> color(serverJarLabel, Color.BLUE).setText(labelText));

						outputStream.write(buffer, 0, len);
					}
				}

				Files.move(minecraftJarTmp, minecraftJar, StandardCopyOption.REPLACE_EXISTING);

				updateServerJarLabel();
				downloadButton.setEnabled(true);
			} catch (IOException e) {
				color(serverJarLabel, Color.RED).setText(e.getMessage());
				serverHandler.error(e);
			}
		}).start();
	}

	private void generateLaunchScripts() {
		Map<Path, String> launchScripts = new HashMap<>();
		launchScripts.put(installDir.resolve("start.bat"), launchCommand + "\npause");
		launchScripts.put(installDir.resolve("start.sh"), "#!/usr/bin/env bash\n" + launchCommand);

		boolean exists = launchScripts.entrySet().stream().anyMatch(entry -> Files.exists(entry.getKey()));

		if (exists && (JOptionPane.showConfirmDialog(this, Utils.BUNDLE.getString("prompt.server.overwrite"), "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)) {
			return;
		}

		launchScripts.forEach((path, s) -> {
			try {
				Utils.writeToFile(path, s);
				path.toFile().setExecutable(true, false);
			} catch (IOException e) {
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
