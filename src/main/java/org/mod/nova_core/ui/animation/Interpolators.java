package org.mod.nova_core.ui.animation;

public final class Interpolators {
    public static final Interpolator<Float> FLOAT = (a, b, t) -> a * (1f - t) + b * t;

    public static final Interpolator<Integer> INT =
            (a, b, t) -> (int) Math.round((double) a * (1.0 - t) + (double) b * t);

    // Цвета ARGB: каждый канал отдельно, результат ограничивается 0..255 / ARGB colors: each channel separately, clamped to 0..255
    public static final Interpolator<Integer> COLOR = (a, b, t) -> lerpColor(a, b, t);

    private Interpolators() {}

    private static int lerpColor(int a, int b, float t) {
        int result = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            int c = Math.round(ca * (1f - t) + cb * t);
            result |= (Math.max(0, Math.min(255, c)) & 0xFF) << shift;
        }
        return result;
    }
}