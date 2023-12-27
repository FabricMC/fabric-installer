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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.xml.stream.XMLStreamException;

import net.fabricmc.installer.util.Utils;

@SuppressWarnings("serial")
public class InstallerGui extends JFrame {
	public static InstallerGui instance;

	private JTabbedPane contentPane;

	public InstallerGui() throws IOException {
		initComponents();
		setContentPane(contentPane);

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		Image iconImage = Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("icon.png"));
		setIconImage(iconImage);
		setTaskBarImage(iconImage);

		instance = this;

		Main.loadMetadata();
	}

	public static void selectInstallLocation(Supplier<String> initalDir, Consumer<String> selectedDir) {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File(initalDir.get()));
		chooser.setDialogTitle(Utils.BUNDLE.getString("prompt.select.location"));
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(false);

		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			selectedDir.accept(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	public static void start() throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, XMLStreamException {
		//This will make people happy
		String lafCls = UIManager.getSystemLookAndFeelClassName();
		UIManager.setLookAndFeel(lafCls);

		if (lafCls.endsWith("AquaLookAndFeel")) { // patch osx tab text color bug JDK-8251377
			UIManager.put("TabbedPane.foreground", Color.BLACK);
		}

		InstallerGui dialog = new InstallerGui();
		dialog.updateSize(true);
		dialog.setTitle(Utils.BUNDLE.getString("installer.title"));
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	public void updateSize(boolean updateMinimum) {
		if (updateMinimum) setMinimumSize(null);
		setPreferredSize(null);
		pack();
		Dimension size = getPreferredSize();
		if (updateMinimum) setMinimumSize(size);
		setPreferredSize(new Dimension(Math.max(450, size.width), size.height));
		setSize(getPreferredSize());
	}

	private void initComponents() {
		contentPane = new JTabbedPane(JTabbedPane.TOP);
		Main.HANDLERS.forEach(handler -> contentPane.addTab(Utils.BUNDLE.getString("tab." + handler.name().toLowerCase(Locale.ROOT)), handler.makePanel(this)));
	}

	private static void setTaskBarImage(Image image) {
		try {
			// Only supported in Java 9 +
			Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
			Method getTaskbar = taskbarClass.getDeclaredMethod("getTaskbar");
			Method setIconImage = taskbarClass.getDeclaredMethod("setIconImage", Image.class);
			Object taskbar = getTaskbar.invoke(null);
			setIconImage.invoke(taskbar, image);
		} catch (Exception e) {
			// Ignored, running on Java 8
		}
	}
}
