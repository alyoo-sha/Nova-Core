package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.style.SlotStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

// Рамка слота инвентаря: только рисует, клики пропускает сквозь себя к слоту под ней
// An inventory slot frame: draws only, lets clicks pass through to the slot below
public class SlotFrame extends Widget {
    public static final int SIZE = 18;
    public static final int LARGE_SIZE = 26;

    private final StyleRef<SlotStyle> style = new StyleRef<>(Styles.SLOT);
    private final boolean output;

    // output = большая рамка для результата / output = a large frame for a result
    public SlotFrame(boolean output) {
        super(0, 0, output ? LARGE_SIZE : SIZE, output ? LARGE_SIZE : SIZE);
        this.output = output;
        setPointerTransparent(true);
    }

    public boolean isOutput() { return output; }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        SlotStyle s = style.get();
        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();
        int edge = output ? s.outputBorder() : s.border();
        boolean hasEdge = (edge >>> 24) != 0;

        if (s.radius() > 0) {
            float i = s.inset();
            float radius = output ? s.radius() + 1 : s.radius();
            g.fillRounded(ax + i, ay + i, ax + w - i, ay + h - i, radius, s.background());
            if (hasEdge) g.strokeRounded(ax + i, ay + i, ax + w - i, ay + h - i, radius, 1f, edge);
            return;
        }

        // Прямые углы, как раньше / Square corners, as before
        if (hasEdge) {
            g.fill(ax, ay, ax + w, ay + h, edge);
            g.fill(ax + 1, ay + 1, ax + w - 1, ay + h - 1, s.background());
        } else {
            g.fill(ax, ay, ax + w, ay + h, s.background());
        }
    }
}