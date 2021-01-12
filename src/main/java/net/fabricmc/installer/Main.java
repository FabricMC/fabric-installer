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
import net.fabricmc.installer.util.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class Main {

	public static MetaHandler GAME_VERSION_META;
	public static MetaHandler LOADER_META;

	//TODO is gui the best name for this?
	public static final List<Handler> HANDLERS = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		String[] versionSplit = System.getProperty("java.version").split("\\.");
		boolean showGui = args.length == 0 || !args[0].equals("nogui");
		if (versionSplit.length == 2) { //Only check 1.x versions of java, new versions are formatted liked 12
			int javaVersionMajor = Integer.parseInt(versionSplit[0]);
			int javaVersionMinor = Integer.parseInt(versionSplit[1]);
			if (javaVersionMinor < 8 && javaVersionMajor <= 1) {
				System.out.println("You are using an outdated version of Java, Fabric will not work! Please update to Java 8 or newer to use Fabric.");
				if (showGui) {
					JOptionPane.showMessageDialog(null, "You are using an outdated version of Java, Fabric will not work! Please update to Java 8 or newer to use Fabric.", "Java Version Warning", JOptionPane.ERROR_MESSAGE);
				}
				return;
			}
		}
		try {
			Utils.launcherCheck(showGui);
		} catch (IOException e) {
			e.printStackTrace();
			if (showGui) {
				JOptionPane.showMessageDialog(null, "Failed to check if the Minecraft launcher is open! Please make sure it's closed so fabric can be installed properly.", "Minecraft Launcher Warning", JOptionPane.WARNING_MESSAGE);
			}
		}
		System.out.println("Loading Fabric Installer: " + Main.class.getPackage().getImplementationVersion());

		HANDLERS.add(new ClientHandler());
		HANDLERS.add(new ServerHandler());

		ArgumentParser argumentParser = ArgumentParser.create(args);
		String command = argumentParser.getCommand().orElse(null);

		//Used to suppress warning from libs
		setDebugLevel(Level.SEVERE);

		//Can be used if you wish to re-host or provide custom versions. Ensure you include the trailing /
		argumentParser.ifPresent("mavenurl", s -> Reference.mavenServerUrl = s);
		final String metaUrl = argumentParser.getOrDefault("metaurl", () -> "https://meta.fabricmc.net/");

		GAME_VERSION_META = new MetaHandler(metaUrl + "v2/versions/game");
		LOADER_META = new MetaHandler(metaUrl + "v2/versions/loader");

		//Default to the help command in a headless environment
		if(GraphicsEnvironment.isHeadless() && command == null){
			command = "help";
		}

		if (command == null) {
			try {
				InstallerGui.start();
			} catch (Exception e){
				e.printStackTrace();
				new CrashDialog(e);
			}
		} else if (command.equals("help")) {
			System.out.println("help - Opens this menu");
			HANDLERS.forEach(handler -> System.out.printf("%s %s\n", handler.name().toLowerCase(), handler.cliHelp()));

			LOADER_META.load();
			GAME_VERSION_META.load();

			System.out.printf("\nLatest Version: %s\nLatest Loader: %s\n", GAME_VERSION_META.getLatestVersion(argumentParser.has("snapshot")).getVersion(), Main.LOADER_META.getLatestVersion(false).getVersion());
		} else {
			for (Handler handler : HANDLERS) {
				if (command.equalsIgnoreCase(handler.name())) {
					try {
						handler.installCli(argumentParser);
					} catch (Exception e) {
						throw new RuntimeException("Failed to install " + handler.name(), e);
					}
					return;
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
