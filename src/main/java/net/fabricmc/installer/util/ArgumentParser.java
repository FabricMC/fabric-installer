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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ArgumentParser {
	private final String[] args;
	private Map<String, String> argMap;
	//The command will be the first argument passed, and if it doesnt start with -
	private String command = null;

	private ArgumentParser(String[] args) {
		this.args = args;
		parse();
	}

	public String get(String argument) {
		return argMap.get(argument);
	}

	public String getOrDefault(String argument, Supplier<String> stringSuppler) {
		String ret = argMap.get(argument);

		if (ret == null) {
			ret = stringSuppler.get();
		}

		return ret;
	}

	public boolean has(String argument) {
		return argMap.containsKey(argument);
	}

	public Optional<String> getCommand() {
		return command == null ? Optional.empty() : Optional.of(command);
	}

	private void parse() {
		argMap = new HashMap<>();

		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-")) {
				String key = args[i].substring(1);
				String value = null;

				if (i + 1 < args.length) {
					value = args[i + 1];

					if (value.startsWith("-")) {
						argMap.put(key, "");
						continue;
					}

					i++;
				}

				if (argMap.containsKey(key)) {
					throw new IllegalArgumentException(String.format("Argument %s already passed", key));
				}

				argMap.put(key, value);
			} else if (i == 0) {
				command = args[i];
			}
		}
	}

	public static ArgumentParser create(String[] args) {
		return new ArgumentParser(args);
	}
}
