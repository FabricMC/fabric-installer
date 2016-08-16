package net.fabricmc.installer.utill;


import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class VersionInfo {

    public static String latestVersion = "";
    public static List<String> versions;


    public static void load() throws IOException, ParserConfigurationException, SAXException, XmlPullParserException {
        String baseMavenMeta = IOUtils.toString(new URL("https://maven.fabricmc.net/net/fabricmc/fabric-base/maven-metadata.xml"), "UTF-8");
        Metadata metadata = new MetadataXpp3Reader().read(new StringReader(baseMavenMeta));
        latestVersion = metadata.getVersioning().getRelease();
        versions = metadata.getVersioning().getVersions();
        Collections.reverse(versions);
    }


}
