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
