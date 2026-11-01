package org.mod.nova_core.ui.dnd;

import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.overlay.TooltipBubble;

// Готовые «призраки» / Ready-made ghosts
public final class DragGhosts {
    private DragGhosts() {}

    // Облачко с текстом / A bubble with text
    public static Widget text(UiText text) {
        return new TooltipBubble(text);
    }

    // Цветной прямоугольник с белой рамкой / A colored rectangle with a white border
    public static Widget box(int width, int height, int argb) {
        Panel panel = new Panel(0, 0, width, height);
        panel.setBackgroundColor(argb);
        panel.setBorderColor(0xFFFFFFFF);
        return panel;
    }
}