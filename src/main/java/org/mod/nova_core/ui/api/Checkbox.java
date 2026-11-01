package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.KeyboardEvent;
import org.mod.nova_core.ui.input.Keys;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.Objects;

// Флажок с подписью. Состояние в Signal: можно передать свой, чтобы делить его с другими виджетами
// A checkbox with a label. The state lives in a Signal: pass your own to share it with other widgets
public class Checkbox extends Widget {
    private static final int BOX = 10;
    private static final int GAP = 4;

    private final StyleRef<ButtonStyle> style = new StyleRef<>(Styles.BUTTON);
    private final Signal<Boolean> checked;

    private UiText label;
    private boolean pressedState = false;

    public Checkbox(int x, int y, UiText label) {
        this(x, y, label, Signal.of(false));
    }

    public Checkbox(int x, int y, UiText label, Signal<Boolean> state) {
        super(x, y, 0, 0);
        this.label = label;
        this.checked = Objects.requireNonNull(state);
        setSizeMode(SizeMode.WRAP, SizeMode.WRAP);
    }

    public boolean isChecked() { return checked.peek(); }

    public Checkbox setChecked(boolean value) {
        checked.set(value);
        return this;
    }

    public Signal<Boolean> checkedSignal() { return checked; }

    public UiText getLabel() { return label; }

    public Checkbox setLabel(UiText label) {
        this.label = label;
        requestLayout();
        return this;
    }

    public ButtonStyle getStyle() { return style.get(); }

    public Checkbox setStyle(ButtonStyle value) {
        style.set(value);
        requestLayout();
        return this;
    }

    @Override
    public boolean isFocusable() { return true; }

    // Переключить со звуком / Toggle with a click sound
    public void toggle() {
        if (!isActive()) return;
        UiContext.platform().playClickSound();
        checked.set(!checked.peek());
    }

    @Override
    protected Size measureContent(Constraints c) {
        UiPlatform p = UiContext.platform();
        int labelWidth = p.textWidth(label);
        int w = BOX + (labelWidth > 0 ? GAP + labelWidth : 0);
        return new Size(w, Math.max(BOX, p.lineHeight()));
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        ButtonStyle s = style.get();
        UiPlatform p = UiContext.platform();
        int ax = absX();
        int boxY = absY() + (getHeight() - BOX) / 2;
        boolean hovered = isActive() && isHovered();

        int bg;
        if (!isActive()) bg = s.disabled();
        else if (pressedState && hovered) bg = s.pressed();
        else if (hovered) bg = s.hover();
        else bg = s.normal();
        int markColor = isActive() ? s.text() : s.disabledText();

        g.fill(ax, boxY, ax + BOX, boxY + BOX, isFocused() ? s.focusedBorder() : s.border());
        g.fill(ax + 1, boxY + 1, ax + BOX - 1, boxY + BOX - 1, bg);
        if (checked.peek()) g.fill(ax + 3, boxY + 3, ax + BOX - 3, boxY + BOX - 3, markColor);

        int textY = absY() + (getHeight() - p.lineHeight()) / 2;
        g.drawText(label, ax + BOX + GAP, textY, markColor, s.textShadow());
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
        if (contains(e.getX(), e.getY())) toggle();
        e.stopPropagation();
    }

    @Override
    protected void onKeyDown(KeyboardEvent e) {
        int key = e.getKey();
        if (key == Keys.SPACE || key == Keys.ENTER || key == Keys.KP_ENTER) {
            toggle();
            e.stopPropagation();
        }
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