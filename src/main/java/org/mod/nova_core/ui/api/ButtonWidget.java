package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.Keys;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.layout.SizeMode;

// Кнопка: срабатывает при отпускании ЛКМ над ней, а также по Enter/Space в фокусе
// Button: fires when LMB is released over it, and on Enter/Space while focused
public class ButtonWidget extends Widget {
    private static final int COLOR_NORMAL = 0xFF303030;
    private static final int COLOR_HOVER = 0xFF454545;
    private static final int COLOR_PRESSED = 0xFF202020;
    private static final int COLOR_DISABLED = 0xFF1A1A1A;
    private static final int BORDER_NORMAL = 0xFF606060;
    private static final int BORDER_FOCUSED = 0xFFFFFFFF;
    private static final int TEXT_NORMAL = 0xFFE0E0E0;
    private static final int TEXT_DISABLED = 0xFF808080;

    private UiText label;
    private Runnable onClick;
    private boolean playSound = true;

    // Нажата ли кнопка мышью сейчас / Is the mouse button currently held on it
    private boolean pressedState = false;

    public ButtonWidget(int x, int y, int width, int height, UiText label, Runnable onClick) {
        super(x, y, width, height);
        this.label = label;
        this.onClick = onClick;
    }

    public UiText getLabel() { return label; }

    public ButtonWidget setLabel(UiText label) {
        this.label = label;
        if (getWidthMode() == SizeMode.WRAP) requestLayout();
        return this;
    }

    public ButtonWidget setOnClick(Runnable onClick) { this.onClick = onClick; return this; }
    public ButtonWidget setPlaySound(boolean playSound) { this.playSound = playSound; return this; }

    @Override
    public boolean isFocusable() { return true; }

    // Программное нажатие / Programmatic press
    public void activate() {
        if (!isActive()) return;
        if (playSound) UiContext.platform().playClickSound();
        onClick.run();
    }

    @Override
    protected Size measureContent(Constraints c) {
        return new Size(UiContext.platform().textWidth(label) + 16, 20);
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();
        boolean hovered = isActive() && contains(mouseX, mouseY);

        int bg;
        if (!isActive()) bg = COLOR_DISABLED;
        else if (pressedState && hovered) bg = COLOR_PRESSED;
        else if (hovered) bg = COLOR_HOVER;
        else bg = COLOR_NORMAL;

        int border = isFocused() ? BORDER_FOCUSED : BORDER_NORMAL;

        g.fill(ax, ay, ax + w, ay + h, border);
        g.fill(ax + 1, ay + 1, ax + w - 1, ay + h - 1, bg);

        UiPlatform p = UiContext.platform();
        int tx = ax + (w - p.textWidth(label)) / 2;
        int ty = ay + (h - p.lineHeight()) / 2;
        g.drawText(label, tx, ty, isActive() ? TEXT_NORMAL : TEXT_DISABLED, true);
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        if (button != MouseButton.LEFT) return false;
        pressedState = true;
        return true;
    }

    @Override
    public boolean onMouseReleased(double mx, double my, int button) {
        if (button != MouseButton.LEFT) return false;
        boolean wasPressed = pressedState;
        pressedState = false;
        // Срабатываем, только если отпустили над кнопкой / Fire only if released over the button
        if (wasPressed && contains(mx, my)) activate();
        return wasPressed;
    }

    @Override
    public boolean onKeyPressed(int key, int scanCode, int modifiers) {
        if (key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) {
            activate();
            return true;
        }
        return false;
    }

    @Override
    public void onFocusChanged(boolean focused) {
        if (!focused) pressedState = false;
    }
}