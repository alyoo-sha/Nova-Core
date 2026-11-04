package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiItem;
import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.FieldStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

// Ячейка с предметом по центру: подсветка при наведении и выборе, подсказка предмета. Размер задаёт список или сетка
// A cell with an item in the middle: hover and selection highlight, the item tooltip. The size is set by the list or grid
public class ItemCell extends Widget {
    private final StyleRef<ButtonStyle> buttonStyle = new StyleRef<>(Styles.BUTTON);
    private final StyleRef<FieldStyle> fieldStyle = new StyleRef<>(Styles.FIELD);

    private UiItem item = null;
    private boolean selected = false;
    private boolean decorations = true;

    public ItemCell() {
        super(0, 0, 0, 0);
    }

    public ItemCell set(UiItem item, boolean selected) {
        this.item = item;
        this.selected = selected;
        return this;
    }

    public ItemCell setDecorations(boolean decorations) {
        this.decorations = decorations;
        return this;
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

        if (item != null && !item.isEmpty()) g.drawItem(item, ax + (w - 16) / 2, ay + (h - 16) / 2, decorations);
    }

    // Подсказка рисуется поверх всего, где бы ни лежала ячейка / The tooltip is drawn above everything, wherever the cell sits
    @Override
    protected void renderOverlay(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (isHovered() && item != null && !item.isEmpty()) g.drawItemTooltip(item, mouseX, mouseY);
    }
}