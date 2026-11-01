package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.Draw;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.style.TooltipStyle;
import org.mod.nova_core.ui.theme.StyleRef;

// Облачко тултипа с однострочным текстом / A tooltip bubble with single-line text
public final class TooltipBubble extends Widget {
    private final StyleRef<TooltipStyle> style = new StyleRef<>(Styles.TOOLTIP);
    private UiText text;

    public TooltipBubble(UiText text) {
        super(0, 0, 0, 0);
        this.text = text;
        setSizeMode(SizeMode.WRAP, SizeMode.WRAP);
    }

    public UiText getText() { return text; }

    public TooltipBubble setText(UiText text) {
        this.text = text;
        requestLayout();
        return this;
    }

    public TooltipStyle getStyle() { return style.get(); }

    public TooltipBubble setStyle(TooltipStyle value) {
        style.set(value);
        requestLayout();
        return this;
    }

    @Override
    protected Size measureContent(Constraints c) {
        TooltipStyle s = style.get();
        UiPlatform p = UiContext.platform();
        return new Size(p.textWidth(text) + s.paddingX() * 2, p.lineHeight() + s.paddingY() * 2);
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        TooltipStyle s = style.get();
        int ax = absX();
        int ay = absY();
        Draw.box(g, ax, ay, getWidth(), getHeight(), s.radius(), s.background(), s.border(), 1);
        g.drawText(text, ax + s.paddingX(), ay + s.paddingY(), s.text(), s.textShadow());
    }
}