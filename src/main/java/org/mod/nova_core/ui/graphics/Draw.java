package org.mod.nova_core.ui.graphics;

// Общие приёмы рисования для виджетов / Shared drawing helpers for widgets
public final class Draw {
    private Draw() {}

    // Фон с рамкой; при radius <= 0 рисуется как раньше, прямыми углами / A box with a border; for radius <= 0 it is drawn as before, with square corners
    public static void box(UiGraphics g, int x, int y, int w, int h, int radius, int fill, int border, int borderWidth) {
        boolean hasBorder = borderWidth > 0 && (border >>> 24) != 0;
        if (radius <= 0) {
            if (hasBorder) {
                g.fill(x, y, x + w, y + h, border);
                g.fill(x + borderWidth, y + borderWidth, x + w - borderWidth, y + h - borderWidth, fill);
            } else {
                g.fill(x, y, x + w, y + h, fill);
            }
            return;
        }
        g.fillRounded(x, y, x + w, y + h, radius, fill);
        if (hasBorder) g.strokeRounded(x, y, x + w, y + h, radius, borderWidth, border);
    }
}