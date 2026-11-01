package org.mod.nova_core.ui.state;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.UnaryOperator;

// Изменяемое значение; set() уведомляет зависимые Computed и Effect / A mutable value; set() notifies dependent Computed and Effect
public final class Signal<T> extends AbstractSignal<T> {
    private T value;
    // Равные значения не считаются изменением / Equal values do not count as a change
    private final BiPredicate<? super T, ? super T> equality;

    public Signal(T initial) {
        this(initial, Objects::equals);
    }

    // Свой критерий равенства; (a, b) -> false уведомляет при каждом set (удобно для изменяемых объектов)
    // Custom equality; (a, b) -> false notifies on every set (handy for mutable objects)
    public Signal(T initial, BiPredicate<? super T, ? super T> equality) {
        this.value = initial;
        this.equality = equality;
    }

    public static <T> Signal<T> of(T initial) {
        return new Signal<>(initial);
    }

    public void set(T next) {
        if (equality.test(value, next)) return;
        value = next;
        version++;
        Reactive.beginBatch();
        try {
            notifyDependents();
        } finally {
            Reactive.endBatch();
        }
    }

    public void update(UnaryOperator<T> change) {
        set(change.apply(value));
    }

    @Override
    void refresh() {}

    @Override
    T rawValue() {
        return value;
    }
}