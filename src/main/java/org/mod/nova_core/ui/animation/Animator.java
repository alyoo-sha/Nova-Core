package org.mod.nova_core.ui.animation;

import java.util.ArrayList;

// Планировщик анимаций. Хост вызывает tick один раз за кадр; повторный вызов с тем же временем игнорируется
// Animation scheduler. The host calls tick once per frame; a repeated call with the same time is ignored
public final class Animator {
    // Покадровая задача: update возвращает false, когда закончила / A per-frame task: update returns false when it is done
    public interface FrameTask {
        boolean update(long nowNanos);
    }

    private static final class Entry {
        final FrameTask task;
        boolean done = false;

        Entry(FrameTask task) {
            this.task = task;
        }
    }

    private static final Animator SHARED = new Animator();

    // Активные анимации и новые, добавленные во время tick / Active animations and new ones added during tick
    private final ArrayList<Tween<?>> active = new ArrayList<>();
    private final ArrayList<Tween<?>> pending = new ArrayList<>();
    private final ArrayList<Entry> tasks = new ArrayList<>();
    private final ArrayList<Entry> pendingTasks = new ArrayList<>();
    private long lastTick = Long.MIN_VALUE;
    private boolean ticking = false;

    private Animator() {}

    public static Animator shared() { return SHARED; }

    <T> Tween<T> start(Tween<T> tween) {
        if (ticking) pending.add(tween);
        else active.add(tween);
        return tween;
    }

    public void addTask(FrameTask task) {
        Entry entry = new Entry(task);
        if (ticking) pendingTasks.add(entry);
        else tasks.add(entry);
    }

    public void tick(long nowNanos) {
        if (nowNanos == lastTick) return;
        lastTick = nowNanos;

        ticking = true;
        RuntimeException first = null;
        try {
            for (int i = 0; i < active.size(); i++) {
                Tween<?> tween = active.get(i);
                try {
                    tween.step(nowNanos);
                } catch (RuntimeException ex) {
                    tween.cancel(); // сломанная анимация не должна падать каждый кадр / a broken animation must not fail every frame
                    if (first == null) first = ex;
                }
            }
            for (int i = 0; i < tasks.size(); i++) {
                Entry entry = tasks.get(i);
                try {
                    if (!entry.task.update(nowNanos)) entry.done = true;
                } catch (RuntimeException ex) {
                    entry.done = true;
                    if (first == null) first = ex;
                }
            }
        } finally {
            ticking = false;
            active.removeIf(t -> !t.isRunning());
            active.addAll(pending);
            pending.clear();
            tasks.removeIf(e -> e.done);
            tasks.addAll(pendingTasks);
            pendingTasks.clear();
        }
        if (first != null) throw first;
    }

    public boolean hasActive() {
        return !active.isEmpty() || !pending.isEmpty() || !tasks.isEmpty() || !pendingTasks.isEmpty();
    }

    // Отменить все анимации / Cancel all animations
    public void cancelAll() {
        for (Tween<?> t : active) t.cancel();
        for (Tween<?> t : pending) t.cancel();
        active.clear();
        pending.clear();
        tasks.clear();
        pendingTasks.clear();
    }
}