package org.mod.nova_core.ui.state;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

// Значение, вычисляемое из других сигналов. Первое значение считается сразу, дальше пересчёт при чтении,
// и только если зависимость действительно изменилась. Внутри функции нельзя менять сигналы
// A value derived from other signals. The first value is computed immediately, later it recomputes on read,
// and only if a dependency really changed. Do not write to signals inside the function
public final class Computed<T> extends AbstractSignal<T> implements Subscription {
    private final Supplier<? extends T> compute;
    private final BiPredicate<? super T, ? super T> equality;
    private final DependencyTracker tracker = new DependencyTracker(this::onSourceChanged);

    private T value;
    private boolean stale = false;
    private boolean computing = false;
    private boolean disposed = false;

    public Computed(Supplier<? extends T> compute) {
        this(compute, Objects::equals);
    }

    public Computed(Supplier<? extends T> compute, BiPredicate<? super T, ? super T> equality) {
        this.compute = Objects.requireNonNull(compute);
        this.equality = equality;
        this.value = evaluate();
    }

    private T evaluate() {
        computing = true;
        DependencyTracker previous = tracker.begin();
        try {
            return compute.get();
        } finally {
            tracker.end(previous);
            computing = false;
        }
    }

    private void onSourceChanged() {
        if (stale) return;
        stale = true;
        notifyDependents();
    }

    @Override
    void refresh() {
        if (computing) throw new IllegalStateException("Cyclic dependency in Computed");
        if (!stale || disposed) return;
        if (tracker.dependenciesChanged()) {
            T next = evaluate();
            if (!equality.test(value, next)) {
                value = next;
                version++;
            }
        }
        stale = false;
    }

    @Override
    T rawValue() {
        return value;
    }

    // Отписывается от источников; значение остаётся последним вычисленным / Unsubscribes from sources; the value stays the last computed one
    @Override
    public void cancel() {
        if (disposed) return;
        disposed = true;
        tracker.clear();
    }
}