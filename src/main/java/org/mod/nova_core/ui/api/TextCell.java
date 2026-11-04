package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.collection.ItemAdapter;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.FieldStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.function.Function;

// Строка списка: текст с подсветкой при наведении и выборе. Размер задаёт список
// A list row: text with hover and selection highlight. The size is set by the list
public class TextCell extends Widget {
    private final StyleRef<ButtonStyle> buttonStyle = new StyleRef<>(Styles.BUTTON);
    private final StyleRef<FieldStyle> fieldStyle = new StyleRef<>(Styles.FIELD);

    private UiText text = UiText.literal("");
    private boolean selected = false;
    private int paddingX = 4;

    public TextCell() {
        super(0, 0, 0, 0);
    }

    public UiText getText() { return text; }

    public TextCell setText(UiText text) {
        this.text = text;
        return this;
    }

    public boolean isSelected() { return selected; }

    public TextCell setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public TextCell setPaddingX(int paddingX) {
        this.paddingX = Math.max(0, paddingX);
        return this;
    }

    // Готовый адаптер для текстовых списков / A ready-made adapter for text lists
    public static <T> ItemAdapter<T> adapter(Function<? super T, ? extends UiText> label) {
        return ItemAdapter.<T, TextCell>of(TextCell::new, (cell, item, index, selected) -> {
            cell.setText(label.apply(item));
            cell.setSelected(selected);
        });
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        ButtonStyle b = buttonStyle.get();
        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();

        if (selected) g.fillRounded(ax, ay, ax + w, ay + h, b.radius(), fieldStyle.get().selection());
        else if (isHovered()) g.fillRounded(ax, ay, ax + w, ay + h, b.radius(), b.hover());

        UiPlatform p = UiContext.platform();
        g.drawText(text, ax + paddingX, ay + (h - p.lineHeight()) / 2, b.text(), b.textShadow());
    }
}