package org.mod.nova_core.ui.minecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.Checkbox;
import org.mod.nova_core.ui.api.GridPane;
import org.mod.nova_core.ui.api.ItemCell;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextField;
import org.mod.nova_core.ui.api.VirtualGrid;
import org.mod.nova_core.ui.collection.ItemAdapter;
import org.mod.nova_core.ui.collection.ListModel;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.EventType;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.overlay.Overlay;
import org.mod.nova_core.ui.overlay.OverlayHost;
import org.mod.nova_core.ui.overlay.OverlayOptions;
import org.mod.nova_core.ui.overlay.Placement;
import org.mod.nova_core.ui.overlay.TooltipBubble;
import org.mod.nova_core.ui.overlay.Tooltips;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.style.SlotStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.ThemeManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Книга рецептов в теме Nova Core для окон с RecipeBookMenu (верстак, печи, инвентарь): вкладки категорий, поиск,
// фильтр «можно скрафтить», варианты рецептов сгруппированы в одну ячейку, клик расставляет ингредиенты.
// Что можно скрафтить, считается здесь, а не берётся из ванильных флагов
// A recipe book in the Nova Core theme for windows with a RecipeBookMenu (crafting, furnaces, inventory): category tabs,
// search, the "craftable" filter, recipe variants grouped into one cell, a click places the ingredients.
// What can be crafted is computed here instead of taking the vanilla flags
public final class RecipeBookPanel extends Panel {
    private static final int WIDTH = 164;
    private static final int STATE_CRAFTABLE = 0;
    private static final int STATE_MISSING = 1;
    private static final int STATE_NO_FIT = 2;
    private static final int STATE_LOCKED = 3;

    // Запоминается между окнами / Remembered between windows
    private static boolean open = false;
    private static boolean showAllSetting = true;

    private final RecipeBookMenu<?> menu;
    private final List<RecipeBookCategories> categories;
    private final List<ItemStack> categoryIcons = new ArrayList<>();
    private final List<ItemCell> tabCells = new ArrayList<>();
    private final ListModel<RecipeCollection> collections = new ListModel<>();
    private final Signal<String> query = Signal.of("");
    private final Signal<Boolean> craftableOnly = Signal.of(false);
    private final Signal<Boolean> showAll = Signal.of(showAllSetting);
    private final Signal<Integer> category = Signal.of(0);

    // Результаты последнего пересчёта / The results of the last recomputation
    private final Map<RecipeCollection, Integer> states = new IdentityHashMap<>();
    private final Map<RecipeCollection, List<Recipe<?>>> shownRecipes = new IdentityHashMap<>();
    private final Set<Recipe<?>> craftable = Collections.newSetFromMap(new IdentityHashMap<>());

    private RecipeBookPanel(RecipeBookMenu<?> menu, int x, int y, int height) {
        super(x, y, WIDTH, height, Styles.POPUP);
        this.menu = menu;
        this.categories = RecipeBookCategories.getCategories(menu.getRecipeBookType());

        for (int i = 0; i < categories.size(); i++) {
            List<ItemStack> icons = categories.get(i).getIconItems();
            categoryIcons.add(icons.isEmpty() ? ItemStack.EMPTY : icons.get(0));
            ItemCell tab = new ItemCell().setSlotBackground(true);
            tab.setBounds(8 + i * 22, 6, 20, 20);
            int index = i;
            tab.addListener(EventType.POINTER_DOWN, e -> category.set(index));
            tabCells.add(tab);
            add(tab);
        }

        TextField search = new TextField(8, 32, WIDTH - 16, 16, query);
        search.setPlaceholder(NovaTexts.SEARCH);
        add(search);
        add(new Checkbox(8, 52, NovaTexts.CRAFTABLE, craftableOnly));
        add(new Checkbox(8, 68, NovaTexts.RECIPES_ALL, showAll));

        ItemAdapter<RecipeCollection> adapter = ItemAdapter.<RecipeCollection, RecipeCell>of(
                RecipeCell::new, (cell, c, i, sel) -> cell.set(c));
        VirtualGrid<RecipeCollection> grid = new VirtualGrid<>(8, 84, WIDTH - 16, height - 92, collections, adapter);
        grid.setCellSize(20, 20);
        add(grid);

        whileMounted(() -> {
            scope().subscribe(query, q -> refresh());
            scope().subscribe(craftableOnly, c -> refresh());
            scope().subscribe(category, c -> refresh());
            scope().subscribe(showAll, v -> {
                showAllSetting = v;
                refresh();
            });
        });
    }

    // Панель сбоку от окна и кнопка в панели механизма; прежняя книга в этом окне заменяется
    // The panel beside the window and the button in the machine panel; a previous book in this window is replaced
    public static RecipeBookPanel install(Widget root, Panel machinePanel, RecipeBookMenu<?> menu,
                                          int leftPos, int topPos, int windowWidth, int height) {
        for (Widget child : new ArrayList<>(root.getChildren())) {
            if (child instanceof RecipeBookPanel) root.remove(child);
        }
        int x = leftPos - WIDTH - 6;
        if (x < 4) x = leftPos + windowWidth + 6;
        RecipeBookPanel panel = new RecipeBookPanel(menu, x, topPos, height);
        panel.setVisible(open);
        root.add(panel);
        machinePanel.add(new ButtonWidget(machinePanel.getWidth() - 46, 2, 38, 14, NovaTexts.RECIPES_SHORT, () -> {
            open = !open;
            panel.setVisible(open);
            if (open) panel.refresh();
        }));
        panel.refresh();
        return panel;
    }

    private static ItemStack resultOf(Recipe<?> recipe) {
        return recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
    }

    // У рецепта есть что расставлять и что получать / The recipe has something to place and something to produce
    private static boolean isReal(Recipe<?> recipe) {
        if (resultOf(recipe).isEmpty()) return false;
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (!ingredient.isEmpty()) return true;
        }
        return false;
    }

    // Пересчёт списка и состояний. Сначала то, что можно скрафтить из текущего инвентаря
    // Recomputes the list and the states. First what can be crafted from the current inventory
    public void refresh() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || categories.isEmpty()) return;
        ClientRecipeBook book = player.getRecipeBook();
        boolean creative = player.isCreative();
        StackedContents contents = new StackedContents();
        player.getInventory().fillStackedContents(contents);
        menu.fillCraftSlotsStackedContents(contents);

        int index = Math.max(0, Math.min(category.peek(), categories.size() - 1));
        String q = query.peek().trim().toLowerCase(Locale.ROOT);
        states.clear();
        shownRecipes.clear();
        craftable.clear();

        List<RecipeCollection> ready = new ArrayList<>();
        List<RecipeCollection> other = new ArrayList<>();
        for (RecipeCollection c : book.getCollection(categories.get(index))) {
            List<Recipe<?>> all = new ArrayList<>();
            List<Recipe<?>> doable = new ArrayList<>();
            boolean known = false;
            boolean fits = false;
            for (Recipe<?> r : c.getRecipes()) {
                if (!isReal(r)) continue;
                boolean isKnown = creative || book.contains(r);
                boolean fit = isKnown && r.canCraftInDimensions(menu.getGridWidth(), menu.getGridHeight());
                known |= isKnown;
                fits |= fit;
                if (fit && contents.canCraft(r, null)) {
                    craftable.add(r);
                    doable.add(r);
                }
                all.add(r);
            }
            if (all.isEmpty()) continue;

            int state = !known ? STATE_LOCKED : !fits ? STATE_NO_FIT : doable.isEmpty() ? STATE_MISSING : STATE_CRAFTABLE;
            if (state >= STATE_NO_FIT && !showAll.peek()) continue;
            if (craftableOnly.peek() && state != STATE_CRAFTABLE) continue;
            if (!q.isEmpty() && !matches(all, q)) continue;

            states.put(c, state);
            shownRecipes.put(c, craftableOnly.peek() ? doable : all);
            (state == STATE_CRAFTABLE ? ready : other).add(c);
        }
        ready.addAll(other);
        collections.setAll(ready);

        for (int i = 0; i < tabCells.size(); i++) tabCells.get(i).set(McItem.of(categoryIcons.get(i)), i == index);
    }

    private static boolean matches(List<Recipe<?>> recipes, String q) {
        for (Recipe<?> r : recipes) {
            if (resultOf(r).getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    // Автоматическая расстановка ингредиентов; Shift = максимум / Automatic ingredient placement; Shift = the maximum
    private void place(Recipe<?> recipe) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.player == null) return;
        mc.gameMode.handlePlaceRecipe(mc.player.containerMenu.containerId, recipe, Screen.hasShiftDown());
    }

    // Выбор варианта, когда в ячейке несколько рецептов / Choosing a variant when a cell holds several recipes
    private void openVariants(Widget anchor, List<Recipe<?>> recipes) {
        GridPane variants = new GridPane(Math.min(4, recipes.size()));
        variants.setGaps(2, 2).setPadding(Insets.all(3));
        Overlay[] ref = new Overlay[1];
        for (Recipe<?> recipe : recipes) {
            ItemCell cell = new ItemCell().setSlotBackground(true);
            cell.setSize(20, 20);
            cell.set(McItem.of(resultOf(recipe)), false);
            cell.addListener(EventType.POINTER_DOWN, e -> {
                place(recipe);
                if (ref[0] != null) ref[0].close();
                e.stopPropagation();
            });
            variants.add(cell);
        }
        Panel popup = new Panel(0, 0, 0, 0, Styles.POPUP);
        popup.setSizeMode(SizeMode.WRAP, SizeMode.WRAP);
        popup.add(variants);
        ref[0] = OverlayHost.of(anchor).show(popup, OverlayOptions.popup(Placement.anchored(anchor, Placement.Side.BELOW)));
    }

    // Ячейка группы рецептов: показывает результаты вариантов по очереди, как ванильная
    // A recipe group cell: shows the variants' results in turn, like the vanilla one
    private final class RecipeCell extends Widget {
        private RecipeCollection collection = null;

        RecipeCell() {
            super(0, 0, 0, 0);
            Tooltips.attach(this, this::tooltip, 0L);
        }

        void set(RecipeCollection c) {
            this.collection = c;
        }

        private int state() {
            return collection == null ? STATE_LOCKED : states.getOrDefault(collection, STATE_LOCKED);
        }

        private List<Recipe<?>> shown() {
            List<Recipe<?>> list = collection == null ? null : shownRecipes.get(collection);
            return list == null ? List.of() : list;
        }

        private ItemStack displayed() {
            List<Recipe<?>> list = shown();
            if (list.isEmpty()) return ItemStack.EMPTY;
            LocalPlayer player = Minecraft.getInstance().player;
            int tick = player == null ? 0 : player.tickCount;
            return resultOf(list.get((tick / 30) % list.size()));
        }

        private TooltipBubble tooltip() {
            ItemStack stack = displayed();
            if (stack.isEmpty()) return null;
            UiText text = UiContext.platform().itemTooltip(McItem.of(stack));
            int state = state();
            if (state >= STATE_NO_FIT) {
                Component note = McText.toComponent(state == STATE_NO_FIT ? NovaTexts.RECIPE_NO_FIT : NovaTexts.RECIPE_LOCKED)
                        .copy().withStyle(ChatFormatting.RED);
                text = McText.of(Component.empty().append(McText.toComponent(text)).append("\n").append(note));
            }
            return new TooltipBubble(text).setMaxWidth(300);
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            SlotStyle s = ThemeManager.current().get(Styles.SLOT);
            float in = s.inset();
            int ax = absX();
            int ay = absY();
            int w = getWidth();
            int h = getHeight();
            int state = state();
            ItemStack stack = displayed();

            g.fillRounded(ax + in, ay + in, ax + w - in, ay + h - in, s.radius(), s.background());
            if (isHovered()) g.fillRounded(ax + in, ay + in, ax + w - in, ay + h - in, s.radius(), s.highlight());
            if (state == STATE_CRAFTABLE) g.strokeRounded(ax + in, ay + in, ax + w - in, ay + h - in, s.radius(), 1f, 0xFF60C060);
            if (!stack.isEmpty()) g.drawItem(McItem.of(stack), ax + (w - 16) / 2, ay + (h - 16) / 2, true);

            // Затемнение: нет ингредиентов, красноватое: не помещается, тёмное: не открыт
            // Dimming: no ingredients, reddish: does not fit, dark: not unlocked
            int overlay = state == STATE_MISSING ? 0x70000000
                    : state == STATE_NO_FIT ? 0x70802020 : state == STATE_LOCKED ? 0x90000000 : 0;
            if (overlay != 0) g.fillRounded(ax + in, ay + in, ax + w - in, ay + h - in, s.radius(), overlay);
        }

        @Override
        protected void onPointerDown(PointerEvent e) {
            if (e.getButton() != MouseButton.LEFT || collection == null) return;
            List<Recipe<?>> usable = new ArrayList<>();
            for (Recipe<?> r : shown()) {
                if (craftable.contains(r)) usable.add(r);
            }
            if (usable.size() == 1) place(usable.get(0));
            else if (usable.size() > 1) openVariants(this, usable);
            e.stopPropagation();
        }
    }
}