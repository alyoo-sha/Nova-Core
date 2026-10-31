package org.mod.nova_core.ui.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;

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

    private Component label;
    private Runnable onClick;
    private boolean playSound = true;

    // Нажата ли кнопка мышью сейчас / Is the mouse button currently held on it
    private boolean pressedState = false;

    public ButtonWidget(int x, int y, int width, int height, Component label, Runnable onClick) {
        super(x, y, width, height);
        this.label = label;
        this.onClick = onClick;
    }

    public Component getLabel() { return label; }
    public ButtonWidget setLabel(Component label) { this.label = label; return this; }
    public ButtonWidget setOnClick(Runnable onClick) { this.onClick = onClick; return this; }
    public ButtonWidget setPlaySound(boolean playSound) { this.playSound = playSound; return this; }

    @Override
    public boolean isFocusable() { return true; }

    // Программное нажатие / Programmatic press
    public void activate() {
        if (!isActive()) return;
        if (playSound) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
        onClick.run();
    }

    @Override
    protected void renderSelf(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
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

        Font font = Minecraft.getInstance().font;
        int tx = ax + (w - font.width(label)) / 2;
        int ty = ay + (h - font.lineHeight) / 2;
        g.drawString(font, label, tx, ty, isActive() ? TEXT_NORMAL : TEXT_DISABLED, true);
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        pressedState = true;
        return true;
    }

    @Override
    public boolean onMouseReleased(double mx, double my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        boolean wasPressed = pressedState;
        pressedState = false;
        // Срабатываем, только если отпустили над кнопкой / Fire only if released over the button
        if (wasPressed && contains(mx, my)) activate();
        return wasPressed;
    }

    @Override
    protected Size measureContent(Constraints c) {
        Font font = Minecraft.getInstance().font;
        return new Size(font.width(label) + 16, 20);
    }

    @Override
    public boolean onKeyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
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