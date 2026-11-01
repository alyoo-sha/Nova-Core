package org.mod.nova_core.ui.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;

import java.util.ArrayDeque;

// UiGraphics поверх GuiGraphics (Forge 1.20.1) / UiGraphics over GuiGraphics (Forge 1.20.1)
public final class McGraphics implements UiGraphics {
    private GuiGraphics delegate = null;
    // Стек множителей прозрачности и текущее произведение / Opacity multiplier stack and the current product
    private final ArrayDeque<Float> opacityStack = new ArrayDeque<>();
    private float opacity = 1f;

    // Начало и конец кадра / Start and end of a frame
    public void begin(GuiGraphics g) {
        this.delegate = g;
        opacityStack.clear();
        opacity = 1f;
    }

    public void end() { this.delegate = null; }

    // Доступ к настоящему GuiGraphics для того, чего ещё нет в UiGraphics (например, предметы)
    // Access to the real GuiGraphics for what UiGraphics lacks yet (e.g. items)
    public GuiGraphics raw() { return delegate; }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        int c = applyOpacity(argb);
        if ((c >>> 24) == 0) return;
        delegate.fill(x1, y1, x2, y2, c);
    }

    @Override
    public void drawText(UiText text, int x, int y, int argb, boolean shadow) {
        // Font считает альфу меньше 4 «отсутствующей» и рисует непрозрачно; повторяем это правило здесь,
        // чтобы затухание не давало вспышек
        // Font treats alpha below 4 as "missing" and draws opaque; we mirror that rule here
        // so fading produces no flashes
        if ((argb & 0xFC000000) == 0) argb |= 0xFF000000;
        int c = applyOpacity(argb);
        if ((c >>> 24) < 4) return;

        Font font = Minecraft.getInstance().font;
        Component component = McText.toComponent(text);

        // Встроенная тень сдвигает основной текст по глубине (z + 0.03), и позже нарисованные заливки его не закрывают.
        // Поэтому тень рисуем сами: тот же цвет, затемнённый до четверти, со сдвигом (1, 1), всё на одной глубине.
        // The built-in shadow moves the main text in depth (z + 0.03), and later fills do not cover it.
        // So we draw the shadow ourselves: the same color dimmed to a quarter, offset by (1, 1), all at one depth.
        if (shadow) {
            int shadowColor = ((c & 0xFCFCFC) >> 2) | (c & 0xFF000000);
            delegate.drawString(font, Component.literal(component.getString()), x + 1, y + 1, shadowColor, false);
        }
        delegate.drawString(font, component, x, y, c, false);
    }

    @Override
    public void pushClip(int x1, int y1, int x2, int y2) {
        delegate.enableScissor(x1, y1, x2, y2);
    }

    @Override
    public void popClip() {
        delegate.disableScissor();
    }

    @Override
    public void pushOpacity(float value) {
        opacityStack.push(opacity);
        opacity *= Math.max(0f, Math.min(1f, value));
    }

    @Override
    public void popOpacity() {
        opacity = opacityStack.isEmpty() ? 1f : opacityStack.pop();
    }

    private int applyOpacity(int argb) {
        if (opacity >= 1f) return argb;
        int a = Math.round((argb >>> 24) * opacity);
        return (a << 24) | (argb & 0xFFFFFF);
    }
}