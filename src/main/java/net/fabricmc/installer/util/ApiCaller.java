package net.fabricmc.installer.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ApiCaller {

	/**
	 * Fetches a mod from Modrinth by its slug, filtered by a specific game version
	 * and loader, then downloads the most recent matching file (checking versions in reverse).
	 *
	 * @param slug              The Modrinth project slug (e.g., "itemlore" or a full URL to strip).
	 * @param targetGameVersion The game version you want (e.g. "1.21.3").
	 * @param targetLoader      The loader you want (e.g. "fabric").
	 * @param destination       The folder path where the file will be downloaded (e.g., "./downloads").
	 */
	public static void apiGrabMod(String slug, String targetGameVersion, String targetLoader, String destination) {
		OkHttpClient client = new OkHttpClient();

		// 1) Strip "https://modrinth.com/mod/" if present
		String prefix = "https://modrinth.com/mod/";
		if (slug.startsWith(prefix)) {
			slug = slug.substring(prefix.length());
			System.out.println("[DEBUG] Stripped slug: " + slug);
		}

		// 2) Fetch the project details
		String projectUrl = "https://api.modrinth.com/v2/project/" + slug;
		System.out.println("[DEBUG] Fetching project info from: " + projectUrl);

		Request projectRequest = new Request.Builder()
				.url(projectUrl)
				.header("Accept", "application/json")
				.build();

		try (Response projectResponse = client.newCall(projectRequest).execute()) {
			if (!projectResponse.isSuccessful()) {
				System.out.println("[ERROR] Project request failed. HTTP code: " + projectResponse.code());
				return;
			}

			String projectJson = projectResponse.body().string();
			System.out.println("[DEBUG] Project JSON: " + projectJson);

			// Parse the project JSON
			JsonElement projectRoot = JsonParser.parseString(projectJson);
			if (!projectRoot.isJsonObject()) {
				System.out.println("[ERROR] Project JSON is not an object.");
				return;
			}
			JsonObject projectObj = projectRoot.getAsJsonObject();

			// 3) Get the version IDs array
			if (!projectObj.has("versions") || !projectObj.get("versions").isJsonArray()) {
				System.out.println("[ERROR] 'versions' field is missing or not an array.");
				return;
			}
			JsonArray versionIds = projectObj.get("versions").getAsJsonArray();
			if (versionIds.size() == 0) {
				System.out.println("[ERROR] No version IDs found in the project.");
				return;
			}
			System.out.println("[DEBUG] Found " + versionIds.size() + " version IDs.");

			// Ensure the destination directory exists
			File dir = new File(destination);
			if (!dir.exists()) {
				if (dir.mkdirs()) {
					System.out.println("[DEBUG] Created directory: " + dir.getAbsolutePath());
				}
			}

			// 4) Iterate version IDs in reverse order
			//    (assuming the last entry in "versions" is the newest)
			boolean foundMatch = false;
			for (int i = versionIds.size() - 1; i >= 0; i--) {
				String versionId = versionIds.get(i).getAsString();
				System.out.println("[DEBUG] Checking version ID (reverse order): " + versionId);

				// 4a) Fetch the version details
				String versionUrl = "https://api.modrinth.com/v2/version/" + versionId;
				System.out.println("[DEBUG] Fetching version info from: " + versionUrl);

				Request versionRequest = new Request.Builder()
						.url(versionUrl)
						.header("Accept", "application/json")
						.build();

				try (Response versionResponse = client.newCall(versionRequest).execute()) {
					if (!versionResponse.isSuccessful()) {
						System.out.println("[WARN] Failed to fetch version " + versionId + ". HTTP: "
								+ versionResponse.code());
						continue; // skip to next
					}

					String versionJson = versionResponse.body().string();
					System.out.println("[DEBUG] Version JSON for " + versionId + ": " + versionJson);

					JsonElement versionRoot = JsonParser.parseString(versionJson);
					if (!versionRoot.isJsonObject()) {
						System.out.println("[WARN] Version JSON is not an object for ID " + versionId);
						continue;
					}
					JsonObject versionObj = versionRoot.getAsJsonObject();

					// 4b) Check if "game_versions" contains our targetGameVersion
					if (!versionObj.has("game_versions") || !versionObj.get("game_versions").isJsonArray()) {
						System.out.println("[WARN] 'game_versions' missing or not array for " + versionId);
						continue;
					}
					JsonArray gameVersions = versionObj.get("game_versions").getAsJsonArray();
					System.out.println("[DEBUG] game_versions for " + versionId + ": " + gameVersions);

					boolean matchesVersion = false;
					for (JsonElement gvElement : gameVersions) {
						if (gvElement.getAsString().equalsIgnoreCase(targetGameVersion)) {
							matchesVersion = true;
							break;
						}
					}
					if (!matchesVersion) {
						System.out.println("[DEBUG] Version ID " + versionId + " does not match game version: "
								+ targetGameVersion);
						continue; // check the next version
					}

					// 4c) Check if "loaders" contains our targetLoader
					if (!versionObj.has("loaders") || !versionObj.get("loaders").isJsonArray()) {
						System.out.println("[WARN] 'loaders' missing or not array for " + versionId);
						continue;
					}
					JsonArray loadersArray = versionObj.get("loaders").getAsJsonArray();
					System.out.println("[DEBUG] loaders for " + versionId + ": " + loadersArray);

					boolean matchesLoader = false;
					for (JsonElement loaderElem : loadersArray) {
						if (loaderElem.getAsString().equalsIgnoreCase(targetLoader)) {
							matchesLoader = true;
							break;
						}
					}
					if (!matchesLoader) {
						System.out.println("[DEBUG] Version ID " + versionId + " does not match loader: "
								+ targetLoader);
						continue; // check the next version
					}

					// If we reach here, we found a version that supports BOTH the targetGameVersion and loader
					System.out.println("[INFO] Found matching version ID: " + versionId);

					// 4d) Now find the primary file or first file
					if (!versionObj.has("files") || !versionObj.get("files").isJsonArray()) {
						System.out.println("[WARN] No 'files' array found for version ID " + versionId);
						continue;
					}
					JsonArray filesArray = versionObj.get("files").getAsJsonArray();
					if (filesArray.size() == 0) {
						System.out.println("[WARN] Empty files array for version ID " + versionId);
						continue;
					}

					JsonObject primaryFile = null;
					for (JsonElement fileElem : filesArray) {
						JsonObject fileObj = fileElem.getAsJsonObject();
						if (fileObj.has("primary") && fileObj.get("primary").getAsBoolean()) {
							primaryFile = fileObj;
							break;
						}
					}
					if (primaryFile == null) {
						// fallback: use the first file
						primaryFile = filesArray.get(0).getAsJsonObject();
					}

					String downloadUrl = primaryFile.get("url").getAsString();
					String filename = primaryFile.get("filename").getAsString();
					System.out.println("[INFO] Download URL: " + downloadUrl);
					System.out.println("[INFO] Filename: " + filename);

					// 4e) Download the file
					Request fileRequest = new Request.Builder().url(downloadUrl).build();
					try (Response fileResp = client.newCall(fileRequest).execute()) {
						if (!fileResp.isSuccessful()) {
							System.out.println("[ERROR] Failed to download file. HTTP code: " + fileResp.code());
							continue;
						}

						// Write file to disk
						File outFile = new File(dir, filename);
						System.out.println("[DEBUG] Saving to: " + outFile.getAbsolutePath());
						try (InputStream in = fileResp.body().byteStream();
							 FileOutputStream fos = new FileOutputStream(outFile)) {
							byte[] buffer = new byte[8192];
							int bytesRead;
							while ((bytesRead = in.read(buffer)) != -1) {
								fos.write(buffer, 0, bytesRead);
							}
						}

						System.out.println("[INFO] Successfully downloaded " + filename);
					} catch (IOException e) {
						System.out.println("[ERROR] Exception while downloading file: " + e.getMessage());
						e.printStackTrace();
					}

					// We found and downloaded the matching version, so we can stop
					foundMatch = true;
					break;
				} catch (IOException e) {
					System.out.println("[ERROR] Exception while fetching version " + versionId + ": " + e.getMessage());
					e.printStackTrace();
				}
			}

			if (!foundMatch) {
				System.out.println("[INFO] No version found matching game version '" + targetGameVersion
						+ "' and loader '" + targetLoader + "'.");
			}

		} catch (IOException e) {
			System.out.println("[ERROR] Exception while fetching project: " + e.getMessage());
			e.printStackTrace();
		}
	}

	// Optional main method for quick testing
	public static void main(String[] args) {
		// Example usage: "itemlore" for game version "1.21.3" and "fabric"
		apiGrabMod("itemlore", "1.21.3", "fabric", "./downloads");
		apiGrabMod("https://modrinth.com/mod/xaeros-minimap", "1.21.3", "fabric", "./downloads");
		apiGrabMod("https://modrinth.com/mod/xaeros-world-map", "1.21.3", "fabric", "./downloads");
	}
}
