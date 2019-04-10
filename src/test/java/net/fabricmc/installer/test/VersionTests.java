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

package net.fabricmc.installer.test;

import net.fabricmc.installer.util.Version;
import org.junit.Assert;
import org.junit.Test;

public class VersionTests {

	@Test
	public void test(){
		Version version = new Version("18w49a.1");
		Assert.assertEquals(version.getMappingsVersion(), "1");
		Assert.assertEquals(version.getMinecraftVersion(), "18w49a");

		version = new Version("1.14 Pre-Release 1+build.1");
		Assert.assertEquals(version.getMappingsVersion(), "1");
		Assert.assertEquals(version.getMinecraftVersion(), "1.14 Pre-Release 1");

		version = new Version("1.14 Pre-Release 1+build.123");
		Assert.assertEquals(version.getMappingsVersion(), "123");
		Assert.assertEquals(version.getMinecraftVersion(), "1.14 Pre-Release 1");

		version = new Version("3D Shareware v1.34.1");
		Assert.assertEquals(version.getMappingsVersion(), "1");
		Assert.assertEquals(version.getMinecraftVersion(), "3D Shareware v1.34");

	}

}
