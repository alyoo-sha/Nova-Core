package org.mod.nova_core.ui.minecraft;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.layout.Constraints;

public abstract class UIScreen extends Screen {
    // Корень дерева, создаётся сразу / Tree root, created eagerly
    protected final Widget root = new Widget(0, 0, 0, 0);

    // Виджет, получивший нажатие: ему идут drag/release / Widget that took the press: gets drag/release
    private Widget pressed = null;

    protected UIScreen(Component title) {
        super(title);
    }

    // Строит дерево виджетов; вызывается при открытии и ресайзе / Builds the widget tree; called on open and resize
    protected abstract void build(Widget root);

    @Override
    protected void init() {
        root.clearChildren(); // заодно сбрасывает фокус / also drops focus
        root.setBounds(0, 0, width, height);
        pressed = null;
        build(root);
        root.measure(Constraints.tight(width, height));
        root.layout();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        root.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        Widget h = root.dispatchMouseClicked(mx, my, button);
        pressed = h;
        if (h != null && h.isFocusable()) h.requestFocus();
        else root.clearFocus();
        return h != null;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        Widget p = pressed;
        pressed = null;
        return p != null && p.onMouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return pressed != null && pressed.onMouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return root.dispatchMouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        Widget f = root.getFocusOwner();
        if (f != null && f.onKeyPressed(key, scanCode, modifiers)) return true;
        return super.keyPressed(key, scanCode, modifiers); // ESC закрывает экран / ESC closes the screen
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        Widget f = root.getFocusOwner();
        if (f != null && f.onKeyReleased(key, scanCode, modifiers)) return true;
        return super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        Widget f = root.getFocusOwner();
        return f != null && f.onCharTyped(c, modifiers);
    }
}