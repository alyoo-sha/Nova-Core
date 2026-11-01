package org.mod.nova_core.ui.animation;

// Функция плавности: превращает линейный прогресс 0..1 в «ускоренный». apply(0) = 0, apply(1) = 1
// Easing function: turns linear progress 0..1 into an "eased" one. apply(0) = 0, apply(1) = 1
@FunctionalInterface
public interface Easing {
    float apply(float t);

    Easing LINEAR = t -> t;
    Easing EASE_IN = t -> t * t * t;
    Easing EASE_OUT = t -> {
        float u = 1f - t;
        return 1f - u * u * u;
    };
    Easing EASE_IN_OUT = t -> {
        if (t < 0.5f) return 4f * t * t * t;
        float u = -2f * t + 2f;
        return 1f - u * u * u / 2f;
    };
    // Небольшой перелёт за цель и возврат / A slight overshoot past the target and back
    Easing BACK_OUT = t -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    };

    // Кубическая кривая Безье как в CSS cubic-bezier; x1 и x2 должны быть в диапазоне 0..1
    // Cubic Bezier curve as in CSS cubic-bezier; x1 and x2 must be within 0..1
    static Easing cubicBezier(float x1, float y1, float x2, float y2) {
        return t -> {
            if (t <= 0f) return 0f;
            if (t >= 1f) return 1f;
            // Бинарный поиск параметра кривой по x / Binary search for the curve parameter by x
            float lo = 0f;
            float hi = 1f;
            float s = t;
            for (int i = 0; i < 24; i++) {
                s = (lo + hi) * 0.5f;
                if (bezier(s, x1, x2) < t) lo = s;
                else hi = s;
            }
            return bezier(s, y1, y2);
        };
    }

    private static float bezier(float s, float p1, float p2) {
        float u = 1f - s;
        return 3f * u * u * s * p1 + 3f * u * s * s * p2 + s * s * s;
    }
}