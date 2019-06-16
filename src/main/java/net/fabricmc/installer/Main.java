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

import net.fabricmc.installer.client.ClientHandler;
import net.fabricmc.installer.server.ServerHandler;
import net.fabricmc.installer.util.ArgumentParser;
import net.fabricmc.installer.util.MavenHandler;
import net.fabricmc.installer.util.MetaHandler;
import net.fabricmc.installer.util.Reference;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {

	public static final MetaHandler GAME_VERSION_META = new MetaHandler("https://meta.fabricmc.net/v2/versions/game");
	public static final MavenHandler LOADER_MAVEN = new MavenHandler(Reference.MAVEN_SERVER_URL, Reference.PACKAGE, Reference.LOADER_NAME);

	//TODO is gui the best name for this?
	public static final List<Handler> HANDLERS = new ArrayList<>();

	public static void main(String[] args) throws IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException, XMLStreamException {
		String[] versionSplit = System.getProperty("java.version").split("\\.");
		if (versionSplit.length == 2) { //Only check 1.x versions of java, new versions are formatted liked 12
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

		HANDLERS.add(new ClientHandler());
		HANDLERS.add(new ServerHandler());

		ArgumentParser argumentParser = ArgumentParser.create(args);
		String command = argumentParser.getCommand().orElse(null);

		//Used to suppress warning from libs
		setDebugLevel(Level.SEVERE);

		if (command == null) {
			InstallerGui.start();
		} else if (command.equals("help")) {
			System.out.println("help - Opens this menu");
			HANDLERS.forEach(handler -> System.out.printf("%s %s\n", handler.name().toLowerCase(), handler.cliHelp()));

			LOADER_MAVEN.load();
			GAME_VERSION_META.load();

			System.out.printf("\nLatest Version: %s\nLatest Loader: %s\n", GAME_VERSION_META.getLatestVersion(argumentParser.has("snapshot")).getVersion(), Main.LOADER_MAVEN.latestVersion);
		} else {
			for (Handler handler : HANDLERS) {
				if (command.equalsIgnoreCase(handler.name())) {
					try {
						handler.installCli(argumentParser);
					} catch (Exception e) {
						throw new RuntimeException("Failed to install " + handler.name(), e);
					}
					break;
				}
			}
			//Only reached if a handler is not found
			System.out.println("No handler found for " + args[0] + " see help");
		}

	}

	public static void setDebugLevel(Level newLvl) {
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		java.util.logging.Handler[] handlers = rootLogger.getHandlers();
		rootLogger.setLevel(newLvl);
		for (java.util.logging.Handler h : handlers) {
			if (h instanceof FileHandler)
				h.setLevel(newLvl);
		}
	}

}
