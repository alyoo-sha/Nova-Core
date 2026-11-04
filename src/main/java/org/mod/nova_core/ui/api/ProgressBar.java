package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.style.SliderStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.function.Supplier;

// Полоса прогресса: заполняется слева направо, снизу вверх или сверху вниз. Значение 0..1 опрашивается каждый кадр
// A progress bar: fills left to right, bottom to top or top to bottom. The value 0..1 is polled every frame
public class ProgressBar extends Widget {
    public enum Direction { RIGHT, UP, DOWN }

    private final StyleRef<SliderStyle> style = new StyleRef<>(Styles.SLIDER);
    private final Direction direction;
    private Supplier<Float> source = () -> 0f;
    private int fillColor = 0;
    private boolean customFill = false;

    public ProgressBar(int x, int y, int width, int height, Direction direction) {
        super(x, y, width, height);
        this.direction = direction;
        setPointerTransparent(true);
    }

    public ProgressBar setProgressSource(Supplier<Float> source) {
        this.source = source;
        return this;
    }

    // Свой цвет заполнения вместо цвета темы / Own fill color instead of the theme's
    public ProgressBar setFillColor(int argb) {
        this.fillColor = argb;
        this.customFill = true;
        return this;
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        SliderStyle s = style.get();
        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();
        float p = Math.max(0f, Math.min(1f, source.get()));

        g.fillRounded(ax, ay, ax + w, ay + h, s.radius(), s.track());
        if (p <= 0f) return;

        int color = customFill ? fillColor : s.fill();
        switch (direction) {
            case RIGHT -> g.fillRounded(ax, ay, ax + Math.round(w * p), ay + h, s.radius(), color);
            case UP -> g.fillRounded(ax, ay + h - Math.round(h * p), ax + w, ay + h, s.radius(), color);
            case DOWN -> g.fillRounded(ax, ay, ax + w, ay + Math.round(h * p), s.radius(), color);
        }
    }
}