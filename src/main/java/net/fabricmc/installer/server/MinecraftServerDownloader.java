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
        Path serverJarTmp = serverJar.getParent().resolve(serverJar.getFileName().toString() + ".tmp");
        Files.deleteIfExists(serverJar);
        Utils.downloadFile(new URL(getVersionMeta().downloads.get("server").url), serverJarTmp);

        if (!validateServerHash(serverJarTmp)) {
            throw new IOException("Failed to validate server jar hash");
        }

        Files.move(serverJarTmp, serverJar, StandardCopyOption.REPLACE_EXISTING);
    }

    public boolean validateServerHash(Path serverJar) {
        if (!Files.exists(serverJar)) {
            return false;
        }

        // TODO
        return true;
    }

    // TODO support experimental versions
    // TODO cache this
    private VersionMeta getVersionMeta() throws IOException {
        return LauncherMeta.getLauncherMeta().getVersion(gameVersion).getVersionMeta();
    }
}
