package com.moyettes.legacyvoicechat.api.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Event<T> {

    private final List<T> listeners;
    private final Function<List<T>, T> invokerFactory;
    private T invoker;

    private Event(Function<List<T>, T> invokerFactory) {
        this.listeners = new ArrayList<>();
        this.invokerFactory = invokerFactory;
    }

    public static <T> Event<T> of(Function<List<T>, T> invokerFactory) {
        return new Event<>(invokerFactory);
    }

    public static <T> Event<Consumer<T>> consumer() {
        return of(listeners -> t -> listeners.forEach(listener -> listener.accept(t)));
    }

    public static <T, U> Event<BiConsumer<T, U>> biConsumer() {
        return of(listeners -> (t, u) -> listeners.forEach(listener -> listener.accept(t, u)));
    }

    public void register(T listener) {
        listeners.add(listener);

        if (invoker != null) {
            invoker = null;
        }
    }

    public T invoker() {
        if (invoker == null) {
            invoker = invokerFactory.apply(listeners);
        }

        return invoker;
    }

}
