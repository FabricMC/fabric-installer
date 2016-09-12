package net.fabricmc.installer.installer;

import com.google.gson.*;
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
import net.fabricmc.installer.util.IInstallerProgress;
import net.fabricmc.installer.util.Translator;
import org.apache.commons.io.FileUtils;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class ClientInstaller {

    public static void install(File mcDir, String version, IInstallerProgress progress) throws IOException, MappingParseException {
        progress.updateProgress(Translator.getString("gui.installing") + ": " + version, 0);
        String[] split = version.split("-");
        if (isValidInstallLocation(mcDir, split[0]).isPresent()) {
            throw new RuntimeException(isValidInstallLocation(mcDir, split[0]).get());
        }
        File fabricData = new File(mcDir, "fabricData");
        File fabricJar = new File(fabricData, version + ".jar");
        if (!fabricJar.exists()) {
            progress.updateProgress(Translator.getString("install.client.downloadFabric"), 10);
            FileUtils.copyURLToFile(new URL("http://maven.fabricmc.net/net/fabricmc/fabric-base/" + version + "/fabric-base-" + version + ".jar"), fabricJar);
        }

        JarFile jarFile = new JarFile(fabricJar);
        Attributes attributes = jarFile.getManifest().getMainAttributes();

        String id = "fabric-" + attributes.getValue("FabricVersion");
        Optional<String> pomfVersion = Optional.empty();
        String ver = attributes.getValue("PomfVersion");
        if (ver != null && !ver.isEmpty()) {
            pomfVersion = Optional.of(ver);
        }
        System.out.println(Translator.getString("gui.installing") + " " + id);
        File versionsFolder = new File(mcDir, "versions");
        File fabricVersionFolder = new File(versionsFolder, id);
        File mcVersionFolder = new File(versionsFolder, split[0]);
        File fabricJsonFile = new File(fabricVersionFolder, id + ".json");
        File fabricJarFile = new File(fabricVersionFolder, id + ".jar");
        File mcJsonFile = new File(mcVersionFolder, split[0] + ".json");
        File mcJarFile = new File(mcVersionFolder, split[0] + ".jar");
        if (fabricVersionFolder.exists()) {
            progress.updateProgress(Translator.getString("install.client.removeOld"), 10);
            FileUtils.deleteDirectory(fabricVersionFolder);
        }
        fabricVersionFolder.mkdirs();

        progress.updateProgress(Translator.getString("install.client.createJson"), 20);

        String mcJson = FileUtils.readFileToString(mcJarFile, Charset.defaultCharset());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement jsonElement = gson.fromJson(new FileReader(mcJsonFile), JsonElement.class);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        jsonObject.remove("id");
        jsonObject.addProperty("id", id);

        jsonObject.remove("mainClass");
        jsonObject.addProperty("mainClass", "net.minecraft.launchwrapper.Launch");

        jsonObject.remove("downloads");

        String args = jsonObject.get("minecraftArguments").getAsString();
        jsonObject.remove("minecraftArguments");
        jsonObject.addProperty("minecraftArguments", args.replace("${version_name}", split[0]) + " --tweakClass net.fabricmc.base.launch.FabricClientTweaker");

        JsonArray librarys = jsonObject.getAsJsonArray("libraries");

        addDep("net.fabricmc:fabric-base:" + attributes.getValue("FabricVersion"), "http://maven.fabricmc.net/", librarys);

        File tempWorkDir = new File(fabricVersionFolder, "temp");
        File depJson = new File(tempWorkDir, "dependencies.json");
        ZipUtil.unpack(fabricJar, mcVersionFolder, name -> {
            if (name.startsWith("dependencies.json")) {
                return name;
            } else {
                return null;
            }
        });
        JsonElement depElement = gson.fromJson(new FileReader(depJson), JsonElement.class);
        JsonObject depObject = depElement.getAsJsonObject();
        librarys.addAll(depObject.getAsJsonArray("libraries"));

        FileUtils.write(fabricJsonFile, gson.toJson(jsonElement), "UTF-8");

        progress.updateProgress(Translator.getString("install.client.createTempDir"), 40);


        if (tempWorkDir.exists()) {
            FileUtils.deleteDirectory(tempWorkDir);
        }
        progress.updateProgress(Translator.getString("install.client.extractMappings"), 50);

        File mappingsDir = new File(tempWorkDir, "pomf-" + split[0] + File.separator + "mappings");
        if (!pomfVersion.isPresent()) {
            //Resort to old mappings type
            ZipUtil.unpack(fabricJar, tempWorkDir, name -> {
                if (name.startsWith("pomf-" + split[0])) {
                    return name;
                } else {
                    return null;
                }
            });
        } else {
            mappingsDir = new File(tempWorkDir, "pomf-" + split[0] + File.separator + "pomf-enigma-" + pomfVersion);
            File pomfZip = new File(tempWorkDir, "pomf-enigma-" + pomfVersion + ".zip");
            FileUtils.copyURLToFile(new URL("http://asie.pl:8080/job/pomf/" + pomfVersion.get() + "/artifact/build/libs/pomf-enigma-" + split[0] + "." + pomfVersion.get() + ".zip"), pomfZip);
            ZipUtil.unpack(pomfZip, mappingsDir);
        }

        File tempAssests = new File(tempWorkDir, "assets");
        progress.updateProgress(Translator.getString("install.client.loadJar"), 60);
        Deobfuscator deobfuscator = new Deobfuscator(new JarFile(mcJarFile));
        progress.updateProgress(Translator.getString("install.client.readMappings"), 65);
        deobfuscator.setMappings(new MappingsEnigmaReader().read(mappingsDir));
        progress.updateProgress(Translator.getString("install.client.exportMappedJar"), 70);
        writeJar(fabricJarFile, new ProgressListener(), deobfuscator);
        progress.updateProgress(Translator.getString("install.client.cleanJar"), 80);
        ZipUtil.unpack(mcJarFile, tempAssests, name -> {
            if (name.startsWith("assets") || name.startsWith("log4j2.xml") || name.startsWith("pack.png")) {
                return name;
            } else {
                return null;
            }
        });
        ZipUtil.unpack(fabricJarFile, tempAssests);
        ZipUtil.pack(tempAssests, fabricJarFile);

        progress.updateProgress(Translator.getString("install.client.cleanDir"), 90);
        FileUtils.deleteDirectory(tempWorkDir);
        FileUtils.deleteDirectory(fabricData);
        if (!mcJsonFile.exists()) { //I noticed the json was being deleted, this adds it back if it doesnt exist
            FileUtils.write(mcJsonFile, mcJson, Charset.defaultCharset());
        }
        progress.updateProgress(Translator.getString("install.success"), 100);
    }

    public static void addDep(String dep, String maven, JsonArray jsonArray) {
        JsonObject object = new JsonObject();
        object.addProperty("name", dep);
        if (!maven.isEmpty()) {
            object.addProperty("url", maven);
        }
        jsonArray.add(object);
    }

    public static Optional<String> isValidInstallLocation(File mcDir, String mcVer) {
        if (!mcDir.isDirectory()) {
            return Optional.of(mcDir.getName() + " " + Translator.getString("install.client.error.noDir"));
        }
        File versionsFolder = new File(mcDir, "versions");
        if (!versionsFolder.exists() || !versionsFolder.isDirectory()) {
            return Optional.of(Translator.getString("install.client.error.noMc") + mcVer);
        }
        File versionFolder = new File(versionsFolder, mcVer);
        if (!versionsFolder.exists() || !versionsFolder.isDirectory()) {
            return Optional.of(Translator.getString("install.client.error.noMc") + mcVer);
        }

        File mcJsonFile = new File(versionFolder, mcVer + ".json");
        File mcJarFile = new File(versionFolder, mcVer + ".jar");
        if (!mcJsonFile.exists() || !mcJarFile.exists()) {
            return Optional.of(Translator.getString("install.client.error.noMc") + mcVer);
        }

        //All is ok
        return Optional.empty();
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
