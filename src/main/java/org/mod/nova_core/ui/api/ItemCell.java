package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiItem;
import org.mod.nova_core.ui.overlay.TooltipBubble;
import org.mod.nova_core.ui.overlay.Tooltips;
import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.FieldStyle;
import org.mod.nova_core.ui.style.SlotStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;
import org.mod.nova_core.ui.theme.ThemeManager;

// Ячейка с предметом по центру: подсветка, фон слота, подсказка предмета нашей темы. Размер задаёт список или сетка
// A cell with an item in the middle: highlight, a slot background, the item tooltip in our theme. The size is set by the list or grid
public class ItemCell extends Widget {
    private final StyleRef<ButtonStyle> buttonStyle = new StyleRef<>(Styles.BUTTON);
    private final StyleRef<FieldStyle> fieldStyle = new StyleRef<>(Styles.FIELD);

    private UiItem item = null;
    private boolean selected = false;
    private boolean decorations = true;
    private boolean slotBackground = false;

    public ItemCell() {
        super(0, 0, 0, 0);
        // Ячейки переиспользуются, поэтому подсказка читает текущий предмет в момент показа
        // Cells are reused, so the tooltip reads the current item at show time
        Tooltips.attach(this, this::tooltip, 0L);
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

    // Тёмный скруглённый фон, как у слота инвентаря / A dark rounded background like an inventory slot's
    public ItemCell setSlotBackground(boolean value) {
        this.slotBackground = value;
        return this;
    }

    private TooltipBubble tooltip() {
        if (item == null || item.isEmpty()) return null;
        return new TooltipBubble(UiContext.platform().itemTooltip(item)).setMaxWidth(300);
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        ButtonStyle b = buttonStyle.get();
        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();

        if (slotBackground) {
            SlotStyle ss = ThemeManager.current().get(Styles.SLOT);
            float in = ss.inset();
            g.fillRounded(ax + in, ay + in, ax + w - in, ay + h - in, ss.radius(), ss.background());
        }
        if (selected) g.fillRounded(ax, ay, ax + w, ay + h, b.radius(), fieldStyle.get().selection());
        else if (isHovered()) g.fillRounded(ax, ay, ax + w, ay + h, b.radius(),
                slotBackground ? ThemeManager.current().get(Styles.SLOT).highlight() : b.hover());

        if (item != null && !item.isEmpty()) g.drawItem(item, ax + (w - 16) / 2, ay + (h - 16) / 2, decorations);
    }
}