package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.TextWrap;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.style.TextStyle;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.ArrayList;
import java.util.List;

// Многострочный текст с переносом по ширине. По умолчанию ширина FILL, высота WRAP (считается сама).
// Переносится простой текст (plain): стили внутри Component при переносе не сохраняются
// Multi-line text wrapped to the width. Width is FILL and height is WRAP (computed) by default.
// Plain text is wrapped: styles inside a Component are not kept
public class WrappedText extends Widget {
    private final StyleRef<TextStyle> style = new StyleRef<>(Styles.TEXT);
    // Строки, полученные при последней раскладке / Lines produced by the last layout
    private final List<String> lines = new ArrayList<>();

    private UiText text;
    // START, CENTER, END (STRETCH = START) / START, CENTER, END (STRETCH = START)
    private Alignment alignment = Alignment.START;
    private int lineSpacing = 1;

    public WrappedText(int x, int y, UiText text) {
        super(x, y, 0, 0);
        this.text = text;
        setSizeMode(SizeMode.FILL, SizeMode.WRAP);
    }

    public UiText getText() { return text; }

    public WrappedText setText(UiText text) {
        this.text = text;
        requestLayout();
        return this;
    }

    // Текст берётся из сигнала, пока виджет смонтирован / The text comes from a signal while the widget is mounted
    public WrappedText bindText(ReadableSignal<? extends UiText> source) {
        bind(source, t -> setText(t));
        return this;
    }

    public TextStyle getStyle() { return style.get(); }

    public WrappedText setStyle(TextStyle value) {
        style.set(value);
        return this;
    }

    public WrappedText setColor(int argb) { return setStyle(style.get().withColor(argb)); }

    public WrappedText setAlignment(Alignment alignment) {
        this.alignment = alignment;
        return this;
    }

    public WrappedText setLineSpacing(int spacing) {
        this.lineSpacing = Math.max(0, spacing);
        requestLayout();
        return this;
    }

    private List<String> wrapTo(int maxWidth) {
        UiPlatform p = UiContext.platform();
        return TextWrap.wrap(text.plain(), maxWidth, s -> p.textWidth(UiText.literal(s)));
    }

    @Override
    protected Size measureContent(Constraints c) {
        UiPlatform p = UiContext.platform();
        List<String> measured = wrapTo(c.maxWidth());
        int w = 0;
        for (String line : measured) w = Math.max(w, p.textWidth(UiText.literal(line)));
        int n = Math.max(1, measured.size());
        return new Size(w, n * p.lineHeight() + (n - 1) * lineSpacing);
    }

    @Override
    protected void layoutChildren() {
        lines.clear();
        lines.addAll(wrapTo(getWidth()));
        super.layoutChildren();
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        TextStyle s = style.get();
        UiPlatform p = UiContext.platform();
        int y = absY();
        for (String line : lines) {
            UiText lineText = UiText.literal(line);
            int lineWidth = p.textWidth(lineText);
            int x = switch (alignment) {
                case CENTER -> absX() + (getWidth() - lineWidth) / 2;
                case END -> absX() + getWidth() - lineWidth;
                default -> absX();
            };
            g.drawText(lineText, x, y, s.color(), s.shadow());
            y += p.lineHeight() + lineSpacing;
        }
    }
}