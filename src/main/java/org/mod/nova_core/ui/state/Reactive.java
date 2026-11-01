package org.mod.nova_core.ui.state;

import java.util.ArrayDeque;

// Общее состояние реактивного слоя: текущий отслеживающий узел, пакеты изменений и очередь эффектов
// Shared reactive state: the current tracking node, change batches and the effect queue
final class Reactive {
    private static final int MAX_EFFECT_RUNS_PER_FLUSH = 10_000;

    private static final ArrayDeque<Effect> QUEUE = new ArrayDeque<>();
    private static DependencyTracker current = null;
    private static int batchDepth = 0;
    private static boolean flushing = false;

    private Reactive() {}

    // Возвращает предыдущий, его нужно вернуть через exit / Returns the previous one, restore it via exit
    static DependencyTracker enter(DependencyTracker tracker) {
        DependencyTracker previous = current;
        current = tracker;
        return previous;
    }

    static void exit(DependencyTracker previous) {
        current = previous;
    }

    static void track(AbstractSignal<?> source) {
        if (current != null) current.recordRead(source);
    }

    static void beginBatch() {
        batchDepth++;
    }

    static void endBatch() {
        batchDepth--;
        if (batchDepth == 0) flush();
    }

    static void schedule(Effect effect) {
        QUEUE.add(effect);
        if (batchDepth == 0) flush();
    }

    // Выполняет очередь эффектов; вложенные вызовы ничего не делают, очередь разбирает внешний
    // Runs the effect queue; nested calls do nothing, the outer one drains the queue
    private static void flush() {
        if (flushing || QUEUE.isEmpty()) return;
        flushing = true;
        RuntimeException first = null;
        int runs = 0;
        try {
            Effect e;
            while ((e = QUEUE.poll()) != null) {
                if (++runs > MAX_EFFECT_RUNS_PER_FLUSH) {
                    e.clearQueued();
                    while ((e = QUEUE.poll()) != null) e.clearQueued();
                    throw new IllegalStateException("Reactive cycle: effects keep triggering each other");
                }
                try {
                    e.runQueued();
                } catch (RuntimeException ex) {
                    if (first == null) first = ex; // остальные эффекты всё равно выполнятся / the other effects still run
                }
            }
        } finally {
            flushing = false;
        }
        if (first != null) throw first;
    }
}