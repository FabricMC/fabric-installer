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

package net.fabricmc.installer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipError;
import java.util.zip.ZipFile;

import mjson.Json;

import net.fabricmc.installer.util.Utils;

public final class LoaderVersion {
	public final String name;
	public final Path path;

	public LoaderVersion(String name) {
		this.name = name;
		this.path = null;
	}

	public LoaderVersion(Path path) throws IOException {
		try (ZipFile zf = new ZipFile(path.toFile())) {
			ZipEntry entry = zf.getEntry("fabric.mod.json");
			if (entry == null) throw new FileNotFoundException("fabric.mod.json");

			String modJsonContent;

			try (InputStream is = zf.getInputStream(entry)) {
				modJsonContent = Utils.readString(is);
			}

			this.name = Json.read(modJsonContent).at("version").asString();
		} catch (ZipError e) {
			throw new IOException(e);
		}

		this.path = path;
	}
}
