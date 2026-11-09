package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.Draw;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.style.TooltipStyle;
import org.mod.nova_core.ui.theme.ThemeManager;

import java.util.List;

// Рисует подсказку темы в точке сразу, без виджета: для слотов ванильных меню
// Draws a themed tooltip at a point right away, with no widget: for vanilla menu slots
public final class TooltipPainter {
    private static final int MAX_WIDTH = 260;

    private TooltipPainter() {}

    public static void draw(UiGraphics g, UiText text, int mouseX, int mouseY, int screenW, int screenH) {
        TooltipStyle s = ThemeManager.current().get(Styles.TOOLTIP);
        UiPlatform p = UiContext.platform();
        List<UiText> lines = p.wrap(text, MAX_WIDTH - s.paddingX() * 2);
        int textW = 0;
        for (UiText line : lines) textW = Math.max(textW, p.textWidth(line));
        int w = textW + s.paddingX() * 2;
        int h = lines.size() * (p.lineHeight() + 1) - 1 + s.paddingY() * 2;

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + w > screenW - 4) x = mouseX - 12 - w;
        x = Math.max(4, x);
        y = Math.max(4, Math.min(y, screenH - 4 - h));

        Draw.box(g, x, y, w, h, s.radius(), s.background(), s.border(), 1);
        int ty = y + s.paddingY();
        for (UiText line : lines) {
            g.drawText(line, x + s.paddingX(), ty, s.text(), s.textShadow());
            ty += p.lineHeight() + 1;
        }
    }
}