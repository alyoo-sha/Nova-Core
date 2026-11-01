package org.mod.nova_core.ui.animation;

// Как получить промежуточное значение между from и to при прогрессе t (t может выходить за 0..1 при перелёте)
// How to get an in-between value of from and to at progress t (t may leave 0..1 on overshoot)
@FunctionalInterface
public interface Interpolator<T> {
    T interpolate(T from, T to, float t);
}