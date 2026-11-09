package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.Draw;
import org.mod.nova_core.ui.graphics.Icons;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.function.Supplier;

// Квадратная кнопка с иконкой; иконка берётся каждый кадр, поэтому может менять вид по состоянию
// A square button with an icon; the icon is read every frame, so it can change with the state
public class IconButton extends Widget {
    private final StyleRef<ButtonStyle> style = new StyleRef<>(Styles.BUTTON);
    private final Supplier<Icons> icon;
    private final Runnable action;
    private boolean pressed = false;

    public IconButton(int x, int y, Supplier<Icons> icon, Runnable action) {
        super(x, y, 20, 20);
        this.icon = icon;
        this.action = action;
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        ButtonStyle s = style.get();
        boolean hovered = isHovered();
        int bg = pressed && hovered ? s.pressed() : hovered ? s.hover() : s.normal();
        Draw.box(g, absX(), absY(), getWidth(), getHeight(), s.radius(), bg, s.border(), 1);
        icon.get().drawShadowed(g, absX() + (getWidth() - Icons.SIZE) / 2, absY() + (getHeight() - Icons.SIZE) / 2,
                hovered ? 0xFFFFFFFF : 0xFFE0E0E0);
    }

    @Override
    protected void onPointerDown(PointerEvent e) {
        if (e.getButton() != MouseButton.LEFT) return;
        pressed = true;
        e.stopPropagation();
    }

    @Override
    protected void onPointerUp(PointerEvent e) {
        if (e.getButton() != MouseButton.LEFT || !pressed) return;
        pressed = false;
        if (contains(e.getX(), e.getY())) action.run();
        e.stopPropagation();
    }

    @Override
    protected void onPointerCancel(PointerEvent e) {
        pressed = false;
    }
}