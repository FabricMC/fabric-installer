package net.fabricmc.installer;

import cuchaz.enigma.throwables.MappingParseException;
import net.fabricmc.installer.gui.MainGui;
import net.fabricmc.installer.installer.ServerInstaller;
import net.fabricmc.installer.util.IInstallerProgress;
import net.fabricmc.installer.util.VersionInfo;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.logging.*;

public class Main {

    public static ResourceBundle languageBundle;

    public static void main(String[] args)
            throws ParserConfigurationException, XmlPullParserException, SAXException, IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException,
            IllegalAccessException, MappingParseException {

        Locale locale = new Locale(System.getProperty("user.language"), System.getProperty("user.country"));
        try {
            languageBundle = ResourceBundle.getBundle("Lang", locale);
        } catch (MissingResourceException e) {
            System.out.println("Could not find language file for " + locale.toString() + ", english will be used");
            locale = new Locale("en", "US");
            languageBundle = ResourceBundle.getBundle("Lang", locale);
        }

        System.out.println(languageBundle.getString("fabric.installer.load"));

        //Used to suppress warning from libs
        setDebugLevel(Level.SEVERE);

        if (args.length == 0) {
            MainGui.start();
        } else if (args[0].equals("help")) {
            System.out.println(languageBundle.getString("cli.help.title"));
            System.out.println(languageBundle.getString("cli.help.noArgs"));
            System.out.println(languageBundle.getString("cli.help.nogui"));
        } else if (args[0].equals("nogui")) {
            System.out.println("Fabric Server cli installer");
            System.out.println("Loading available versions for install");
            VersionInfo.load();
            File runDir = new File(".");
            System.out.println("Current directory: " + runDir.getAbsolutePath());
            if (getUserInput("Is this the directory you want to install the server into? (Y/n)").equals("Y")) {

            } else {
                runDir = new File(getUserInput("Enter the directory you wish to install to"));
                if (!runDir.exists() || !runDir.isDirectory()) {
                    System.out.println("That location is not valid, either its not a folder, or does not exist");
                    return;
                }
            }
            if (runDir.listFiles().length != 0) {
                if (!getUserInput("The current select install location is not empty, are you sure you want to install here? (Y/n)").equals("Y")) {
                    return;
                }
            }

            System.out.println("The latest version available to install is " + VersionInfo.latestVersion);
            String version = VersionInfo.latestVersion;
            if (getUserInput("Would you like to install this version or pick another one? (Y/n)").equals("Y")) {

            } else {
                for (String str : VersionInfo.versions) {
                    System.out.println(VersionInfo.versions.indexOf(str) + " - " + str);
                }
                int value = Integer.parseInt(getUserInput("Please enter the number corresponding to the version to wish to install"));
                if (value < 0 || value > VersionInfo.versions.size()) {
                    System.out.println("That isn't a valid version!");
                    return;
                }
                version = VersionInfo.versions.get(value);
            }
            ServerInstaller.install(runDir, version, new IInstallerProgress() {
                @Override
                public void updateProgress(String text, int percentage) {
                    System.out.println(percentage + "% - " + text);
                }

                @Override
                public void error(String error) {
                    System.out.println("A fatal error has occurred:");
                    System.out.println(error);
                }
            });
        } else if (args.length == 3) {
            //TODO install without any user input
        }

    }

    public static String getUserInput(String question) {
        Scanner in = new Scanner(System.in);
        System.out.println(question);
        return in.nextLine();
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
