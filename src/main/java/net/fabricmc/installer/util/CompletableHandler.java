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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CompletableHandler<T> {
	private boolean complete;

	private List<Consumer<T>> completeConsumers = new ArrayList<>();

	public void onComplete(Consumer<T> completeConsumer) {
		completeConsumers.add(completeConsumer);
	}

	protected void complete(T value) {
		complete = true;
		completeConsumers.forEach(listConsumer -> listConsumer.accept(value));
	}

	public boolean isComplete() {
		return complete;
	}
}
