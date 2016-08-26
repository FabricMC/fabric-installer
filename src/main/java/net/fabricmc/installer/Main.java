package net.fabricmc.installer;

import net.fabricmc.installer.gui.MainGui;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main {

	public static void main(String[] args)
		throws ParserConfigurationException, XmlPullParserException, SAXException, IOException, ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException,
		IllegalAccessException {
		MainGui.start();
	}

}
