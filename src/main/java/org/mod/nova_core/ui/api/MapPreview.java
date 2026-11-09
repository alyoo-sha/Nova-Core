package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiItem;
import org.mod.nova_core.ui.style.SlotStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.ThemeManager;

import java.util.function.Supplier;

// Предпросмотр карты на тёмной подложке; предмет читается каждый кадр / A map preview on a dark backing; the item is read every frame
public class MapPreview extends Widget {
    private final Supplier<UiItem> source;

    public MapPreview(int x, int y, int size, Supplier<UiItem> source) {
        super(x, y, size, size);
        this.source = source;
        setPointerTransparent(true);
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        SlotStyle s = ThemeManager.current().get(Styles.SLOT);
        g.fillRounded(absX(), absY(), absX() + getWidth(), absY() + getHeight(), 4, s.background());
        UiItem item = source.get();
        if (item != null && !item.isEmpty()) g.drawMapPreview(item, absX() + 2, absY() + 2, getWidth() - 4);
    }
}