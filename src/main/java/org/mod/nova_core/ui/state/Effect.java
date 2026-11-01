package org.mod.nova_core.ui.state;

import java.util.Objects;

// Действие, которое выполняется сразу и перезапускается при изменении прочитанных в нём сигналов
// An action that runs immediately and re-runs when the signals it read change
public final class Effect implements Subscription {
    private final Runnable body;
    private final DependencyTracker tracker = new DependencyTracker(this::onSourceChanged);

    private boolean queued = false;
    private boolean running = false;
    private boolean disposed = false;

    private Effect(Runnable body) {
        this.body = Objects.requireNonNull(body);
    }

    // Выполняет body сразу; если оно бросит исключение, эффект отменяется
    // Runs body immediately; if it throws, the effect is cancelled
    public static Effect create(Runnable body) {
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
    }

    private void onSourceChanged() {
        if (disposed || queued) return;
        queued = true;
        Reactive.schedule(this);
    }

    private void execute() {
        if (running) throw new IllegalStateException("Effect re-entered itself");
        running = true;
        DependencyTracker previous = tracker.begin();
        try {
            body.run();
        } finally {
            tracker.end(previous);
            running = false;
            if (disposed) tracker.clear(); // отменили изнутри body / canceled from inside the body
        }
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