package org.mod.nova_core.ui.animation;

import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.state.Subscription;

// Значение, которое идёт к цели по законам пружины: с разгоном, лёгким перелётом и затуханием.
// Смена цели на ходу сохраняет скорость, поэтому движение не рвётся
// A value that heads to its target by spring physics: accelerating, with a slight overshoot and damping.
// Changing the target mid-flight keeps the velocity, so the motion never breaks
public final class SpringValue implements Subscription {
    private final Signal<Float> value;
    private float target;
    private float velocity = 0f;
    private float stiffness = 180f;
    private float damping = 22f;
    private long lastNanos = 0L;
    private boolean running = false;
    private int token = 0;

    public SpringValue(float initial) {
        this.value = Signal.of(initial);
        this.target = initial;
    }

    public float get() { return value.get(); }

    public float peek() { return value.peek(); }

    public ReadableSignal<Float> signal() { return value; }

    public float target() { return target; }

    public boolean isAnimating() { return running; }

    // Жёсткость и затухание; критическое затухание примерно 2 * sqrt(stiffness) / Stiffness and damping; critical damping is about 2 * sqrt(stiffness)
    public SpringValue setPhysics(float stiffness, float damping) {
        this.stiffness = Math.max(1f, stiffness);
        this.damping = Math.max(0f, damping);
        return this;
    }

    public void setTarget(float next) {
        target = next;
        if (!running) start();
    }

    // Мгновенно, без движения / Instantly, with no motion
    public void jumpTo(float next) {
        token++;
        running = false;
        velocity = 0f;
        target = next;
        value.set(next);
    }

    @Override
    public void cancel() {
        token++;
        running = false;
        velocity = 0f;
        target = value.peek();
    }

    private void start() {
        running = true;
        lastNanos = 0L;
        int mine = ++token;
        Animator.shared().addTask(now -> mine == token && step(now));
    }

    private boolean step(long now) {
        if (lastNanos == 0L) {
            lastNanos = now;
            return true;
        }
        float dt = Math.min(0.05f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;

        float x = value.peek();
        int steps = Math.max(1, (int) Math.ceil(dt / 0.004f));
        float h = dt / steps;
        for (int i = 0; i < steps; i++) {
            float acceleration = stiffness * (target - x) - damping * velocity;
            velocity += acceleration * h;
            x += velocity * h;
        }

        boolean settled = Math.abs(target - x) < 0.01f && Math.abs(velocity) < 0.01f;
        if (settled) {
            x = target;
            velocity = 0f;
            running = false;
        }
        value.set(x);
        return !settled;
    }
}