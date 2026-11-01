package org.mod.nova_core.ui.graphics;

// Рисование без привязки к версии Minecraft. Координаты в GUI-пикселях.
// Drawing without tying to a Minecraft version. Coordinates are in GUI pixels.
public interface UiGraphics {
    // Закрашенный прямоугольник [x1, x2) x [y1, y2), цвет ARGB / Filled rectangle, ARGB color
    void fill(int x1, int y1, int x2, int y2, int argb);

    void drawText(UiText text, int x, int y, int argb, boolean shadow);

    // Область обрезки; вложенные вызовы пересекаются / Clip area; nested calls intersect
    void pushClip(int x1, int y1, int x2, int y2);

    void popClip();

    // Множитель прозрачности для всего, что рисуется до popOpacity; вложенные перемножаются
    // Opacity multiplier for everything drawn until popOpacity; nested calls multiply
    void pushOpacity(float opacity);

    void popOpacity();

    // ---------- Скруглённые фигуры / Rounded shapes ----------
    // Запасные реализации рисуют прямоугольники, поэтому платформа может их не переопределять
    // The fallbacks draw rectangles, so a platform may leave them unimplemented

    default void fillRounded(float x1, float y1, float x2, float y2, float radius, int argb) {
        fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), argb);
    }

    // Цвета четырёх углов плавно перетекают друг в друга / The four corner colors blend smoothly into each other
    default void fillRoundedGradient(float x1, float y1, float x2, float y2, float radius,
                                     int topLeft, int topRight, int bottomLeft, int bottomRight) {
        fillRounded(x1, y1, x2, y2, radius, topLeft);
    }

    // Контур толщиной thickness внутрь от границы / An outline of the given thickness going inward from the bounds
    default void strokeRounded(float x1, float y1, float x2, float y2, float radius, float thickness, int argb) {
        int t = Math.max(1, Math.round(thickness));
        int ax = Math.round(x1);
        int ay = Math.round(y1);
        int bx = Math.round(x2);
        int by = Math.round(y2);
        fill(ax, ay, bx, ay + t, argb);
        fill(ax, by - t, bx, by, argb);
        fill(ax, ay + t, ax + t, by - t, argb);
        fill(bx - t, ay + t, bx, by - t, argb);
    }
}