package org.mod.nova_core.ui.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;

// UiGraphics поверх GuiGraphics (Forge 1.20.1) / UiGraphics over GuiGraphics (Forge 1.20.1)
public final class McGraphics implements UiGraphics {
    private GuiGraphics delegate = null;

    // Начало и конец кадра / Start and end of a frame
    public void begin(GuiGraphics g) { this.delegate = g; }
    public void end() { this.delegate = null; }

    // Доступ к настоящему GuiGraphics для того, чего ещё нет в UiGraphics (например, предметы)
    // Access to the real GuiGraphics for what UiGraphics lacks yet (e.g. Items)
    public GuiGraphics raw() { return delegate; }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        delegate.fill(x1, y1, x2, y2, argb);
    }

    @Override
    public void drawText(UiText text, int x, int y, int argb, boolean shadow) {
        delegate.drawString(Minecraft.getInstance().font, McText.toComponent(text), x, y, argb, shadow);
    }

    @Override
    public void pushClip(int x1, int y1, int x2, int y2) {
        delegate.enableScissor(x1, y1, x2, y2);
    }

    @Override
    public void popClip() {
        delegate.disableScissor();
    }
}