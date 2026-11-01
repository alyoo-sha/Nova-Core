package org.mod.nova_core.ui.state;

import java.util.function.Supplier;

public final class Signals {
    private Signals() {}

    // Все изменения внутри считаются одним: эффекты выполнятся один раз после action
    // All changes inside count as one: effects run once after the action
    public static void batch(Runnable action) {
        Reactive.beginBatch();
        try {
            action.run();
        } finally {
            Reactive.endBatch();
        }
    }

    // Чтения внутри не создают зависимостей / Reads inside create no dependencies
    public static void untracked(Runnable action) {
        DependencyTracker previous = Reactive.enter(null);
        try {
            action.run();
        } finally {
            Reactive.exit(previous);
        }
    }

    public static <T> T untrackedGet(Supplier<? extends T> supplier) {
        DependencyTracker previous = Reactive.enter(null);
        try {
            return supplier.get();
        } finally {
            Reactive.exit(previous);
        }
    }
}