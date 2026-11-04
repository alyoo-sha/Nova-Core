package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.style.SliderStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.style.TextStyle;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.function.Supplier;

// Стрелка-индикатор прогресса механизма / A machine progress arrow
public class ArrowWidget extends Widget {
    public static final int WIDTH = 22;
    public static final int HEIGHT = 15;

    private final StyleRef<TextStyle> baseStyle = new StyleRef<>(Styles.TEXT);
    private final StyleRef<SliderStyle> fillStyle = new StyleRef<>(Styles.SLIDER);
    private float progress = 0f;
    // Если задан, опрашивается каждый кадр вместо progress / If set, polled every frame instead of progress
    private Supplier<Float> source = null;

    public ArrowWidget(int x, int y) {
        super(x, y, WIDTH, HEIGHT);
        setPointerTransparent(true);
    }

    public ArrowWidget setProgress(float progress) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        return this;
    }

    // Прогресс 0..1 из сигнала, пока виджет смонтирован / Progress 0..1 from a signal while the widget is mounted
    public ArrowWidget bindProgress(ReadableSignal<Float> signal) {
        bind(signal, p -> setProgress(p));
        return this;
    }

    // Прогресс 0..1, который опрашивается каждый кадр (удобно для данных меню) / Progress 0..1 polled every frame (handy for menu data)
    public ArrowWidget setProgressSource(Supplier<Float> source) {
        this.source = source;
        return this;
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        int ax = absX();
        int ay = absY();
        float p = source != null ? Math.max(0f, Math.min(1f, source.get())) : progress;
        shape(g, ax, ay, (baseStyle.get().color() & 0xFFFFFF) | 0x50000000);
        if (p > 0f) {
            g.pushClip(ax, ay, ax + Math.round(WIDTH * p), ay + HEIGHT);
            shape(g, ax, ay, fillStyle.get().fill());
            g.popClip();
        }
    }

    // Стержень и наконечник из вертикальных полос / A stem and a head made of vertical strips
    private static void shape(UiGraphics g, int ax, int ay, int color) {
        g.fill(ax, ay + 5, ax + 14, ay + 10, color);
        for (int i = 0; i < 8; i++) g.fill(ax + 14 + i, ay + i, ax + 15 + i, ay + HEIGHT - i, color);
    }
}