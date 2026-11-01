package org.mod.nova_core.ui.api;


import org.mod.nova_core.ui.animation.AnimatedValue;
import org.mod.nova_core.ui.animation.Easing;
import org.mod.nova_core.ui.animation.Interpolators;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.debug.WidgetInfo;
import org.mod.nova_core.ui.graphics.Draw;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.KeyboardEvent;
import org.mod.nova_core.ui.input.Keys;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.List;

// Кнопка: срабатывает при отпускании ЛКМ над ней, а также по Enter/Space в фокусе
// Button: fires when LMB is released over it, and on Enter/Space while focused
public class ButtonWidget extends Widget {
    private final StyleRef<ButtonStyle> style = new StyleRef<>(Styles.BUTTON);
    // Плавный цвет фона; начальное значение ставится на первом кадре, чтобы не было перехода «из ниоткуда»
    // Smooth background color; the initial value is set on the first frame to avoid a transition "from nowhere"
    private final AnimatedValue<Integer> bg = new AnimatedValue<>(Interpolators.COLOR, 0, 100L, Easing.EASE_OUT);
    private boolean bgInitialized = false;

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

    public ButtonStyle getStyle() { return style.get(); }

    // Свой стиль для этой кнопки; null = брать из темы / Own style for this button; null = use the theme
    public ButtonWidget setStyle(ButtonStyle value) {
        style.set(value);
        requestLayout(); // отступы и высота влияют на размер / padding and height affect the size
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
    protected void onUnmount() {
        bg.finish(); // не оставлять цвет «на полпути» при повторном монтировании / do not leave the color mid-way on a remount
    }

    @Override
    protected Size measureContent(Constraints c) {
        ButtonStyle s = style.get();
        return new Size(UiContext.platform().textWidth(label) + s.paddingX() * 2, s.height());
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        ButtonStyle s = style.get();
        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();
        boolean hovered = isActive() && isHovered();

        int targetBg;
        if (!isActive()) targetBg = s.disabled();
        else if (pressedState && hovered) targetBg = s.pressed();
        else if (hovered) targetBg = s.hover();
        else targetBg = s.normal();

        if (bgInitialized) {
            bg.setTarget(targetBg);
        } else {
            bg.jumpTo(targetBg);
            bgInitialized = true;
        }

        int border = isFocused() ? s.focusedBorder() : s.border();

        Draw.box(g, ax, ay, w, h, s.radius(), bg.peek(), border, 1);

        UiPlatform p = UiContext.platform();
        int tx = ax + (w - p.textWidth(label)) / 2;
        int ty = ay + (h - p.lineHeight()) / 2;
        g.drawText(label, tx, ty, isActive() ? s.text() : s.disabledText(), s.textShadow());
    }

    @Override
    protected void onPointerDown(PointerEvent e) {
        if (e.getButton() != MouseButton.LEFT) return;
        pressedState = true;
        e.stopPropagation();
    }

    @Override
    protected void onPointerUp(PointerEvent e) {
        if (e.getButton() != MouseButton.LEFT || !pressedState) return;
        pressedState = false;
        // Срабатываем, только если отпустили над кнопкой / Fire only if released over the button
        if (contains(e.getX(), e.getY())) activate();
        e.stopPropagation();
    }

    @Override
    protected void onKeyDown(KeyboardEvent e) {
        int key = e.getKey();
        if (key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) {
            activate();
            e.stopPropagation();
        }
    }

    @Override
    public void describe(List<String> lines) {
        lines.add("label \"" + WidgetInfo.shorten(label.plain()) + "\"" + (pressedState ? "  pressed" : ""));
    }

    @Override
    protected void onPointerCancel(PointerEvent e) {
        pressedState = false;
    }

    @Override
    public void onFocusChanged(boolean focused) {
        if (!focused) pressedState = false;
    }
}