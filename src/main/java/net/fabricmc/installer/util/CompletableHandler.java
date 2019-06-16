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

	protected void complete(T value){
		complete = true;
		completeConsumers.forEach(listConsumer -> listConsumer.accept(value));
	}

	public boolean isComplete() {
		return complete;
	}

}
