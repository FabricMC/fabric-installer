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

package net.fabricmc.installer.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    public static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("lang/installer", Locale.getDefault(), new ResourceBundle.Control() {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            final String bundleName = toBundleName(baseName, locale);
            final String resourceName = toResourceName(bundleName, "properties");
            try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                if (stream != null) {
                    try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        return new PropertyResourceBundle(reader);
                    }
                }
            }
            return super.newBundle(baseName, locale, format, loader, reload);
        }
    });

    public static File findDefaultUserDir() {
        String home = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name").toLowerCase();
        File dir;
        File homeDir = new File(home);

        if (os.contains("win") && System.getenv("APPDATA") != null) {
            dir = new File(System.getenv("APPDATA"));
        } else if (os.contains("mac")) {
            dir = new File(homeDir, "Library" + File.separator + "Application Support");
        } else {
            dir = homeDir;
        }
        return dir;
    }

    public static File findDefaultInstallDir() {
        String home = System.getProperty("user.home", ".");
        String os = System.getProperty("os.name").toLowerCase();
        File dir;
        File homeDir = new File(home);

        if (os.contains("win") && System.getenv("APPDATA") != null) {
            dir = new File(System.getenv("APPDATA"), ".minecraft");
        } else if (os.contains("mac")) {
            dir = new File(homeDir, "Library" + File.separator + "Application Support" + File.separator + "minecraft");
        } else {
            dir = new File(homeDir, ".minecraft");
        }
        return dir;
    }

    public static String readTextFile(URL url) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public static void writeToFile(File file, String string) throws FileNotFoundException {
        try (PrintStream printStream = new PrintStream(new FileOutputStream(file))) {
            printStream.print(string);
        }
    }

    public static String readFile(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }

    public static void downloadFile(URL url, File file) throws IOException {
        if (!file.getParentFile().isDirectory()) {
            if (!file.mkdirs()) {
                throw new IOException("Could not create directory for " + file.getAbsolutePath() + "!");
            }
        }

        try (InputStream in = url.openStream()) {
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static MinecraftLaunchJson getLaunchMeta(String loaderVersion) throws IOException {
        String url = String.format("%s/%s/%s/%s/%3$s-%4$s.json", Reference.mavenServerUrl, Reference.PACKAGE, Reference.LOADER_NAME, loaderVersion);
        String fabricInstallMeta = Utils.readTextFile(new URL(url));
        JsonObject installMeta = Utils.GSON.fromJson(fabricInstallMeta, JsonObject.class);
        return new MinecraftLaunchJson(installMeta);
    }

    public static String getProfileIcon() {

        try (InputStream is = Utils.class.getClassLoader().getResourceAsStream("profile_icon.png")) {
            byte[] ret = new byte[4096];
            int offset = 0;
            int len;

            while ((len = is.read(ret, offset, ret.length - offset)) != -1) {
                offset += len;
                if (offset == ret.length) ret = Arrays.copyOf(ret, ret.length * 2);
            }

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(Arrays.copyOf(ret, offset));

        } catch (IOException e) {
            e.printStackTrace();
        }
        return "TNT"; // Fallback to TNT icon if we cant load Fabric icon.
    }

    public static void launcherCheck(boolean showGui) throws IOException {
        List<String> processes = getRunningProcesses();
        if (processes.stream().anyMatch(process -> process.toLowerCase().contains("minecraft"))) {
            System.out.println("The Minecraft launcher is open! Please close it before installing fabric!");
            if (showGui) {
                JOptionPane.showMessageDialog(null,
                        "The Minecraft launcher is open! Please close it before installing fabric!");
            }
            System.exit(1);
        }
    }

    private static List<String> getRunningProcesses() throws IOException {
        Process proc;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            proc = Runtime.getRuntime().exec("tasklist.exe /FO TABLE /NH /FI \"IMAGENAME eq MinecraftLauncher.exe\"");
        } else {
            proc = Runtime.getRuntime().exec("ps -e -o command");
        }
        BufferedReader lineReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        return lineReader.lines().collect(Collectors.toList());
    }
}
