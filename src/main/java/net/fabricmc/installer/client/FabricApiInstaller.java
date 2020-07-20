package net.fabricmc.installer.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.installer.util.Utils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class FabricApiInstaller {

    private static final String RELEASES_URL = "https://api.github.com/repos/FabricMC/fabric/releases";
    private static final boolean PRE_RELEASES = false;

    public static void install(File mcDir, String mcVersion) {
        try {
            String releasesJsonData = Utils.readTextFile(new URL(RELEASES_URL));
            JsonArray releases = JsonParser.parseString(releasesJsonData).getAsJsonArray();

            for (int i = 0; i < releases.size(); i++) {
                JsonObject releaseJson = releases.get(i).getAsJsonObject();

                if (releaseJson.get("draft").getAsBoolean()) {
                    continue;
                }
                if (releaseJson.get("prerelease").getAsBoolean() && !PRE_RELEASES) {
                    continue;
                }
                if (!releaseJson.get("target_commitish").getAsString().equalsIgnoreCase(mcVersion))
                    continue;

                String name = releaseJson.get("name").getAsString();
                System.out.println("Installing fabric api: " + name);

                JsonArray assetsJson = releaseJson.get("assets").getAsJsonArray();

                for (int j = 0; j < assetsJson.size(); j++) {
                    JsonObject assetJson = assetsJson.get(j).getAsJsonObject();

                    if (!assetJson.get("content_type").getAsString().equalsIgnoreCase("application/java-archive")) {
                        continue;
                    }

                    String downloadUrl = assetJson.get("browser_download_url").getAsString();

                    File modsDir = new File(mcDir, "mods");
                    if (!modsDir.exists()) {
                        modsDir.mkdir();
                    }

                    Utils.downloadFile(new URL(downloadUrl), new File(modsDir, assetJson.get("name").getAsString()));
                    break;
                }
                break;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
