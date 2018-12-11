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

package net.fabricmc.installer.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

public class Translator {

	public static final Translator INSTANCE = new Translator();

	private HashMap<String, String> lang = new HashMap<>();
	private HashMap<String, String> defaultLang = new HashMap<>();

	public void load(Locale locale) throws IOException {
		load(locale, lang);
		load(new Locale("en", "US"), defaultLang);
	}

	public void load(Locale locale, HashMap<String, String> map) throws IOException {
		if (!isValid(locale)) {
			return;
		}
		String string = IOUtils.toString(ClassLoader.getSystemClassLoader().getResource(locale.getLanguage() + "_" + locale.getCountry() + ".lang"), StandardCharsets.UTF_8);
		String[] lines = string.split(System.getProperty("line.separator"));
		for (String line : lines) {
			if (!line.startsWith("#") && line.contains("=")) {
				String[] split = line.split("=");
				map.put(split[0], split[1]);
			}
		}

	}

	public boolean isValid(Locale locale) {
		return ClassLoader.getSystemClassLoader().getResource(locale.getLanguage() + "_" + locale.getCountry() + ".lang") != null;
	}

	public String getString(String key) {
		if (lang.containsKey(key)) {
			return lang.get(key);
		}
		if (defaultLang.containsKey(key)) {
			return defaultLang.get(key);
		}
		return key;
	}

}
