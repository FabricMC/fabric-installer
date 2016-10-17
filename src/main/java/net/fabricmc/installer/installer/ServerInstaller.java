package net.fabricmc.installer.installer;


import net.fabricmc.installer.util.IInstallerProgress;
import net.fabricmc.installer.util.Translator;
import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.zeroturnaround.zip.NameMapper;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class ServerInstaller {

    public static void install(File mcDir, String version, IInstallerProgress progress) throws IOException {
        progress.updateProgress(Translator.getString("gui.installing") + ": " + version, 0);
        String[] split = version.split("-");
        String mcVer = split[0];
        String fabricVer = split[1];

        File fabricJar = new File(mcDir, "fabric-" + version + ".jar");
        File mcJar = new File(mcDir, "minecraft_server." + mcVer + ".jar");
        if (fabricJar.exists()) {
            fabricJar.delete();
        }
        if (mcJar.exists()) {
            mcJar.delete();
        }
        progress.updateProgress(Translator.getString("install.server.downloadFabric"), 5);
        FileUtils.copyURLToFile(new URL("http://maven.fabricmc.net/net/fabricmc/fabric-base/" + version + "/fabric-base-" + version + ".jar"), fabricJar);
        progress.updateProgress(Translator.getString("install.server.downloadServer"), 10);
        FileUtils.copyURLToFile(new URL("https://s3.amazonaws.com/Minecraft.Download/versions/" + mcVer + "/minecraft_server." + mcVer + ".jar"), mcJar);

        progress.updateProgress(Translator.getString("install.server.createTemp"), 15);
        File tempDir = new File(mcDir, "temp");
        if (tempDir.exists()) {
            FileUtils.deleteDirectory(tempDir);
        }
        tempDir.mkdir();

        progress.updateProgress("Extracting Mappings", 20);
        ZipUtil.unpack(fabricJar, tempDir, name -> {
            if (name.startsWith("pomf-" + split[0])) {
                return name;
            } else {
                return null;
            }
        });

        List<File> libs = new ArrayList<>();
        libs.addAll(getAndDownloadLibs("net.fabricmc:fabric-base:" + version, true));
        libs.addAll(getAndDownloadLibs("net.sf.jopt-simple:jopt-simple:5.0.2", true));

        List<String> extracted = new ArrayList<>();
        for (File lib : libs) {
            if (!extracted.contains(lib.getAbsolutePath()) && !lib.getName().contains("lwjgl") && !lib.getName().contains("fabric-base")) {
                extracted.add(lib.getAbsolutePath());
//                ZipUtil.unpack(lib, mcExtractedDir, name -> {
//                    if (name.startsWith("META-INF")) {
//                        return null;
//                    } else {
//                        return name;
//                    }
//                });
            }
        }
        progress.updateProgress(Translator.getString("install.server.packJar"), 90);
        mcJar.delete();

        FileUtils.deleteQuietly(tempDir);

        progress.updateProgress(Translator.getString("install.success"), 100);
    }

    public static List<File> getAndDownloadLibs(String name, boolean getDeps) {
        ArrayList<File> libs = new ArrayList<>();
        File[] files;
        if (getDeps) {
            files = Maven.configureResolver().withRemoteRepo("fabricmc", "http://maven.fabricmc.net/", "default").withRemoteRepo("mojang", "https://libraries.minecraft.net/", "default").withRemoteRepo("spongepowered", "https://repo.spongepowered.org/maven/", "default")
                    .resolve(name).withTransitivity().asFile();
        } else {
            files = Maven.configureResolver().withRemoteRepo("fabricmc", "http://maven.fabricmc.net/", "default").withRemoteRepo("mojang", "https://libraries.minecraft.net/", "default").withRemoteRepo("spongepowered", "https://repo.spongepowered.org/maven/", "default")
                    .resolve(name).withoutTransitivity().asFile();
        }

        for (File file : files) {
            libs.add(file);
        }
        return libs;
    }
}
