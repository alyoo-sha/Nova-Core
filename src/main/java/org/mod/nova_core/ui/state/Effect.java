package org.mod.nova_core.ui.state;

import java.util.Objects;
import java.util.function.Supplier;

// Действие, которое выполняется сразу и перезапускается при изменении прочитанных в нём сигналов.
// Вариант с cleanup возвращает функцию очистки: она вызывается перед каждым перезапуском и при отмене
// An action that runs immediately and re-runs when the signals it read change.
// The cleanup variant returns a cleanup function: it runs before every re-run and on cancel
public final class Effect implements Subscription {
    private final Supplier<Runnable> body;
    private final DependencyTracker tracker = new DependencyTracker(this::onSourceChanged);

    private Runnable cleanup = null;
    private boolean queued = false;
    private boolean running = false;
    private boolean disposed = false;

    private Effect(Supplier<Runnable> body) {
        this.body = Objects.requireNonNull(body);
    }

    // Выполняет body сразу; если оно бросит исключение, эффект отменяется
    // Runs body immediately; if it throws, the effect is cancelled
    public static Effect create(Runnable body) {
        Objects.requireNonNull(body);
        return createWithCleanup(() -> {
            body.run();
            return null;
        });
    }

    // body возвращает очистку (или null) / body returns a cleanup (or null)
    public static Effect createWithCleanup(Supplier<Runnable> body) {
        Effect effect = new Effect(body);
        Reactive.beginBatch();
        try {
            effect.execute();
        } catch (RuntimeException ex) {
            effect.cancel();
            throw ex;
        } finally {
            Reactive.endBatch();
        }
        return effect;
    }

    public boolean isCancelled() { return disposed; }

    @Override
    public void cancel() {
        if (disposed) return;
        disposed = true;
        tracker.clear();
        if (!running) runCleanup(); // иначе очистка выполнится в конце текущего прогона / otherwise the cleanup runs at the end of the current run
    }

    private void onSourceChanged() {
        if (disposed || queued) return;
        queued = true;
        Reactive.schedule(this);
    }

    private void execute() {
        if (running) throw new IllegalStateException("Effect re-entered itself");
        runCleanup();
        running = true;
        DependencyTracker previous = tracker.begin();
        try {
            cleanup = body.get();
        } finally {
            tracker.end(previous);
            running = false;
            if (disposed) { // отменили изнутри body / cancelled from inside the body
                tracker.clear();
                runCleanup();
            }
        }
    }

    private void runCleanup() {
        Runnable c = cleanup;
        cleanup = null;
        if (c != null) Signals.untracked(c);
    }

    // Вызывает очередь: перезапуск только если зависимость правда изменилась / Called by the queue: re-run only if a dependency really changed
    void runQueued() {
        queued = false;
        if (disposed) return;
        if (!tracker.dependenciesChanged()) return;
        execute();
    }

    void clearQueued() {
        queued = false;
    }
}