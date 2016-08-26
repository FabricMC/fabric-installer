package net.fabricmc.installer.installer;

import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.TranslatingTypeLoader;
import cuchaz.enigma.mapping.MappingsEnigmaReader;
import cuchaz.enigma.mapping.TranslationDirection;
import cuchaz.enigma.throwables.MappingParseException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.InnerClassesAttribute;
import net.fabricmc.installer.utill.IInstallerProgress;
import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

/**
 * Created by Mark on 26/08/2016.
 */
public class ServerInstaller {

	public static void install(File mcDir, String version, IInstallerProgress progress) throws IOException, MappingParseException {
		progress.updateProgress("Installing: " + version, 0);
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
		progress.updateProgress("Downloading fabric", 5);
		FileUtils.copyURLToFile(new URL("http://maven.fabricmc.net/net/fabricmc/fabric-base/" + version + "/fabric-base-" + version + ".jar"), fabricJar);
		progress.updateProgress("Downloading minecraft server jar", 10);
		FileUtils.copyURLToFile(new URL("https://s3.amazonaws.com/Minecraft.Download/versions/" + mcVer + "/minecraft_server." + mcVer + ".jar"), mcJar);

		progress.updateProgress("Creating temporary directory for files", 15);
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
		File mappingsDir = new File(tempDir, "pomf-" + split[0] + File.separator + "mappings");
		File mcMappedJar = new File(tempDir, "minecraft_server.mapped." + mcVer + ".jar");
		File mcExtractedDir = new File(tempDir, "minecraft_server_extracted");
		File mcCleanedJar = new File(tempDir, "minecraft_server_clean.jar");

		ZipUtil.unpack(mcJar, mcExtractedDir, name -> {
			if (name.contains("/") && !name.startsWith("net")) {
				return null;
			} else {
				return name;
			}
		});
		ZipUtil.pack(mcExtractedDir, mcCleanedJar);

		progress.updateProgress("Loading jar file into deobfuscator", 30);
		Deobfuscator deobfuscator = new Deobfuscator(new JarFile(mcCleanedJar));
		progress.updateProgress("Reading mappings", 40);
		deobfuscator.setMappings(new MappingsEnigmaReader().read(mappingsDir));
		progress.updateProgress("Writing out new jar file", 50);
		writeJar(mcMappedJar, new ProgressListener(), deobfuscator);
		progress.updateProgress("Extracting library's", 60);

		FileUtils.deleteDirectory(mcExtractedDir);

		ZipUtil.unpack(mcJar, mcExtractedDir, name -> {
			if (name.contains("/") && !name.startsWith("net") || !name.endsWith(".class")) {
				return name;
			} else {
				return null;
			}
		});
		ZipUtil.unpack(mcMappedJar, mcExtractedDir);
		progress.updateProgress("Downloading library's", 70);
		List<File> libs = new ArrayList<>();
		libs.addAll(getAndDownloadLibs("net.fabricmc:fabric-base:" + version, true));
		libs.addAll(getAndDownloadLibs("net.sf.jopt-simple:jopt-simple:5.0.2", true));

		List<String> extracted = new ArrayList<>();
		for (File lib : libs) {
			if (!extracted.contains(lib.getAbsolutePath()) && !lib.getName().contains("lwjgl") && !lib.getName().contains("fabric-base")) {
				extracted.add(lib.getAbsolutePath());
				ZipUtil.unpack(lib, mcExtractedDir, name -> {
					if (name.startsWith("META-INF")) {
						return null;
					} else {
						return name;
					}
				});
			}
		}
		progress.updateProgress("Packing library's", 90);
		mcJar.delete();
		ZipUtil.pack(mcExtractedDir, mcJar);

		FileUtils.deleteQuietly(tempDir);

		progress.updateProgress("Done " + version, 100);
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

	public static class ProgressListener implements Deobfuscator.ProgressListener {
		@Override
		public void init(int i, String s) {

		}

		@Override
		public void onProgress(int i, String s) {

		}
	}

	public static void writeJar(File out, Deobfuscator.ProgressListener progress, Deobfuscator deobfuscator) {
		TranslatingTypeLoader loader = new TranslatingTypeLoader(deobfuscator.getJar(), deobfuscator.getJarIndex(), deobfuscator.getTranslator(TranslationDirection.Obfuscating), deobfuscator.getTranslator(TranslationDirection.Deobfuscating));
		deobfuscator.transformJar(out, progress, new CustomClassTransformer(loader));
	}

	private static class CustomClassTransformer implements Deobfuscator.ClassTransformer {

		TranslatingTypeLoader loader;

		public CustomClassTransformer(TranslatingTypeLoader loader) {
			this.loader = loader;
		}

		@Override
		public CtClass transform(CtClass ctClass) throws Exception {
			return publify(loader.transformClass(ctClass));
		}
	}

	//Taken from enigma, anc changed a little
	public static CtClass publify(CtClass c) {

		for (CtField field : c.getDeclaredFields()) {
			field.setModifiers(publify(field.getModifiers()));
		}
		for (CtBehavior behavior : c.getDeclaredBehaviors()) {
			behavior.setModifiers(publify(behavior.getModifiers()));
		}
		InnerClassesAttribute attr = (InnerClassesAttribute) c.getClassFile().getAttribute(InnerClassesAttribute.tag);
		if (attr != null) {
			for (int i = 0; i < attr.tableLength(); i++) {
				attr.setAccessFlags(i, publify(attr.accessFlags(i)));
			}
		}

		return c;
	}

	private static int publify(int flags) {
		if (!AccessFlag.isPublic(flags)) {
			flags = AccessFlag.setPublic(flags);
		}
		return flags;
	}
}
