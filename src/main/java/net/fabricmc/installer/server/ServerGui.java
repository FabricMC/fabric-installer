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

import net.fabricmc.installer.BaseGui;
import net.fabricmc.installer.InstallerGui;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class ServerGui extends BaseGui {

	@Override
	public void install() {
		String loaderVersion = (String) loaderVersionComboBox.getSelectedItem();
		new Thread(() -> {
			try {
				ServerInstaller.install(new File(installLocation.getText()), loaderVersion, this);
			} catch (IOException e) {
				e.printStackTrace();
				error(e.getLocalizedMessage());
			}
		}).start();
	}

	@Override
	public void setupPane1(JPanel pane, InstallerGui installerGui) {

	}

	@Override
	public void setupPane2(JPanel pane, InstallerGui installerGui) {
		installLocation.setText(new File("").getAbsolutePath());
	}

}
