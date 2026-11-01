package org.mod.nova_core.ui.minecraft.demo;

import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.HStack;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextCell;
import org.mod.nova_core.ui.api.TextField;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.api.VirtualGrid;
import org.mod.nova_core.ui.api.VirtualList;
import org.mod.nova_core.ui.api.WrappedText;
import org.mod.nova_core.ui.collection.ItemAdapter;
import org.mod.nova_core.ui.collection.ItemSource;
import org.mod.nova_core.ui.collection.ListModel;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.minecraft.UIScreen;
import org.mod.nova_core.ui.overlay.TooltipBubble;
import org.mod.nova_core.ui.overlay.Tooltips;
import org.mod.nova_core.ui.state.Computed;
import org.mod.nova_core.ui.state.Signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class CollectionsDemoScreen extends UIScreen {
    private static final int ITEM_COUNT = 10_000;
    private static final int CELL_COUNT = 5_000;

    // Состояние живёт в экране, а не в build: build вызывается заново при ресайзе, а значения должны сохраняться.
    // Порядок полей важен: Computed считается сразу и читает поля выше
    // State lives in the screen, not in build: build runs again on resize, and the values must survive it.
    // The field order matters: a Computed is evaluated immediately and reads the fields above it
    private final ListModel<String> all = new ListModel<>(generate(ITEM_COUNT));
    private final ListModel<Integer> cells = new ListModel<>(range(CELL_COUNT));
    private final Signal<String> query = Signal.of("");
    private final Signal<Integer> listSel = Signal.of(-1);
    private final Signal<Integer> gridSel = Signal.of(-1);
    private final Signal<String> lastAction = Signal.of("-");

    // Равенство (a, b) -> false: списки не сравниваются целиком, пересчёт идёт только при изменении зависимостей
    // Equality (a, b) -> false: lists are not compared whole, a recompute only happens when a dependency changed
    private final Computed<List<String>> filtered = new Computed<>(() -> {
        all.changes().get();
        String q = query.get().trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return all.snapshot();
        List<String> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            String s = all.get(i);
            if (s.toLowerCase(Locale.ROOT).contains(q)) out.add(s);
        }
        return out;
    }, (a, b) -> false);

    private final Computed<UiText> status = new Computed<>(() -> {
        all.changes().get();
        return UiText.literal("Shown " + filtered.get().size() + " of " + all.size()
                + " | list: " + listSel.get() + " | grid: " + gridSel.get()
                + " | last: " + lastAction.get());
    });

    private TextField searchField;
    private int extraCount = 0;

    public CollectionsDemoScreen() {
        super(Component.literal("Nova Core collections demo"));
    }

    private static List<String> generate(int count) {
        List<String> out = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) out.add(String.format(Locale.ROOT, "Item %05d", i));
        return out;
    }

    private static List<Integer> range(int count) {
        List<Integer> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) out.add(i);
        return out;
    }

    @Override
    protected void build(Widget root) {
        Panel panel = new Panel((root.getWidth() - 380) / 2, (root.getHeight() - 250) / 2, 380, 250);

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        stack.setPadding(Insets.all(8)).setGap(5).setAlignment(Alignment.STRETCH);

        TextWidget title = new TextWidget(0, 0, 0, 12, UiText.literal("Collections demo"))
                .setAlignment(Alignment.CENTER);

        searchField = new TextField(0, 0, 0, 20, query);
        searchField.setPlaceholder(UiText.literal("Filter items..."));

        // Список строк: источник из Computed, строки TextCell / A list of rows: a Computed source, TextCell rows
        ItemSource<String> listSource = ItemSource.ofSignal(filtered);
        ItemAdapter<String> listAdapter = TextCell.adapter(UiText::literal);
        VirtualList<String> list = new VirtualList<>(0, 0, 0, 0, 14, listSource, listAdapter, listSel);
        list.setWeight(1f);
        list.setOnActivate(i -> lastAction.set("list " + i));

        // Сетка своих ячеек с тултипом / A grid of custom cells with a tooltip
        ItemAdapter<Integer> gridAdapter = ItemAdapter.<Integer, ColorCell>of(
                CollectionsDemoScreen::createCell,
                (cell, item, index, sel) -> cell.set(item, sel));
        VirtualGrid<Integer> grid = new VirtualGrid<>(0, 0, 0, 0, cells, gridAdapter, gridSel);
        grid.setCellSize(20, 20).setGap(2);
        grid.setWeight(1f);
        grid.setOnActivate(i -> lastAction.set("grid " + i));

        HStack body = new HStack(0, 0, 0, 0);
        body.setGap(6).setAlignment(Alignment.STRETCH);
        body.setWeight(1f);
        body.add(list);
        body.add(grid);

        WrappedText statusText = new WrappedText(0, 0, status.peek()).setAlignment(Alignment.CENTER);
        statusText.bindText(status);

        HStack buttons = new HStack(0, 0, 0, 20);
        buttons.setGap(4);
        ButtonWidget add = new ButtonWidget(0, 0, 0, 20, UiText.literal("Add 100"), () ->
                all.modify(items -> {
                    for (int i = 0; i < 100; i++) items.add("Extra " + (++extraCount));
                }));
        ButtonWidget clear = new ButtonWidget(0, 0, 0, 20, UiText.literal("Clear"), all::clear);
        ButtonWidget reset = new ButtonWidget(0, 0, 0, 20, UiText.literal("Reset"), () -> {
            extraCount = 0;
            all.setAll(generate(ITEM_COUNT));
        });
        ButtonWidget pick = new ButtonWidget(0, 0, 0, 20, UiText.literal("Pick #5000"), () -> {
            int li = Math.min(4999, filtered.peek().size() - 1);
            if (li >= 0) {
                list.setSelectedIndex(li);
                list.scrollToIndex(li);
            }
            grid.setSelectedIndex(4999);
            grid.scrollToIndex(4999);
        });
        for (ButtonWidget b : new ButtonWidget[] {add, clear, reset, pick}) {
            b.setWeight(1f);
            buttons.add(b);
        }

        stack.add(title);
        stack.add(searchField);
        stack.add(body);
        stack.add(statusText);
        stack.add(buttons);
        panel.add(stack);
        root.add(panel);
    }

    @Override
    protected Widget initialFocus() {
        return searchField;
    }

    // Ячейка создаётся один раз и потом переиспользуется, поэтому тултип цепляем в фабрике
    // A cell is created once and reused afterwards, so the tooltip is attached in the factory
    private static ColorCell createCell() {
        ColorCell cell = new ColorCell();
        Supplier<Widget> tip = () -> new TooltipBubble(UiText.literal("Cell #" + cell.value));
        Tooltips.attach(cell, tip);
        return cell;
    }

    // Цветной квадрат; цвет вычисляется из номера / A colored square; the color is computed from the number
    private static final class ColorCell extends Widget {
        private int value = 0;
        private boolean selected = false;

        ColorCell() {
            super(0, 0, 0, 0);
        }

        void set(int value, boolean selected) {
            this.value = value;
            this.selected = selected;
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            int r = 70 + (value * 37) % 150;
            int gr = 70 + (value * 91) % 150;
            int b = 70 + (value * 53) % 150;
            int ax = absX();
            int ay = absY();
            int w = getWidth();
            int h = getHeight();

            g.fill(ax, ay, ax + w, ay + h, 0xFF000000 | (r << 16) | (gr << 8) | b);
            if (isHovered()) g.fill(ax, ay, ax + w, ay + h, 0x40FFFFFF);
            if (selected) {
                g.fill(ax, ay, ax + w, ay + 2, 0xFFFFFFFF);
                g.fill(ax, ay + h - 2, ax + w, ay + h, 0xFFFFFFFF);
                g.fill(ax, ay + 2, ax + 2, ay + h - 2, 0xFFFFFFFF);
                g.fill(ax + w - 2, ay + 2, ax + w, ay + h - 2, 0xFFFFFFFF);
            }
        }
    }
}