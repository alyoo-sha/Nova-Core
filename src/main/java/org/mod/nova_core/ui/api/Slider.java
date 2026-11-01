package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.input.KeyboardEvent;
import org.mod.nova_core.ui.input.Keys;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.style.SliderStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.Objects;

// Горизонтальный ползунок значения от min до max; step > 0 привязывает значения к сетке
// A horizontal value slider from min to max; step > 0 snaps values to a grid
public class Slider extends Widget {
    private final StyleRef<SliderStyle> style = new StyleRef<>(Styles.SLIDER);
    private final Signal<Float> value;
    private final float min;
    private final float max;

    private float step = 0f;
    private boolean dragging = false;

    public Slider(int x, int y, int width, int height, float min, float max, float initial) {
        this(x, y, width, height, min, max, Signal.of(initial));
    }

    public Slider(int x, int y, int width, int height, float min, float max, Signal<Float> value) {
        super(x, y, width, height);
        if (!(max > min)) throw new IllegalArgumentException("max must be greater than min");
        this.min = min;
        this.max = max;
        this.value = Objects.requireNonNull(value);
    }

    public float getValue() { return clampValue(value.peek()); }

    public Signal<Float> valueSignal() { return value; }

    public Slider setValue(float v) {
        value.set(snap(v));
        return this;
    }

    public Slider setStep(float step) {
        this.step = Math.max(0f, step);
        return this;
    }

    public SliderStyle getStyle() { return style.get(); }

    public Slider setStyle(SliderStyle value) {
        style.set(value);
        requestLayout();
        return this;
    }

    @Override
    public boolean isFocusable() { return true; }

    private float clampValue(float v) { return Math.max(min, Math.min(max, v)); }

    private float snap(float v) {
        float c = clampValue(v);
        if (step > 0f) c = clampValue(min + Math.round((c - min) / step) * step);
        return c;
    }

    private float fraction() { return (clampValue(value.peek()) - min) / (max - min); }

    private void setFromMouse(double mx) {
        SliderStyle s = style.get();
        double range = Math.max(1, getWidth() - s.thumbWidth());
        double f = (mx - absX() - s.thumbWidth() / 2.0) / range;
        f = Math.max(0.0, Math.min(1.0, f));
        value.set(snap((float) (min + f * (max - min))));
    }

    @Override
    protected Size measureContent(Constraints c) {
        return new Size(100, style.get().thumbHeight());
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        SliderStyle s = style.get();
        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();

        int trackY = ay + (h - s.trackHeight()) / 2;
        int travel = Math.max(0, w - s.thumbWidth());
        int thumbX = ax + Math.round(fraction() * travel);
        int thumbY = ay + (h - s.thumbHeight()) / 2;

        g.fillRounded(ax, trackY, ax + w, trackY + s.trackHeight(), s.radius(), s.track());
        g.fillRounded(ax, trackY, thumbX + s.thumbWidth() / 2, trackY + s.trackHeight(), s.radius(),
                s.fill());

        boolean lit = dragging || isFocused() || (isActive() && isHovered());
        g.fillRounded(thumbX, thumbY, thumbX + s.thumbWidth(), thumbY + s.thumbHeight(), s.radius(),
                lit ? s.thumbHover() : s.thumb());
    }

    @Override
    protected void onPointerDown(PointerEvent e) {
        if (e.getButton() != MouseButton.LEFT) return;
        dragging = true;
        setFromMouse(e.getX());
        e.stopPropagation();
    }

    @Override
    protected void onPointerDrag(PointerEvent e) {
        if (!dragging) return;
        setFromMouse(e.getX());
        e.stopPropagation();
    }

    @Override
    protected void onPointerUp(PointerEvent e) {
        if (!dragging) return;
        dragging = false;
        e.stopPropagation();
    }

    @Override
    protected void onKeyDown(KeyboardEvent e) {
        float amount = step > 0f ? step : (max - min) / 20f;
        int key = e.getKey();
        if (key == Keys.LEFT || key == Keys.DOWN) setValue(clampValue(value.peek()) - amount);
        else if (key == Keys.RIGHT || key == Keys.UP) setValue(clampValue(value.peek()) + amount);
        else if (key == Keys.HOME) setValue(min);
        else if (key == Keys.END) setValue(max);
        else return;
        e.stopPropagation();
    }

    @Override
    protected void onPointerCancel(PointerEvent e) {
        dragging = false;
    }

    @Override
    public void onFocusChanged(boolean focused) {
        if (!focused) dragging = false;
    }
}