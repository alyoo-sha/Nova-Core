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
}