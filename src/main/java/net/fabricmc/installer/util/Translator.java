package net.fabricmc.installer.util;

import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

public class Translator {

    private static HashMap<String, String> lang = new HashMap<>();

    public static void load(Locale locale) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (!isValid(locale)) {
            return;
        }
        String string = Resources.toString(classLoader.getResource(locale.getLanguage() + "_" + locale.getCountry() + ".lang"), StandardCharsets.UTF_8);
        String[] lines = string.split(System.getProperty("line.separator"));
        for (String line : lines) {
            if (!line.startsWith("#") && line.contains("=")) {
                String[] split = line.split("=");
                lang.put(split[0], split[1]);
            }
        }

    }

    public static boolean isValid(Locale locale) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader.getResource(locale.getLanguage() + "_" + locale.getCountry() + ".lang") != null;
    }

    public static String getString(String key) {
        if (lang.containsKey(key)) {
            return lang.get(key);
        }
        return key;
    }

}