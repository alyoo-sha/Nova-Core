package org.mod.nova_core.ui.state;

import java.util.ArrayList;
import java.util.function.Consumer;

// Общая часть Signal и Computed; наследовать вне пакета нельзя / Shared part of Signal and Computed; cannot be subclassed outside the package
public abstract class AbstractSignal<T> implements ReadableSignal<T> {
    // Кто зависит от этого сигнала, создаётся сразу / Who depends on this signal, created eagerly
    private final ArrayList<DependencyTracker> dependents = new ArrayList<>();

    // Растёт при каждом реальном изменении значения / Grows on every real value change
    int version = 0;

    AbstractSignal() {}

    // Привести значение в актуальное состояние / Bring the value up to date
    abstract void refresh();

    abstract T rawValue();

    @Override
    public final T get() {
        refresh();
        Reactive.track(this);
        return rawValue();
    }

    @Override
    public final T peek() {
        refresh();
        return rawValue();
    }

    @Override
    public final Subscription subscribe(Consumer<? super T> listener) {
        boolean[] first = {true};
        return Effect.create(() -> {
            T value = get();
            if (first[0]) { // первый прогон только устанавливает зависимость / the first run only sets up the dependency
                first[0] = false;
                return;
            }
            Signals.untracked(() -> listener.accept(value));
        });
    }

    @Override
    public final Subscription observe(Consumer<? super T> listener) {
        return Effect.create(() -> {
            T value = get();
            Signals.untracked(() -> listener.accept(value));
        });
    }

    final void addDependent(DependencyTracker tracker) {
        dependents.add(tracker);
    }

    final void removeDependent(DependencyTracker tracker) {
        dependents.remove(tracker);
    }

    final void notifyDependents() {
        for (int i = 0; i < dependents.size(); i++) dependents.get(i).markStale();
    }
}