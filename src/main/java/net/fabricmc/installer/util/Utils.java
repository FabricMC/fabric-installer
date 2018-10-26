package net.fabricmc.installer.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Utils {
	public static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
	private static final Gson GSON = new Gson();

	public static JsonObject loadRemoteJSON(final URL source) throws IOException {
		return GSON.fromJson(new InputStreamReader(source.openStream()), JsonObject.class);
	}
}
