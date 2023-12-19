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

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.fabricmc.installer.InstallerGui;
import net.fabricmc.installer.util.Utils;

@SuppressWarnings("serial")
public class ServerPostInstallDialog extends JDialog {
	private static final String launchCommand = "java -Xmx2G -jar fabric-server-launch.jar nogui";

	private final JPanel panel = new JPanel();

	private final ServerHandler serverHandler;
	private final Path installDir;

	private JButton generateButton;

	private ServerPostInstallDialog(ServerHandler handler) throws HeadlessException {
		super(InstallerGui.instance, true);
		this.serverHandler = handler;
		this.installDir = Paths.get(handler.installLocation.getText());

		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		initComponents();
		setContentPane(panel);
		setIconImage(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("icon.png")));
	}

	private void initComponents() {
		addRow(panel, panel -> panel.add(fontSize(new JLabel(Utils.BUNDLE.getString("progress.done.server")), 20)));

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
