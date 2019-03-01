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

import net.fabricmc.installer.util.IInstallerProgress;
import net.fabricmc.installer.util.Version;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.*;

public class Main {

	public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, XMLStreamException {
		String[] versionSplit = System.getProperty("java.version").split("\\.");
		if(versionSplit.length == 2){ //Only check 1.x versions of java, new versions are formatted liked 12
			int javaVersionMajor = Integer.parseInt(versionSplit[0]);
			int javaVersionMinor = Integer.parseInt(versionSplit[1]);
			if (javaVersionMinor < 8 && javaVersionMajor <= 1) {
				System.out.println("You are using an outdated version of Java, Fabric will not work! Please update to Java 8 or newer to use Fabric.");
				if (args.length == 0 || !args[0].equals("nogui")) {
					JOptionPane.showMessageDialog(null, "You are using an outdated version of Java, Fabric will not work! Please update to Java 8 or newer to use Fabric.", "Java Version Warning", JOptionPane.ERROR_MESSAGE);
				}
				return;
			}
		}

		System.out.println("Loading Fabric Installer: " + Main.class.getPackage().getImplementationVersion());

		//Used to suppress warning from libs
		setDebugLevel(Level.SEVERE);

		if (args.length == 0) {
			InstallerGui.start();
		} else if (args[0].equals("help") || args.length != 4) {
			System.out.println("installer.jar help - this");
			System.out.println("installer.jar nogui <launcher dir> <mappings version> <loader version>");
			System.out.println("Mappings example: 18w49a.11 ,Loader example: 0.2.0.62");
		} else if (args[0].equals("nogui")) {
			File file = new File(args[1]);
			if (!file.exists()) {
				throw new FileNotFoundException("Launcher directory not found");
			}
			Version version = new Version(args[2]);
			String loaderVersion = args[3];
			String profileName = ClientInstaller.install(file, version, loaderVersion, new IInstallerProgress() {
				@Override
				public void updateProgress(String text) {
					System.out.println(text);
				}

				@Override
				public void error(String error) {
					throw new RuntimeException(error);
				}
			});
			ProfileInstaller.setupProfile(file, profileName, version);
		}

	}

	public static void setDebugLevel(Level newLvl) {
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		rootLogger.setLevel(newLvl);
		for (Handler h : handlers) {
			if (h instanceof FileHandler)
				h.setLevel(newLvl);
		}
	}

}
