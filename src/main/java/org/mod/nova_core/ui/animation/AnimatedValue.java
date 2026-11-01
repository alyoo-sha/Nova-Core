package org.mod.nova_core.ui.animation;

import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.state.Subscription;

import java.util.Objects;

// Значение, которое плавно идёт к цели. Смена цели посреди анимации стартует новую от текущего значения
// A value that glides toward a target. Changing the target mid-animation starts a new one from the current value
public final class AnimatedValue<T> implements Subscription {
    private final Interpolator<T> interpolator;
    private final Signal<T> current;
    private T target;
    private long durationMs;
    private Easing easing;
    private Tween<T> running = null;

    public AnimatedValue(Interpolator<T> interpolator, T initial, long durationMs, Easing easing) {
        this.interpolator = Objects.requireNonNull(interpolator);
        this.current = Signal.of(initial);
        this.target = initial;
        this.durationMs = durationMs;
        this.easing = Objects.requireNonNull(easing);
    }

    // Текущее значение; внутри Computed и Effect создаёт зависимость / Current value; inside Computed and Effect it creates a dependency
    public T get() { return current.get(); }

    public T peek() { return current.peek(); }

    public ReadableSignal<T> signal() { return current; }

    public T target() { return target; }

    public boolean isAnimating() { return running != null && running.isRunning(); }

    public AnimatedValue<T> setDuration(long millis) { this.durationMs = millis; return this; }

    public AnimatedValue<T> setEasing(Easing easing) { this.easing = Objects.requireNonNull(easing); return this; }

    // Плавно идти к значению / Glide to the value
    public void setTarget(T next) {
        if (Objects.equals(next, target)) return;
        stop();
        target = next;
        T from = current.peek();
        if (durationMs <= 0L || Objects.equals(from, next)) {
            current.set(next);
            return;
        }
        running = Tween.of(interpolator, from, next)
                .duration(durationMs)
                .easing(easing)
                .onUpdate(v -> current.set(v))
                .onComplete(() -> running = null)
                .start();
    }

    // Мгновенно установить значение и цель / Instantly set the value and the target
    public void jumpTo(T value) {
        stop();
        target = value;
        current.set(value);
    }

    // Мгновенно дойти до цели / Instantly reach the target
    public void finish() {
        jumpTo(target);
    }

    // Остановить на текущем значении, оно становится новой целью / Stop at the current value, which becomes the new target
    @Override
    public void cancel() {
        stop();
        target = current.peek();
    }

    private void stop() {
        if (running != null) {
            running.cancel();
            running = null;
        }
    }
}