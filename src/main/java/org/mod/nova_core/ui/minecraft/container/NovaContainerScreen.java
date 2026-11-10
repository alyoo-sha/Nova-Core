package org.mod.nova_core.ui.minecraft.container;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.menu.NovaMachineMenu;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.SlotFrame;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.minecraft.McItem;
import org.mod.nova_core.ui.minecraft.NovaTexts;
import org.mod.nova_core.ui.minecraft.RecipeBookPanel;
import org.mod.nova_core.ui.minecraft.UiHost;
import org.mod.nova_core.ui.minecraft.container.interaction.InventoryInteractions;
import org.mod.nova_core.ui.minecraft.container.interaction.InventoryMouseEvent;
import org.mod.nova_core.ui.style.Styles;

// Экран контейнера на Nova Core: две отдельные панели вместо ванильной текстуры. Работает с любым меню,
// в том числе ванильным. Слоты, предметы и перенос предметов остаются ванильными
// A container screen on Nova Core: two separate panels instead of the vanilla texture. Works with any menu,
// including vanilla ones. Slots, items and item dragging stay vanilla
public abstract class NovaContainerScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private final UiHost host = new UiHost(this::buildTree, this::initialFocus);
    private final MachineLayout layout;
    private Panel machinePanel = null;
    private Panel inventoryPanel = null;
    // Виджеты вне панелей (например, полоса эффектов под окном) / Widgets outside the panels (e.g. the effects bar below the window)
    protected void buildExtras(Widget root) {}

    protected NovaContainerScreen(M menu, Inventory inventory, Component title, MachineLayout layout) {
        super(menu, inventory, title);
        this.layout = layout;
        this.imageWidth = layout.imageWidth();
        this.imageHeight = layout.imageHeight();
    }

    protected final MachineLayout layout() { return layout; }

    // Наполняет панель механизма; координаты отсчитываются от её левого верхнего угла
    // Fills the machine panel; coordinates are measured from its top-left corner
    protected abstract void buildMachine(Panel machinePanel);

    protected Widget initialFocus() { return null; }

    // Переставляет слоты под раскладку; для ванильных меню, где позиции не наши. Вызывается при каждой сборке
    // Moves the slots to the layout; for vanilla menus whose positions are not ours. Called on every build
    protected void positionSlots(MachineLayout layout) {}

    // Большая рамка для результата / A large frame for a result
    protected boolean isLargeSlot(Slot slot) {
        return slot instanceof ResultSlot || (menu instanceof NovaMachineMenu m && m.isOutputSlot(slot));
    }

    @Override
    protected void init() {
        super.init(); // считает leftPos и topPos / computes leftPos and topPos
        host.init(width, height);
    }

    private void buildTree(Widget root) {
        positionSlots(layout);

        machinePanel = new Panel(leftPos + layout.machineLeft(), topPos,
                layout.machineWidth(), layout.machineHeight(), Styles.POPUP);
        inventoryPanel = new Panel(leftPos + layout.inventoryLeft(), topPos + layout.inventoryTop(),
                MachineLayout.INVENTORY_WIDTH, MachineLayout.INVENTORY_HEIGHT, Styles.POPUP);
        machinePanel.setDebugName("machine");
        inventoryPanel.setDebugName("inventory");

        buildTitle(machinePanel, NovaTexts.titleFor(title));
        inventoryPanel.add(label(NovaTexts.titleFor(playerInventoryTitle), MachineLayout.INVENTORY_WIDTH - 16, 8, 6));
        root.add(machinePanel);
        root.add(inventoryPanel);

        // Рамки рисуются по настоящим слотам меню: расхождений быть не может / Frames follow the real menu slots: they cannot diverge
        for (Slot slot : menu.slots) {
            boolean large = isLargeSlot(slot);
            int offset = large ? 5 : 1;
            SlotFrame frame = new SlotFrame(large);
            frame.setPosition(leftPos + slot.x - offset, topPos + slot.y - offset);
            root.add(frame);
            buildExtras(root);
        }

        buildMachine(machinePanel);
    }

    // Жирная подпись, которая обрезается «…» и раскрывается подсказкой / A bold label that is cut with "…" and revealed by a tooltip
    protected final TextWidget label(UiText text, int maxWidth, int x, int y) {
        TextWidget widget = new TextWidget(x, y, maxWidth, 9, NovaTexts.bold(text));
        widget.setEllipsize(true);
        return widget;
    }

    // Куда ставить заголовок; потомки могут переопределить, например над нужной группой слотов
    // Where to put the title; subclasses can override, e.g. above a particular slot group
    protected void buildTitle(Panel machinePanel, UiText title) {
        machinePanel.add(label(title, layout.machineWidth() - 16, 8, 6));
    }

    private RecipeBookPanel recipeBook = null;

    // Книга рецептов для окон с RecipeBookMenu; вызывайте из buildExtras / A recipe book for windows with a RecipeBookMenu; call it from buildExtras
    protected final void installRecipeBook(Widget root, RecipeBookMenu<?> bookMenu) {
        recipeBook = RecipeBookPanel.install(root, machinePanel, bookMenu, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // Раз в полсекунды: в инвентаре могли появиться нужные предметы / Every half second: the needed items may have appeared in the inventory
        if (recipeBook != null && minecraft != null && minecraft.player != null && minecraft.player.tickCount % 10 == 0) {
            recipeBook.refresh();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        host.renderBackdrop(g);
        super.render(g, mouseX, mouseY, partialTick); // вызывает renderBg, затем рисует предметы / calls renderBg, then draws the items
        host.renderTop(g, mouseX, mouseY, partialTick);
        if (!host.hasModal()) renderTooltip(g, mouseX, mouseY);
    }

    // Содержимое рисуется под предметами в слотах / The content is drawn below the items in slots
    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        host.prepare(mouseX, mouseY);
        host.renderContent(g, mouseX, mouseY, partialTick);
        drawHover(g);
        drawModuleHighlights(g);
    }

    // Ванильная подсветка гасится (если у вашей версии Forge есть getSlotColor), наша рисуется под предметами
    // The vanilla highlight is switched off (if your Forge version has getSlotColor), ours is drawn under the items
    public int getSlotColor(int index) {
        return 0;
    }

    private void drawHover(GuiGraphics g) {
        Slot s = hoveredSlot;
        if (s == null || !s.isActive() || host.hasModal()) return;
        int offset = isLargeSlot(s) ? 5 : 1;
        int size = isLargeSlot(s) ? 26 : 18;
        host.drawSlotHighlight(g, leftPos + s.x - offset, topPos + s.y - offset, size, size);
    }

    // Слоты, которые просят подсветить модули (например, выбранные для деления поровну), цветом темы под предметами
    // Slots that modules ask to highlight (e.g. picked for an even split), in the theme color under the items
    private void drawModuleHighlights(GuiGraphics g) {
        if (host.hasModal()) return;
        InventoryInteractions.collectHighlights(this, id -> {
            if (id < 0 || id >= menu.slots.size()) return;
            Slot s = menu.slots.get(id);
            if (!s.isActive()) return;
            int offset = isLargeSlot(s) ? 5 : 1;
            int size = isLargeSlot(s) ? 26 : 18;
            host.drawSlotHighlight(g, leftPos + s.x - offset, topPos + s.y - offset, size, size);
        });
    }

    // Нужен кнопке рецептов / Needed by the recipe book button
    protected final Panel machinePanel() {
        return machinePanel;
    }

    // Подписи рисуют виджеты / The labels are drawn by widgets
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}

    // Подсказка слота нашей темы вместо ванильной / The slot tooltip in our theme instead of the vanilla one
    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        if (host.hasModal() || !menu.getCarried().isEmpty() || hoveredSlot == null || !hoveredSlot.hasItem()) return;
        host.drawTooltip(g, UiContext.platform().itemTooltip(McItem.of(hoveredSlot.getItem())), x, y);
    }

    // Клик мимо обеих панелей выбрасывает предмет с курсора; промежуток между панелями считается «внутри»
    // A click outside both panels throws the carried item; the gap between the panels counts as "inside"
    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        if (machinePanel == null || inventoryPanel == null) return super.hasClickedOutside(mx, my, left, top, button);
        boolean inPanels = machinePanel.contains(mx, my) || inventoryPanel.contains(mx, my);
        boolean inGap = mx >= left && mx < left + imageWidth && my >= top && my < top + imageHeight;
        return !inPanels && !inGap;
    }

    // Отключение модулей работы с инвентарём для конкретного окна; потомки могут переопределить
    // Opt-out of inventory-interaction modules for a particular window; subclasses may override
    protected boolean allowsInventoryInteractions() {
        return true;
    }

    // Модули работают, только если окно разрешило их и нет ни одного открытого оверлея (попап, модальное окно, подсказка)
    // Modules work only if the window allows them and no overlay is open (popup, modal, tooltip)
    private boolean interactionsReady() {
        return allowsInventoryInteractions() && !host.hasModal() && host.overlays().openOverlays().isEmpty();
    }

    // Слот под точкой экрана или null; та же геометрия и порядок, что у ванильного hit-теста (16x16 плюс рамка в 1 px)
    // The slot at a screen point or null; the same geometry and order as the vanilla hit test (16x16 plus a 1 px border)
    @Nullable
    public final Slot slotAt(double mx, double my) {
        double px = mx - leftPos;
        double py = my - topPos;
        for (Slot s : menu.slots) {
            if (s.isActive() && px >= s.x - 1 && px < s.x + 17 && py >= s.y - 1 && py < s.y + 17) return s;
        }
        return null;
    }

    @Override
    public void removed() {
        host.removed();
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (host.mouseClicked(mx, my, button)) return true;
        if (interactionsReady() && InventoryInteractions.press(this, slotAt(mx, my), InventoryMouseEvent.press(mx, my, button))) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean hostHandled = host.mouseReleased(mx, my, button);
        // Отпускание доходит до модулей всегда, даже если открыт оверлей: жест обязан завершиться
        // The release always reaches the modules, even with an overlay open: the gesture must finish
        boolean intercepted = allowsInventoryInteractions()
                && InventoryInteractions.release(this, slotAt(mx, my), InventoryMouseEvent.release(mx, my, button));
        return hostHandled || intercepted || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (host.mouseDragged(mx, my, button, dx, dy)) return true;
        if (interactionsReady() && InventoryInteractions.drag(this, slotAt(mx, my), InventoryMouseEvent.drag(mx, my, button, dx, dy))) return true;
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        host.mouseMoved(mx, my);
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (host.mouseScrolled(mx, my, delta)) return true;
        if (interactionsReady() && InventoryInteractions.scroll(this, slotAt(mx, my), InventoryMouseEvent.scroll(mx, my, delta))) return true;
        return super.mouseScrolled(mx, my, delta);
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