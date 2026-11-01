package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.style.PanelStyle;
import org.mod.nova_core.ui.style.StyleKey;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

// Прямоугольник с фоном и рамкой / Rectangle with background and border
public class Panel extends Widget {
    private final StyleRef<PanelStyle> style;

    public Panel(int x, int y, int width, int height) {
        this(x, y, width, height, Styles.PANEL);
    }

    // Стиль берётся из темы по этому ключу, например Styles.POPUP для непрозрачных всплывающих панелей
    // The style comes from the theme by this key, e.g. Styles.POPUP for opaque floating panels
    public Panel(int x, int y, int width, int height, StyleKey<PanelStyle> styleKey) {
        super(x, y, width, height);
        this.style = new StyleRef<>(styleKey);
    }

    public PanelStyle getStyle() { return style.get(); }

    // Свой стиль для этой панели; null = брать из темы / Own style for this panel; null = use the theme
    public Panel setStyle(PanelStyle value) {
        style.set(value);
        return this;
    }

    // Удобные сеттеры: фиксируют текущий стиль темы с одной заменой, дальнейшие смены темы на панель не влияют
    // Convenience setters: freeze the current theme style with one change, later theme switches no longer affect this panel
    public Panel setBackgroundColor(int argb) { return setStyle(style.get().withBackground(argb)); }
    public Panel setBorderColor(int argb) { return setStyle(style.get().withBorder(argb)); }
    public Panel setBorderWidth(int width) { return setStyle(style.get().withBorderWidth(width)); }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        PanelStyle s = style.get();
        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();

        if ((s.background() >>> 24) != 0) g.fill(ax, ay, ax + w, ay + h, s.background());

        int b = s.borderWidth();
        if (b > 0 && (s.border() >>> 24) != 0) {
            g.fill(ax, ay, ax + w, ay + b, s.border());                 // верх / top
            g.fill(ax, ay + h - b, ax + w, ay + h, s.border());         // низ / bottom
            g.fill(ax, ay + b, ax + b, ay + h - b, s.border());         // лево / left
            g.fill(ax + w - b, ay + b, ax + w, ay + h - b, s.border()); // право / right
        }
    }
}