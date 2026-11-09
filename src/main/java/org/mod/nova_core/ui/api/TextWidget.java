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
import org.mod.nova_core.ui.overlay.TooltipBubble;
import org.mod.nova_core.ui.overlay.Tooltips;
import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.style.TextStyle;
import org.mod.nova_core.ui.theme.StyleRef;

// Однострочный текст. С setEllipsize(true) длинный текст обрезается «…», а при наведении показывается целиком
// Single-line text. With setEllipsize(true) long text is cut with "…" and shown in full on hover
public class TextWidget extends Widget {
    private final StyleRef<TextStyle> style = new StyleRef<>(Styles.TEXT);

    private UiText text;
    // По горизонтали: START, CENTER, END (STRETCH = START) / Horizontal: START, CENTER, END (STRETCH = START)
    private Alignment alignment = Alignment.START;
    private boolean ellipsize = false;
    private boolean clipped = false;

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

    // Текст берётся из сигнала, пока виджет смонтирован / The text comes from a signal while the widget is mounted
    public TextWidget bindText(ReadableSignal<? extends UiText> source) {
        bind(source, t -> setText(t));
        return this;
    }

    // Обрезать по ширине виджета с «…» и показывать полный текст при наведении / Cut to the widget width with "…" and show the full text on hover
    public TextWidget setEllipsize(boolean value) {
        if (value && !ellipsize) Tooltips.attach(this, () -> clipped ? new TooltipBubble(text) : null, 250L);
        this.ellipsize = value;
        return this;
    }

    public TextStyle getStyle() { return style.get(); }

    // Свой стиль для этого текста; null = брать из темы / Own style for this text; null = use the theme
    public TextWidget setStyle(TextStyle value) {
        style.set(value);
        return this;
    }

    // Удобные сеттеры: фиксируют текущий стиль темы с одной заменой / Convenience setters: freeze the current theme style with one change
    public TextWidget setColor(int argb) { return setStyle(style.get().withColor(argb)); }
    public TextWidget setShadow(boolean shadow) { return setStyle(style.get().withShadow(shadow)); }

    public TextWidget setAlignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    @Override
    protected Size measureContent(Constraints c) {
        UiPlatform p = UiContext.platform();
        return new Size(p.textWidth(text), p.lineHeight());
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        TextStyle s = style.get();
        UiPlatform p = UiContext.platform();
        UiText shown = text;
        int textWidth = p.textWidth(shown);
        clipped = false;
        if (ellipsize && getWidth() > 0 && textWidth > getWidth()) {
            shown = p.ellipsize(text, getWidth());
            textWidth = p.textWidth(shown);
            clipped = true;
        }
        int tx = switch (alignment) {
            case CENTER -> absX() + (getWidth() - textWidth) / 2;
            case END -> absX() + getWidth() - textWidth;
            default -> absX();
        };
        int ty = absY() + (getHeight() - p.lineHeight()) / 2;
        g.drawText(shown, tx, ty, s.color(), s.shadow());
    }
}