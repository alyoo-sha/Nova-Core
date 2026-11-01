package org.mod.nova_core.ui.minecraft.container;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.menu.NovaMachineMenu;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.SlotFrame;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.minecraft.McText;
import org.mod.nova_core.ui.minecraft.UiHost;
import org.mod.nova_core.ui.style.Styles;

// Экран механизма на Nova Core: две отдельные панели вместо ванильной текстуры. Слоты, предметы и перенос предметов
// остаются ванильными, поэтому всё работает так же, как в обычном контейнере. Потомок наполняет панель механизма
// A machine screen on Nova Core: two separate panels instead of the vanilla texture. Slots, items and item dragging
// stay vanilla, so everything works like in a regular container. A subclass fills the machine panel
public abstract class NovaContainerScreen<M extends NovaMachineMenu> extends AbstractContainerScreen<M> {
    private final UiHost host = new UiHost(this::buildTree, this::initialFocus);
    private Panel machinePanel = null;
    private Panel inventoryPanel = null;

    protected NovaContainerScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = menu.layout().imageWidth();
        this.imageHeight = menu.layout().imageHeight();
    }

    // Наполняет панель механизма; координаты отсчитываются от её левого верхнего угла
    // Fills the machine panel; coordinates are measured from its top-left corner
    protected abstract void buildMachine(Panel machinePanel);

    protected Widget initialFocus() { return null; }

    // Большая рамка для результата / A large frame for a result
    protected boolean isLargeSlot(Slot slot) {
        return slot instanceof ResultSlot;
    }

    @Override
    protected void init() {
        super.init(); // считает leftPos и topPos / computes leftPos and topPos
        host.init(width, height);
    }

    private void buildTree(Widget root) {
        MachineLayout layout = menu.layout();
        machinePanel = new Panel(leftPos + layout.machineLeft(), topPos,
                layout.machineWidth(), layout.machineHeight(), Styles.POPUP);
        inventoryPanel = new Panel(leftPos + layout.inventoryLeft(), topPos + layout.inventoryTop(),
                MachineLayout.INVENTORY_WIDTH, MachineLayout.INVENTORY_HEIGHT, Styles.POPUP);
        machinePanel.setDebugName("machine");
        inventoryPanel.setDebugName("inventory");

        machinePanel.add(new TextWidget(8, 6, McText.of(title.copy().withStyle(ChatFormatting.BOLD))));
        inventoryPanel.add(new TextWidget(8, 6, McText.of(playerInventoryTitle.copy().withStyle(ChatFormatting.BOLD))));
        root.add(machinePanel);
        root.add(inventoryPanel);

        // Рамки рисуются по настоящим слотам меню: расхождений быть не может / Frames follow the real menu slots: they cannot diverge
        for (Slot slot : menu.slots) {
            boolean large = isLargeSlot(slot);
            int offset = large ? 5 : 1;
            SlotFrame frame = new SlotFrame(large);
            frame.setPosition(leftPos + slot.x - offset, topPos + slot.y - offset);
            root.add(frame);
        }

        buildMachine(machinePanel);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick); // вызывает renderBg, затем рисует предметы / calls renderBg, then draws the items
        host.renderTop(g, mouseX, mouseY, partialTick);
        if (!host.hasModal()) renderTooltip(g, mouseX, mouseY);
    }

    // Содержимое рисуется под предметами в слотах / The content is drawn below the items in slots
    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        host.prepare(mouseX, mouseY);
        host.renderContent(g, mouseX, mouseY, partialTick);
    }

    // Подписи рисуют виджеты / The labels are drawn by widgets
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}

    // Клик мимо обеих панелей выбрасывает предмет с курсора; промежуток между панелями считается «внутри»
    // A click outside both panels throws the carried item; the gap between the panels counts as "inside"
    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        if (machinePanel == null || inventoryPanel == null) return super.hasClickedOutside(mx, my, left, top, button);
        boolean inGap = mx >= left && mx < left + imageWidth && my >= top && my < top + imageHeight
                && !machinePanel.contains(mx, my) && !inventoryPanel.contains(mx, my);
        return !machinePanel.contains(mx, my) && !inventoryPanel.contains(mx, my) && !inGap;
    }

    @Override
    public void removed() {
        host.removed();
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return host.mouseClicked(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return host.mouseReleased(mx, my, button) || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return host.mouseDragged(mx, my, button, dx, dy) || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        host.mouseMoved(mx, my);
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return host.mouseScrolled(mx, my, delta) || super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        return host.keyPressed(key, scanCode, modifiers) || super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        return host.keyReleased(key, scanCode, modifiers) || super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return host.charTyped(c, modifiers) || super.charTyped(c, modifiers);
    }
}