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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MavenHandler {

	public String latestVersion = "";
	public List<String> versions = new ArrayList<>();

	public void load(String mavenServerURL, String packageName, String jarName) throws IOException, XMLStreamException {

		URL url = new URL(mavenServerURL + packageName + "/" + jarName + "/maven-metadata.xml");
		XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(url.openStream());

		while (reader.hasNext()) {
			if (reader.next() == XMLStreamConstants.START_ELEMENT && reader.getLocalName().equals("version")) {
				String text = reader.getElementText();
				versions.add(text);
			}
		}

		Collections.reverse(versions);
		latestVersion = versions.get(0);
	}

}
