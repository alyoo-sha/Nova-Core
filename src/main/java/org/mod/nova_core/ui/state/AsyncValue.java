package org.mod.nova_core.ui.state;

import org.mod.nova_core.ui.context.UiContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

// Значение, которое загружается в фоне. Результат применяется в основном потоке, поэтому сигналы безопасны.
// Пока идёт повторная загрузка, прежнее значение остаётся доступным. Устаревшие загрузки игнорируются
// A value that loads in the background. The result is applied on the main thread, so the signals are safe.
// During a reload the previous value stays available. Stale loads are ignored
public final class AsyncValue<T> {
    public enum Status { IDLE, LOADING, READY, FAILED }

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "nova-core-async");
        t.setDaemon(true);
        return t;
    });

    private final Signal<Status> status = Signal.of(Status.IDLE);
    private final Signal<T> value = Signal.<T>of(null);
    private final Signal<Throwable> error = Signal.<Throwable>of(null);
    private int generation = 0;

    public ReadableSignal<Status> status() { return status; }

    // Последнее успешное значение или null / The last successful value or null
    public ReadableSignal<T> value() { return value; }

    public ReadableSignal<Throwable> error() { return error; }

    // Запустить загрузку в фоновом потоке; вызывать из основного потока / Start loading on a background thread; call from the main thread
    public void load(Supplier<? extends T> loader) {
        int gen = ++generation;
        Signals.batch(() -> {
            status.set(Status.LOADING);
            error.set(null);
        });
        EXECUTOR.execute(() -> {
            T result = null;
            Throwable failure = null;
            try {
                result = loader.get();
            } catch (Throwable t) {
                failure = t;
            }
            T finalResult = result;
            Throwable finalFailure = failure;
            UiContext.platform().post(() -> {
                if (gen != generation) return; // пришёл после более новой загрузки / arrived after a newer load
                Signals.batch(() -> {
                    if (finalFailure == null) {
                        value.set(finalResult);
                        status.set(Status.READY);
                    } else {
                        error.set(finalFailure);
                        status.set(Status.FAILED);
                    }
                });
            });
        });
    }

    // Отбросить результат текущей загрузки и вернуться в IDLE / Discard the current load's result and go back to IDLE
    public void reset() {
        generation++;
        Signals.batch(() -> {
            status.set(Status.IDLE);
            value.set(null);
            error.set(null);
        });
    }
}