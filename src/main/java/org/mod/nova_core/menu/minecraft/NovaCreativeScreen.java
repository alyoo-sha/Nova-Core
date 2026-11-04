package org.mod.nova_core.menu.minecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.mod.nova_core.ui.api.ItemCell;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.PlayerPreview;
import org.mod.nova_core.ui.api.SlotFrame;
import org.mod.nova_core.ui.api.TextField;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.api.VirtualGrid;
import org.mod.nova_core.ui.api.VirtualList;
import org.mod.nova_core.ui.collection.ItemAdapter;
import org.mod.nova_core.ui.collection.ListModel;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.graphics.Draw;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.EventType;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.minecraft.McEffectsBar;
import org.mod.nova_core.ui.minecraft.McItem;
import org.mod.nova_core.ui.minecraft.McText;
import org.mod.nova_core.ui.minecraft.UiHost;
import org.mod.nova_core.ui.minecraft.container.SlotAccess;
import org.mod.nova_core.ui.overlay.TooltipBubble;
import org.mod.nova_core.ui.overlay.Tooltips;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.FieldStyle;
import org.mod.nova_core.ui.style.SlotStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;
import org.mod.nova_core.ui.theme.ThemeManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class NovaCreativeScreen extends AbstractContainerScreen<InventoryMenu> {
    // Размеры окна (GUI-пиксели) / Window size (GUI pixels)
    private static final int WIDTH = 336;
    private static final int HEIGHT = 176;
    private static final int TABS_WIDTH = 88;
    private static final int X0 = 8 + TABS_WIDTH + 8; // левый край правой части / left edge of the right part
    private static final int GRID_Y = 26;
    private static final int COLUMNS = 12;
    private static final int HOTBAR_Y = 150;
    private static final int OFFSCREEN = -1000;
    private static final int RECENT_SIZE = 3;

    private static final String[] TIMES = {"day", "noon", "night", "midnight"};
//    private static final String[] TIMES = {"day", "noon", "night", "midnight"};
    private static final String[] WEATHERS = {"clear", "rain", "thunder"};
    private static final GameType[] MODES = {GameType.CREATIVE, GameType.SURVIVAL};
//    private static final GameType[] MODES = {GameType.CREATIVE, GameType.SURVIVAL, GameType.ADVENTURE, GameType.SPECTATOR};
    private static int timeIndex = 0;
    private static int weatherIndex = 0;

    // Живут между открытиями окна / They live between window openings
    private static CreativeModeTab lastTab = null;
    private static final List<ItemStack> RECENT = new ArrayList<>();

    private final UiHost host = new UiHost(this::buildTree, () -> null);
    private final FeatureFlagSet features;
    private final boolean operatorTab;

    // Состояние живёт в экране, а не в buildTree: дерево пересобирается при ресайзе / State lives in the screen, not in buildTree: the tree rebuilds on resize
    private final ListModel<CreativeModeTab> tabs = new ListModel<>();
    private final Signal<Integer> tabIndex = Signal.of(0);
    private final Signal<String> query = Signal.of("");
    private final ListModel<ItemStack> items = new ListModel<>();
    private final Signal<Integer> gridSelection = Signal.of(-1);
    private final Map<Slot, int[]> originals = new HashMap<>();

    private CreativeModeTab current = null;
    private TextWidget titleText = null;
    private VirtualGrid<ItemStack> grid = null;
    private Widget inventoryView = null;
    private final List<ItemCell> recentCells = new ArrayList<>();

    public NovaCreativeScreen(LocalPlayer player, FeatureFlagSet features, boolean operatorTab) {
        super(player.inventoryMenu, player.getInventory(), Component.translatable("itemGroup.search"));
        this.features = features;
        this.operatorTab = operatorTab;
        this.imageWidth = WIDTH;
        this.imageHeight = HEIGHT;
    }

    // ---------- Создание и пересборка / Creation and rebuild ----------

    @Override
    protected void init() {
        if (minecraft == null || minecraft.gameMode == null || !minecraft.gameMode.hasInfiniteItems()) {
            if (minecraft != null) minecraft.setScreen(new InventoryScreen(minecraft.player));
            return;
        }
        super.init();
        CreativeModeTabs.tryRebuildTabContents(features, operatorTab, minecraft.level.registryAccess());

        List<CreativeModeTab> visible = new ArrayList<>();
        for (CreativeModeTab t : CreativeModeTabs.tabs()) {
            if (t.getType() == CreativeModeTab.Type.CATEGORY || t.getType() == CreativeModeTab.Type.INVENTORY) visible.add(t);
        }
        tabs.setAll(visible);
        int start = Math.max(0, lastTab == null ? 0 : visible.indexOf(lastTab));
        tabIndex.set(start);
        current = visible.isEmpty() ? null : visible.get(start);

        host.init(width, height);
    }

    private void buildTree(Widget root) {
        if (originals.isEmpty()) {
            for (Slot s : menu.slots) originals.put(s, new int[] {s.x, s.y});
        }

        Panel panel = new Panel(leftPos, topPos, WIDTH, HEIGHT, Styles.POPUP);
        panel.setDebugName("creative");
        root.add(panel);

        // Вкладки / Tabs
        ItemAdapter<CreativeModeTab> tabAdapter = ItemAdapter.<CreativeModeTab, TabCell>of(
                TabCell::new, (cell, tab, i, sel) -> cell.set(tab, sel));
        panel.add(new VirtualList<>(8, 8, TABS_WIDTH, HEIGHT - 16, 20, tabs, tabAdapter, tabIndex));

        // Заголовок и поиск / Title and search
        titleText = new TextWidget(X0, 10, UiText.literal(""));
        panel.add(titleText);
        TextField search = new TextField(WIDTH - 8 - 92, 5, 92, 16, query);
        search.setPlaceholder(McText.of(Component.translatable("nova_core.creative.search")));
        panel.add(search);

        // Палитра предметов / The item palette
        ItemAdapter<ItemStack> cellAdapter = ItemAdapter.<ItemStack, ItemCell>of(
                () -> new ItemCell().setSlotBackground(true), (cell, stack, i, sel) -> cell.set(McItem.of(stack), false));
        grid = new VirtualGrid<>(X0, GRID_Y, 224, 108, items, cellAdapter, gridSelection);
        grid.setColumns(COLUMNS).setCellSize(18, 18);
        grid.addListener(EventType.POINTER_DOWN, e -> {
            int i = grid.indexAt(e.getX(), e.getY());
            if (i >= 0 && i < items.size()) paletteClick(items.get(i), e.getButton(), Screen.hasShiftDown());
            grid.setSelectedIndex(-1);
        });
        panel.add(grid);

        inventoryView = buildInventoryView();
        panel.add(inventoryView);

        // Хотбар и недавние / Hotbar and recently used
        panel.add(new TextWidget(X0, 139, McText.of(Component.translatable("nova_core.creative.hotbar"))));
        for (int i = 0; i < 9; i++) frame(panel, X0 + i * 18, HOTBAR_Y - 1);
        int recentX = X0 + 9 * 18 + 8;
        panel.add(new TextWidget(recentX, 139, McText.of(Component.translatable("nova_core.creative.recent"))));
        recentCells.clear();
        for (int i = 0; i < RECENT_SIZE; i++) {
            frame(panel, recentX + i * 18, HOTBAR_Y - 1);
            ItemCell cell = new ItemCell();
            cell.setBounds(recentX + i * 18, HOTBAR_Y - 1, 18, 18);
            int index = i;
            cell.addListener(EventType.POINTER_DOWN, e -> {
                if (index < RECENT.size()) paletteClick(RECENT.get(index), e.getButton(), Screen.hasShiftDown());
            });
            recentCells.add(cell);
            panel.add(cell);
        }
        refreshRecent();

        // Кнопки над окном и эффекты под ним / Buttons above the window and effects below it
        root.add(iconButton(leftPos, topPos - 24, Items.CLOCK,
                () -> "nova_core.creative.time", () -> TIMES[timeIndex],
                () -> {
                    sendCommand("time set " + TIMES[timeIndex]);
                    timeIndex = (timeIndex + 1) % TIMES.length;
                }));
        root.add(iconButton(leftPos + 24, topPos - 24, Items.COMMAND_BLOCK,
                () -> "nova_core.creative.gamemode", () -> nextMode().getName(),
                () -> sendCommand("gamemode " + nextMode().getName())));
        root.add(iconButton(leftPos + 48, topPos - 24, Items.WATER_BUCKET,
                () -> "nova_core.creative.weather", () -> WEATHERS[weatherIndex],
                () -> {
                    sendCommand("weather " + WEATHERS[weatherIndex]);
                    weatherIndex = (weatherIndex + 1) % WEATHERS.length;
                }));
        root.add(new McEffectsBar(leftPos, topPos + HEIGHT + 4, WIDTH));

        panel.whileMounted(() -> {
            panel.scope().subscribe(tabIndex, this::onTabIndex);
            panel.scope().subscribe(query, q -> applyView());
        });
        applyView();
    }

    // Вид вкладки «Инвентарь»: броня, предпросмотр, вторая рука, корзина и основной инвентарь
    // The "Inventory" tab view: armor, the preview, the offhand, the trash and the main inventory
    private Widget buildInventoryView() {
        Widget view = new Widget(X0, GRID_Y, 224, 108);
        view.setPointerTransparent(true);
        for (int i = 0; i < 4; i++) frame(view, 7 + i * 18, 7);
        view.add(new PlayerPreview(86, 0, 54, 52).setScale(20));
        frame(view, 147, 7);
        view.add(new TrashSlot(175, 7));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) frame(view, 31 + c * 18, 55 + r * 18);
        }
        return view;
    }

    private static void frame(Widget parent, int x, int y) {
        SlotFrame f = new SlotFrame(false);
        f.setPosition(x, y);
        parent.add(f);
    }

    // ---------- Вкладки, поиск, вид / Tabs, search, view ----------

    private void onTabIndex(Integer index) {
        if (index == null || index < 0 || index >= tabs.size()) return;
        current = tabs.get(index);
        lastTab = current;
        applyView();
    }

    private boolean showInventory() {
        return current != null && current.getType() == CreativeModeTab.Type.INVENTORY && query.peek().isBlank();
    }

    private void applyView() {
        boolean inventory = showInventory();
        if (grid != null) grid.setVisible(!inventory);
        if (inventoryView != null) inventoryView.setVisible(inventory);
        positionSlots(inventory);
        rebuildItems();
        updateTitle(inventory);
    }

    private void rebuildItems() {
        String q = query.peek().trim().toLowerCase(Locale.ROOT);
        List<ItemStack> out = new ArrayList<>();
        if (!q.isEmpty()) {
            for (ItemStack s : CreativeModeTabs.searchTab().getSearchTabDisplayItems()) {
                if (matches(s, q)) out.add(s);
            }
        } else if (current != null && current.getType() == CreativeModeTab.Type.CATEGORY) {
            out.addAll(current.getDisplayItems());
        }
        items.setAll(out);
    }

    // Поиск по названию и по идентификатору; «#» ищет по тегам / Search by name and id; "#" searches by tags
    private static boolean matches(ItemStack stack, String q) {
        if (q.startsWith("#")) {
            String tag = q.substring(1);
            return stack.getTags().anyMatch(t -> t.location().getPath().contains(tag) || t.location().toString().contains(tag));
        }
        return stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)
                || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains(q);
    }

    private void updateTitle(boolean inventory) {
        if (titleText == null) return;
        Component name = !query.peek().isBlank() ? Component.translatable("nova_core.creative.results")
                : current != null ? current.getDisplayName() : Component.empty();
        MutableComponent title = name.copy().withStyle(ChatFormatting.BOLD);
        if (!inventory) title.append(Component.literal("  " + items.size()).withStyle(ChatFormatting.GRAY));
        titleText.setText(McText.of(title));
    }

    // Слоты инвентаря переставляются под вид; ненужные уходят за экран / The inventory slots are moved to the view; unneeded ones go off screen
    private void positionSlots(boolean inventoryView) {
        for (Slot slot : menu.slots) {
            int i = slot.index;
            int x = OFFSCREEN;
            int y = OFFSCREEN;
            if (i >= 36 && i <= 44) {
                x = X0 + 1 + (i - 36) * 18;
                y = HOTBAR_Y;
            } else if (inventoryView) {
                if (i >= 5 && i <= 8) {
                    x = X0 + 8 + (i - 5) * 18;
                    y = GRID_Y + 8;
                } else if (i == 45) {
                    x = X0 + 148;
                    y = GRID_Y + 8;
                } else if (i >= 9 && i <= 35) {
                    x = X0 + 32 + ((i - 9) % 9) * 18;
                    y = GRID_Y + 56 + ((i - 9) / 9) * 18;
                }
            }
            SlotAccess.setPosition(slot, x, y);
        }
    }

    // ---------- Предметы и курсор / Items and the cursor ----------

    private static ItemStack withCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    // Клик по палитре; правила как в ванильном креативе / A palette click; the rules are the vanilla creative ones
    private void paletteClick(ItemStack template, int button, boolean shift) {
        ItemStack carried = menu.getCarried();
        if (shift && button == MouseButton.LEFT) {
            giveToInventory(withCount(template, template.getMaxStackSize()));
            remember(template);
            return;
        }
        if (button == MouseButton.MIDDLE) {
            if (carried.isEmpty()) {
                menu.setCarried(withCount(template, template.getMaxStackSize()));
                remember(template);
            }
            return;
        }
        if (carried.isEmpty()) {
            menu.setCarried(withCount(template, 1));
            remember(template);
        } else if (ItemStack.isSameItemSameTags(carried, template)) {
            if (button == MouseButton.LEFT) {
                if (carried.getCount() < carried.getMaxStackSize()) carried.grow(1);
            } else {
                carried.shrink(1);
            }
        } else if (button == MouseButton.LEFT) {
            menu.setCarried(ItemStack.EMPTY);
        } else {
            carried.shrink(1);
        }
    }

    // Положить в инвентарь и отправить на сервер изменившиеся слоты / Put into the inventory and send the changed slots to the server
    private void giveToInventory(ItemStack stack) {
        Inventory inv = minecraft.player.getInventory();
        ItemStack[] before = new ItemStack[36];
        for (int i = 0; i < 36; i++) before[i] = inv.getItem(i).copy();
        inv.add(stack);
        for (int i = 0; i < 36; i++) {
            ItemStack now = inv.getItem(i);
            if (!ItemStack.matches(before[i], now)) {
                minecraft.gameMode.handleCreativeModeItemAdd(now.copy(), i < 9 ? 36 + i : i);
            }
        }
    }

    private void remember(ItemStack template) {
        ItemStack one = withCount(template, 1);
        RECENT.removeIf(s -> ItemStack.isSameItemSameTags(s, one));
        RECENT.add(0, one);
        while (RECENT.size() > RECENT_SIZE) RECENT.remove(RECENT.size() - 1);
        refreshRecent();
    }

    private void refreshRecent() {
        for (int i = 0; i < recentCells.size(); i++) {
            recentCells.get(i).set(i < RECENT.size() ? McItem.of(RECENT.get(i)) : null, false);
        }
    }

    // Записать стек в слот локально и на сервере (пакет креатива) / Write a stack into a slot locally and on the server (the creative packet)
    private void putSlot(Slot slot, ItemStack stack) {
        ItemStack s = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        slot.set(s);
        minecraft.gameMode.handleCreativeModeItemAdd(s.copy(), slot.index);
    }

    private void slotClick(Slot slot, int button) {
        ItemStack carried = menu.getCarried();
        ItemStack inSlot = slot.getItem();
        if (!carried.isEmpty()) {
            if (!slot.mayPlace(carried)) return;
            if (button == MouseButton.RIGHT) { // положить один / place one
                if (inSlot.isEmpty()) {
                    putSlot(slot, withCount(carried, 1));
                    carried.shrink(1);
                } else if (ItemStack.isSameItemSameTags(inSlot, carried) && inSlot.getCount() < inSlot.getMaxStackSize()) {
                    putSlot(slot, withCount(inSlot, inSlot.getCount() + 1));
                    carried.shrink(1);
                }
            } else if (ItemStack.isSameItemSameTags(inSlot, carried)) { // досыпать / top up
                int move = Math.min(carried.getCount(), inSlot.getMaxStackSize() - inSlot.getCount());
                if (move > 0) {
                    putSlot(slot, withCount(inSlot, inSlot.getCount() + move));
                    carried.shrink(move);
                }
            } else { // обмен / swap
                ItemStack old = inSlot.copy();
                putSlot(slot, carried);
                menu.setCarried(old);
            }
        } else if (!inSlot.isEmpty()) {
            if (button == MouseButton.RIGHT) { // половина / half
                int half = (inSlot.getCount() + 1) / 2;
                menu.setCarried(withCount(inSlot, half));
                putSlot(slot, withCount(inSlot, inSlot.getCount() - half));
            } else {
                menu.setCarried(inSlot.copy());
                putSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private void dropCarried() {
        ItemStack carried = menu.getCarried();
        if (carried.isEmpty()) return;
        minecraft.player.drop(carried, true);
        minecraft.gameMode.handleCreativeModeItemDrop(carried);
        menu.setCarried(ItemStack.EMPTY);
    }

    private void sendCommand(String command) {
        if (minecraft != null && minecraft.player != null) minecraft.player.connection.sendCommand(command);
    }

    private GameType nextMode() {
        GameType now = minecraft.gameMode.getPlayerMode();
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i] == now) return MODES[(i + 1) % MODES.length];
        }
        return GameType.SURVIVAL;
    }

    // ---------- Кадр и ввод / Frame and input ----------

    @Override
    protected void containerTick() {
        super.containerTick();
        // Режим сменили на выживание: открываем обычный инвентарь / The mode changed to survival: open the regular inventory
        if (minecraft != null && minecraft.player != null && minecraft.gameMode != null && !minecraft.gameMode.hasInfiniteItems()) {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
        }
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        host.renderBackdrop(g);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        host.prepare(mouseX, mouseY);
        host.renderContent(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        host.renderBackdrop(g);
        super.render(g, mouseX, mouseY, partialTick);
        host.renderTop(g, mouseX, mouseY, partialTick);
        if (!host.hasModal()) renderTooltip(g, mouseX, mouseY);
    }

    // Клики по слотам обрабатываются здесь, а не ванильной логикой с пакетами контейнера
    // Slot clicks are handled here instead of the vanilla logic with container packets
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {}

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (host.mouseClicked(mx, my, button)) return true;
        if (hoveredSlot != null && hoveredSlot.isActive()) {
            slotClick(hoveredSlot, button);
            return true;
        }
        boolean inside = mx >= leftPos && mx < leftPos + WIDTH && my >= topPos && my < topPos + HEIGHT;
        if (!inside && !menu.getCarried().isEmpty()) {
            dropCarried();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return host.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return host.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return host.mouseScrolled(mx, my, delta);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        host.mouseMoved(mx, my);
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

    @Override
    public void removed() {
        menu.setCarried(ItemStack.EMPTY);
        for (Map.Entry<Slot, int[]> e : originals.entrySet()) {
            SlotAccess.setPosition(e.getKey(), e.getValue()[0], e.getValue()[1]);
        }
        originals.clear();
        host.removed();
        super.removed();
    }

    // ---------- Виджеты окна / The window's widgets ----------

    private IconButton iconButton(int x, int y, net.minecraft.world.item.Item icon,
                                  Supplier<String> titleKey, Supplier<String> value, Runnable action) {
        IconButton b = new IconButton(x, y, new ItemStack(icon), action);
        Tooltips.attach(b, () -> new TooltipBubble(McText.of(Component.translatable(titleKey.get(), value.get()))));
        return b;
    }

    // Ячейка вкладки: значок и название / A tab cell: the icon and the name
    private static final class TabCell extends Widget {
        private final StyleRef<ButtonStyle> buttonStyle = new StyleRef<>(Styles.BUTTON);
        private final StyleRef<FieldStyle> fieldStyle = new StyleRef<>(Styles.FIELD);
        private McItem icon = null;
        private UiText name = UiText.literal("");
        private boolean selected = false;

        TabCell() {
            super(0, 0, 0, 0);
        }

        void set(CreativeModeTab tab, boolean selected) {
            this.icon = McItem.of(tab.getIconItem());
            this.name = McText.of(tab.getDisplayName());
            this.selected = selected;
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            ButtonStyle b = buttonStyle.get();
            int ax = absX();
            int ay = absY();
            int w = getWidth();
            int h = getHeight();
            if (selected) g.fillRounded(ax, ay, ax + w, ay + h, b.radius(), fieldStyle.get().selection());
            else if (isHovered()) g.fillRounded(ax, ay, ax + w, ay + h, b.radius(), b.hover());
            if (icon != null) g.drawItem(icon, ax + 3, ay + 2, false);
            g.pushClip(ax + 22, ay, ax + w - 2, ay + h);
            g.drawText(name, ax + 22, ay + (h - UiContext.platform().lineHeight()) / 2, b.text(), b.textShadow());
            g.popClip();
        }
    }

    // Квадратная кнопка со значком предмета / A square button with an item icon
    private static final class IconButton extends Widget {
        private final StyleRef<ButtonStyle> style = new StyleRef<>(Styles.BUTTON);
        private final McItem icon;
        private final Runnable action;
        private boolean pressed = false;

        IconButton(int x, int y, ItemStack icon, Runnable action) {
            super(x, y, 20, 20);
            this.icon = McItem.of(icon);
            this.action = action;
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            ButtonStyle s = style.get();
            boolean hovered = isHovered();
            int bg = pressed && hovered ? s.pressed() : hovered ? s.hover() : s.normal();
            Draw.box(g, absX(), absY(), getWidth(), getHeight(), s.radius(), bg, s.border(), 1);
            g.drawItem(icon, absX() + 2, absY() + 2, false);
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

    // Корзина: клик с предметом на курсоре удаляет его / The trash: a click with an item on the cursor deletes it
    private final class TrashSlot extends Widget {
        private final StyleRef<SlotStyle> style = new StyleRef<>(Styles.SLOT);

        TrashSlot(int x, int y) {
            super(x, y, 18, 18);
            Tooltips.attach(this, () -> new TooltipBubble(McText.of(Component.translatable("inventory.binSlot"))));
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            SlotStyle s = style.get();
            float in = s.inset();
            g.fillRounded(absX() + in, absY() + in, absX() + 18 - in, absY() + 18 - in, s.radius(), s.background());
            g.strokeRounded(absX() + in, absY() + in, absX() + 18 - in, absY() + 18 - in, s.radius(), 1f, 0xFFB04040);
            g.drawItem(McItem.of(new ItemStack(Items.BARRIER)), absX() + 1, absY() + 1, false);
        }

        @Override
        protected void onPointerDown(PointerEvent e) {
            if (e.getButton() != MouseButton.LEFT) return;
            if (!menu.getCarried().isEmpty()) menu.setCarried(ItemStack.EMPTY);
            e.stopPropagation();
        }
    }
}