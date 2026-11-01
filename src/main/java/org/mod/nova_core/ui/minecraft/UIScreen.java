package org.mod.nova_core.ui.minecraft;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.overlay.OverlayHost;

public abstract class UIScreen extends Screen {
    private final UiHost host = new UiHost(this::build, this::initialFocus);
    // Корень содержимого экрана; build() наполняет его / Root of the screen content; build() fills it
    protected final Widget root = host.root();

    protected UIScreen(Component title) {
        super(title);
    }

    // Слой оверлеев этого экрана; из любого виджета: OverlayHost.of(widget) / This screen's overlay layer; from any widget: OverlayHost.of(widget)
    protected final OverlayHost overlays() { return host.overlays(); }

    // Строит дерево виджетов; вызывается при открытии и ресайзе / Builds the widget tree; called on open and resize
    protected abstract void build(Widget root);

    // Виджет, который получит фокус сразу после построения; null = без фокуса / The widget that gets focus right after the build; null = no focus
    protected Widget initialFocus() { return null; }

    @Override
    protected void init() {
        host.init(width, height);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        host.prepare(mouseX, mouseY);
        host.renderContent(g, mouseX, mouseY, partialTick);
        host.renderTop(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        host.removed();
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) { return host.mouseClicked(mx, my, button); }

    @Override
    public boolean mouseReleased(double mx, double my, int button) { return host.mouseReleased(mx, my, button); }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return host.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public void mouseMoved(double mx, double my) { host.mouseMoved(mx, my); }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) { return host.mouseScrolled(mx, my, delta); }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (host.keyPressed(key, scanCode, modifiers)) return true;
        return super.keyPressed(key, scanCode, modifiers); // ESC закрывает экран / ESC closes the screen
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        return host.keyReleased(key, scanCode, modifiers) || super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) { return host.charTyped(c, modifiers); }
}