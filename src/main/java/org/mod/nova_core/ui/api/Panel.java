package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;

// Прямоугольник с фоном и рамкой / Rectangle with background and border
public class Panel extends Widget {
    // Цвета в формате ARGB / Colors are ARGB
    private int backgroundColor = 0xC0101010;
    private int borderColor = 0xFF404040;
    private int borderWidth = 1;

    public Panel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public Panel setBackgroundColor(int argb) { this.backgroundColor = argb; return this; }
    public Panel setBorderColor(int argb) { this.borderColor = argb; return this; }
    public Panel setBorderWidth(int borderWidth) { this.borderWidth = borderWidth; return this; }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();

        if ((backgroundColor >>> 24) != 0) g.fill(ax, ay, ax + w, ay + h, backgroundColor);

        int b = borderWidth;
        if (b > 0 && (borderColor >>> 24) != 0) {
            g.fill(ax, ay, ax + w, ay + b, borderColor);                 // верх / top
            g.fill(ax, ay + h - b, ax + w, ay + h, borderColor);         // низ / bottom
            g.fill(ax, ay + b, ax + b, ay + h - b, borderColor);         // лево / left
            g.fill(ax + w - b, ay + b, ax + w, ay + h - b, borderColor); // право / right
        }
    }
}