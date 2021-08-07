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

package net.fabricmc.installer.server;

import net.fabricmc.installer.util.LauncherMeta;
import net.fabricmc.installer.util.Utils;
import net.fabricmc.installer.util.VersionMeta;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class MinecraftServerDownloader {
    private final String gameVersion;

    public MinecraftServerDownloader(String gameVersion) {
        this.gameVersion = gameVersion;
    }

    public void downloadMinecraftServer(Path serverJar) throws IOException {
        Path serverJarTmp = serverJar.resolveSibling(serverJar.getFileName().toString() + ".tmp");
        Files.deleteIfExists(serverJar);
        Utils.downloadFile(new URL(getServerDownload().url), serverJarTmp);

        Files.move(serverJarTmp, serverJar, StandardCopyOption.REPLACE_EXISTING);
    }

    private VersionMeta getVersionMeta() throws IOException {
        LauncherMeta.Version version = LauncherMeta.getLauncherMeta().getVersion(gameVersion);

        if (version == null) {
            throw new RuntimeException("Failed to find version info for minecraft " + gameVersion);
        }

        return version.getVersionMeta();
    }

    private VersionMeta.Download getServerDownload() throws IOException {
        return getVersionMeta().downloads.get("server");
    }
}
