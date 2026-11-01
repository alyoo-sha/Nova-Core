package org.mod.nova_core.ui.animation;

import org.mod.nova_core.ui.state.Subscription;

import java.util.function.Consumer;

// Анимация значения от from к to за заданное время. Создаётся через Tween.of(...) / floats / ints / colors
// An animation of a value from `from` to `to` over a given time. Created via Tween.of(...) / floats / ints / colors
public final class Tween<T> implements Subscription {
    // Бесконечное число проигрываний / Endless number of plays
    public static final int INFINITE = Integer.MAX_VALUE;
    private static final long UNSET = Long.MIN_VALUE;

    private final Interpolator<T> interpolator;
    private final T from;
    private final T to;
    private final long durationNanos;
    private final long delayNanos;
    private final Easing easing;
    private final int plays;
    private final boolean yoyo;
    private final Consumer<? super T> onUpdate;
    private final Runnable onComplete;

    private long startNanos = UNSET;
    private boolean running = true;
    private boolean finished = false;

    private Tween(Builder<T> b) {
        this.interpolator = b.interpolator;
        this.from = b.from;
        this.to = b.to;
        this.durationNanos = b.durationMs * 1_000_000L;
        this.delayNanos = b.delayMs * 1_000_000L;
        this.easing = b.easing;
        this.plays = b.plays;
        this.yoyo = b.yoyo;
        this.onUpdate = b.onUpdate;
        this.onComplete = b.onComplete;
    }

    public static <T> Builder<T> of(Interpolator<T> interpolator, T from, T to) {
        return new Builder<>(interpolator, from, to);
    }

    public static Builder<Float> floats(float from, float to) {
        return new Builder<>(Interpolators.FLOAT, from, to);
    }

    public static Builder<Integer> ints(int from, int to) {
        return new Builder<>(Interpolators.INT, from, to);
    }

    public static Builder<Integer> colors(int from, int to) {
        return new Builder<>(Interpolators.COLOR, from, to);
    }

    // Идёт ли анимация (после cancel, finish или естественного конца: false) / Whether it is running (false after cancel, finish or its natural end)
    public boolean isRunning() { return running; }

    // Дошла ли до конца (через время или через finish) / Whether it reached the end (by time or via finish)
    public boolean isFinished() { return finished; }

    // Остановить на текущем значении; onComplete не вызывается / Stop at the current value; onComplete is not called
    @Override
    public void cancel() {
        running = false;
    }

    // Завершить сейчас: применить конечное значение и вызвать onComplete / Finish now: apply the final value and call onComplete
    public void finish() {
        if (running) complete();
    }

    private void complete() {
        running = false;
        finished = true;
        // При yoyo с чётным числом проигрываний значение заканчивается на from / With yoyo and an even number of plays the value ends at `from`
        boolean reversed = yoyo && plays != INFINITE && ((plays - 1) % 2 == 1);
        float t = reversed ? 0f : 1f;
        onUpdate.accept(interpolator.interpolate(from, to, easing.apply(t)));
        if (onComplete != null) onComplete.run();
    }

    // Вызывается Animator на каждом кадре / Called by the Animator every frame
    void step(long nowNanos) {
        if (!running) return;
        if (startNanos == UNSET) startNanos = nowNanos;

        long elapsed = nowNanos - startNanos - delayNanos;
        if (elapsed < 0L) return; // ещё ждём задержку / still waiting out the delay
        if (durationNanos <= 0L) {
            complete();
            return;
        }

        long iteration = elapsed / durationNanos;
        if (plays != INFINITE && iteration >= plays) {
            complete();
            return;
        }

        float local = (float) (elapsed % durationNanos) / (float) durationNanos;
        if (yoyo && (iteration & 1L) == 1L) local = 1f - local;
        onUpdate.accept(interpolator.interpolate(from, to, easing.apply(local)));
    }

    public static final class Builder<T> {
        private final Interpolator<T> interpolator;
        private final T from;
        private final T to;
        private long durationMs = 250L;
        private long delayMs = 0L;
        private Easing easing = Easing.EASE_OUT;
        private int plays = 1;
        private boolean yoyo = false;
        private Consumer<? super T> onUpdate = v -> { };
        private Runnable onComplete = null;

        private Builder(Interpolator<T> interpolator, T from, T to) {
            this.interpolator = interpolator;
            this.from = from;
            this.to = to;
        }

        public Builder<T> duration(long millis) { this.durationMs = Math.max(0L, millis); return this; }
        public Builder<T> delay(long millis) { this.delayMs = Math.max(0L, millis); return this; }
        public Builder<T> easing(Easing easing) { this.easing = easing; return this; }

        // Сколько раз проиграть (минимум 1) / How many times to play (at least 1)
        public Builder<T> plays(int count) { this.plays = Math.max(1, count); return this; }

        public Builder<T> loop() { this.plays = INFINITE; return this; }

        // Чётные проигрывания идут вперёд, нечётные назад / Even plays run forward, odd plays run backward
        public Builder<T> yoyo(boolean yoyo) { this.yoyo = yoyo; return this; }

        // Вызывается на каждом кадре со значением и в конце с конечным / Called every frame with the value and at the end with the final one
        public Builder<T> onUpdate(Consumer<? super T> onUpdate) { this.onUpdate = onUpdate; return this; }

        public Builder<T> onComplete(Runnable onComplete) { this.onComplete = onComplete; return this; }

        // Создать и сразу запустить на общем Animator / Create and start right away on the shared Animator
        public Tween<T> start() {
            return Animator.shared().start(new Tween<>(this));
        }
    }
}