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

import net.fabricmc.installer.client.ClientGui;
import net.fabricmc.installer.server.ServerGui;
import net.fabricmc.installer.util.MavenHandler;
import net.fabricmc.installer.util.Reference;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class InstallerGui extends JFrame {

	private JTabbedPane contentPane;

	private BaseGui clientGui;
	private BaseGui serverGui;

	public final MavenHandler loaderMaven = new MavenHandler();
	public final MavenHandler mappingsMaven = new MavenHandler();

	public InstallerGui() throws IOException, XMLStreamException {
		clientGui = new ClientGui();
		serverGui = new ServerGui();

		initComponents();
		setContentPane(contentPane);

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setIconImage(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("icon.png")));

		mappingsMaven.load(Reference.MAVEN_SERVER_URL, Reference.PACKAGE, Reference.MAPPINGS_NAME);
		loaderMaven.load(Reference.MAVEN_SERVER_URL, Reference.PACKAGE, Reference.LOADER_NAME);
	}

	public static void selectInstallLocation(Supplier<String> initalDir, Consumer<String> selectedDir) {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(initalDir.get()));
		chooser.setDialogTitle("Select Install Location");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			selectedDir.accept(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	public static void start() throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, XMLStreamException {
		//This will make people happy
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		InstallerGui dialog = new InstallerGui();
		dialog.pack();
		dialog.setTitle("Fabric Installer");
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	private void initComponents() {
		contentPane = new JTabbedPane(JTabbedPane.TOP);

		contentPane.addTab("Client", clientGui.makePanel(this));
		contentPane.addTab("Server", serverGui.makePanel(this));
	}

	private void addRow(Container parent, Consumer<JPanel> consumer) {
		JPanel panel = new JPanel(new FlowLayout());
		consumer.accept(panel);
		parent.add(panel);
	}

}
