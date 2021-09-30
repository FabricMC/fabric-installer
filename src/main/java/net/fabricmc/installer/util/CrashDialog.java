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

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

public class CrashDialog {
	public CrashDialog(Throwable throwable) {
		JFrame frame = new JFrame("Fabric installer has crashed!");
		Container pane = frame.getContentPane();
		pane.setLayout(new BorderLayout());

		JTextArea text = new JTextArea();
		text.setTabSize(2);
		pane.add(new JScrollPane(text), BorderLayout.CENTER);

		StringWriter stringWriter = new StringWriter();
		throwable.printStackTrace(new PrintWriter(stringWriter));
		String stackTrace = stringWriter.toString();
		text.setText(stackTrace);

		frame.setSize(600, 400);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.doLayout();
		frame.setVisible(true);
	}
}
