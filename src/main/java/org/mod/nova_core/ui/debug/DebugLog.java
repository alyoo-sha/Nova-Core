package org.mod.nova_core.ui.debug;

import java.util.Objects;
import java.util.function.Consumer;

// Куда инспектор пишет дамп дерева. По умолчанию в стандартный вывод / Where the inspector writes the tree dump. Standard output by default
public final class DebugLog {
    private static volatile Consumer<String> sink = System.out::println;

    private DebugLog() {}

    public static void setSink(Consumer<String> newSink) {
        sink = Objects.requireNonNull(newSink);
    }

    public static void log(String message) {
        sink.accept(message);
    }
}