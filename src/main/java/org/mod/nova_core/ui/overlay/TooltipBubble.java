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

import java.util.ArrayList;
import java.util.List;

// Облачко тултипа: текст переносится по ширине maxWidth, «\n» начинает новую строку
// A tooltip bubble: the text wraps to maxWidth, "\n" starts a new line
public final class TooltipBubble extends Widget {
    private static final int DEFAULT_MAX_WIDTH = 220;
    private static final int LINE_SPACING = 1;

    private final StyleRef<TooltipStyle> style = new StyleRef<>(Styles.TOOLTIP);
    private final List<UiText> lines = new ArrayList<>();
    private UiText text;
    private int maxWidth = DEFAULT_MAX_WIDTH;

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

    // Максимальная ширина облачка вместе с отступами / The maximum bubble width including padding
    public TooltipBubble setMaxWidth(int maxWidth) {
        this.maxWidth = Math.max(40, maxWidth);
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
        lines.clear();
        lines.addAll(p.wrap(text, Math.max(20, maxWidth - s.paddingX() * 2)));
        int w = 0;
        for (UiText line : lines) w = Math.max(w, p.textWidth(line));
        int n = Math.max(1, lines.size());
        return new Size(w + s.paddingX() * 2, n * p.lineHeight() + (n - 1) * LINE_SPACING + s.paddingY() * 2);
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        TooltipStyle s = style.get();
        UiPlatform p = UiContext.platform();
        int ax = absX();
        int ay = absY();
        Draw.box(g, ax, ay, getWidth(), getHeight(), s.radius(), s.background(), s.border(), 1);
        int y = ay + s.paddingY();
        for (UiText line : lines) {
            g.drawText(line, ax + s.paddingX(), y, s.text(), s.textShadow());
            y += p.lineHeight() + LINE_SPACING;
        }
    }
}