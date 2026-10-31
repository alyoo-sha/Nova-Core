package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.layout.SizeMode;

// Однострочный текст / Single-line text
public class TextWidget extends Widget {
    private UiText text;
    private int color = 0xFFFFFFFF;
    private boolean shadow = true;
    // По горизонтали: START, CENTER, END (STRETCH = START) / Horizontal: START, CENTER, END (STRETCH = START)
    private Alignment alignment = Alignment.START;

    // Размер по тексту (WRAP) / Sized by the text (WRAP)
    public TextWidget(int x, int y, UiText text) {
        super(x, y, 0, 0);
        this.text = text;
        setSizeMode(SizeMode.WRAP, SizeMode.WRAP);
    }

    // Фиксированный размер, текст выравнивается внутри / Fixed size, text is aligned inside
    public TextWidget(int x, int y, int width, int height, UiText text) {
        super(x, y, width, height);
        this.text = text;
    }

    public UiText getText() { return text; }

    public TextWidget setText(UiText text) {
        this.text = text;
        if (getWidthMode() == SizeMode.WRAP || getHeightMode() == SizeMode.WRAP) requestLayout();
        return this;
    }

    public TextWidget setColor(int argb) { this.color = argb; return this; }
    public TextWidget setShadow(boolean shadow) { this.shadow = shadow; return this; }
    public TextWidget setAlignment(Alignment alignment) { this.alignment = alignment; return this; }

    @Override
    protected Size measureContent(Constraints c) {
        UiPlatform p = UiContext.platform();
        return new Size(p.textWidth(text), p.lineHeight());
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        UiPlatform p = UiContext.platform();
        int textWidth = p.textWidth(text);
        int tx = switch (alignment) {
            case CENTER -> absX() + (getWidth() - textWidth) / 2;
            case END -> absX() + getWidth() - textWidth;
            default -> absX();
        };
        int ty = absY() + (getHeight() - p.lineHeight()) / 2;
        g.drawText(text, tx, ty, color, shadow);
    }
}