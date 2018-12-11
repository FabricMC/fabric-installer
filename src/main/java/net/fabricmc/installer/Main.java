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

import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Translator;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import javax.swing.*;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.*;

public class Main {

	public static void main(String[] args) throws XmlPullParserException, IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
		Locale locale = new Locale(System.getProperty("user.language"), System.getProperty("user.country"));
		if (!Translator.INSTANCE.isValid(locale)) {
			locale = new Locale("en", "US");
		}

		Translator.INSTANCE.load(locale);

		String[] versionSplit = System.getProperty("java.version").split("\\.");
		int javaVersionMajor = Integer.parseInt(versionSplit[0]);
		int javaVersionMinor = Integer.parseInt(versionSplit[1]);
		if (javaVersionMinor < 8 && javaVersionMajor <= 1) {
			String outdatedVersion = Translator.INSTANCE.getString("error.outdatedJava");

			System.out.println(outdatedVersion);
			if (args.length == 0 || !args[0].equals("nogui")) {
				JOptionPane.showMessageDialog(null, outdatedVersion, "Java Version Warning", JOptionPane.ERROR_MESSAGE);
			}
		}

		System.out.println(Translator.INSTANCE.getString("fabric.installer.load") + ":" + Reference.VERSION);

		//Used to suppress warning from libs
		setDebugLevel(Level.SEVERE);

		InstallerGui.start();
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
