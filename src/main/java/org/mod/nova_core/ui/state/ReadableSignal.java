package org.mod.nova_core.ui.state;

import java.util.function.Consumer;

// Значение, за изменениями которого можно следить / A value whose changes can be observed
public interface ReadableSignal<T> {
    // Читает значение; внутри Computed или Effect добавляет зависимость / Reads the value; inside Computed or Effect adds a dependency
    T get();

    // Читает значение без зависимости / Reads the value without a dependency
    T peek();

    // Слушатель вызывается при каждом изменении, но не сразу / The listener runs on every change, but not immediately
    Subscription subscribe(Consumer<? super T> listener);

    // Слушатель вызывается сразу и затем при каждом изменении / The listener runs immediately and then on every change
    Subscription observe(Consumer<? super T> listener);
}