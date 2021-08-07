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

import org.junit.Assert;
import org.junit.Test;

import net.fabricmc.installer.util.ArgumentParser;

public class ArgumentTests {
	@Test
	public void test() {
		String[] args = new String[]{"command", "-arg1", "Hello", "-arg2", "-anotherArg", "123"};
		ArgumentParser handler = ArgumentParser.create(args);

		Assert.assertTrue(handler.has("arg1"));
		Assert.assertEquals(handler.get("arg1"), "Hello");

		Assert.assertEquals(handler.getOrDefault("arg3", () -> "World"), "World");

		Assert.assertTrue(handler.has("arg2"));
		Assert.assertFalse(handler.has("arg3"));

		Assert.assertEquals(handler.getCommand().get(), "command");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadArgs() {
		ArgumentParser.create(new String[]{"-arg1", "Hello", "-arg1"});
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnknownArg() {
		ArgumentParser.create(new String[]{"-arg1", "Hello"}).get("arg2");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullArg() {
		ArgumentParser.create(new String[]{"-arg1"}).get("arg1");
	}

	@Test
	public void testCommands() {
		Assert.assertTrue(ArgumentParser.create(new String[]{"command", "-arg1", "Hello"}).getCommand().isPresent());
		Assert.assertEquals(ArgumentParser.create(new String[]{"command", "-arg1", "Hello"}).getCommand().get(), "command");

		Assert.assertFalse(ArgumentParser.create(new String[]{"-arg1", "Hello"}).getCommand().isPresent());
	}
}
