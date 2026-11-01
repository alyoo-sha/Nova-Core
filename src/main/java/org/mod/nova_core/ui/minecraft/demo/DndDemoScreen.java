package org.mod.nova_core.ui.minecraft.demo;

import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.api.HStack;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextCell;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.api.VirtualList;
import org.mod.nova_core.ui.api.WrappedText;
import org.mod.nova_core.ui.collection.ListModel;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.dnd.Dnd;
import org.mod.nova_core.ui.dnd.DragGhosts;
import org.mod.nova_core.ui.dnd.DragInfo;
import org.mod.nova_core.ui.dnd.DragSession;
import org.mod.nova_core.ui.dnd.DragSource;
import org.mod.nova_core.ui.dnd.DropZone;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.minecraft.UIScreen;
import org.mod.nova_core.ui.state.Computed;
import org.mod.nova_core.ui.state.Signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DndDemoScreen extends UIScreen {
    // Что переносим: текст и индекс в избранном (-1 = из общего списка) / What is carried: the text and the index in favorites (-1 = from the main list)
    private record Payload(String text, int favIndex) {}

    // Состояние живёт в экране, а не в build: build вызывается заново при ресайзе, а значения должны сохраняться
    // State lives in the screen, not in build: build runs again on resize, and the values must survive it
    private final ListModel<String> items = new ListModel<>(generate(200));
    private final ListModel<String> favorites = new ListModel<>(List.of("Item 007", "Item 042"));
    private final Signal<Integer> itemSel = Signal.of(-1);
    private final Signal<Integer> favSel = Signal.of(-1);
    private final Signal<String> status = Signal.of("Drag an item to Favorites. Escape cancels a drag.");
    private final Computed<UiText> favTitle = new Computed<>(() -> {
        favorites.changes().get();
        return UiText.literal("Favorites (" + favorites.size() + ")");
    });

    public DndDemoScreen() {
        super(Component.literal("Nova Core drag and drop demo"));
    }

    private static List<String> generate(int count) {
        List<String> out = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) out.add(String.format(Locale.ROOT, "Item %03d", i));
        return out;
    }

    @Override
    protected void build(Widget root) {
        Panel panel = new Panel((root.getWidth() - 400) / 2, (root.getHeight() - 250) / 2, 400, 250);

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        stack.setPadding(Insets.all(8)).setGap(5).setAlignment(Alignment.STRETCH);

        TextWidget title = new TextWidget(0, 0, 0, 12, UiText.literal("Drag and drop demo"))
                .setAlignment(Alignment.CENTER);

        // Левый список: источник / The left list: a source
        VirtualList<String> itemList = new VirtualList<>(0, 0, 0, 0, 14, items,
                TextCell.<String>adapter(UiText::literal), itemSel);
        itemList.setWeight(1f);
        Dnd.source(itemList, sourceFor(itemList, items, false));

        // Правый список: избранное; одновременно источник (перестановка, в корзину) / The right list: favorites; also a source (reorder, to the trash)
        VirtualList<String> favList = new VirtualList<>(0, 0, 0, 0, 14, favorites,
                TextCell.<String>adapter(UiText::literal), favSel);
        favList.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        Dnd.source(favList, sourceFor(favList, favorites, true));

        DropZone favZone = new DropZone(0, 0, 0, 0);
        favZone.setFrame(true);
        favZone.setWeight(1f);
        favZone.add(favList);
        favZone.setAccepts(s -> s.dataAs(Payload.class) != null);
        favZone.setOnDrop((s, x, y) -> dropToFavorites(s.dataAs(Payload.class), favList.indexAt(x, y)));

        // Корзина: принимает только то, что уже в избранном / The trash: accepts only what is already in favorites
        DropZone trash = new DropZone(0, 0, 0, 26);
        trash.setFrame(true);
        trash.setAccepts(s -> {
            Payload p = s.dataAs(Payload.class);
            return p != null && p.favIndex() >= 0;
        });
        trash.setOnDrop((s, x, y) -> dropToTrash(s.dataAs(Payload.class)));
        TextWidget trashLabel = new TextWidget(0, 0, 0, 26, UiText.literal("Drop a favorite here to remove it"))
                .setAlignment(Alignment.CENTER);
        trashLabel.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        trashLabel.setPointerTransparent(true);
        trash.add(trashLabel);

        TextWidget favLabel = new TextWidget(0, 0, 0, 12, favTitle.peek());
        favLabel.bindText(favTitle);

        VStack left = new VStack(0, 0, 0, 0);
        left.setGap(3).setAlignment(Alignment.STRETCH);
        left.setWeight(1f);
        left.add(new TextWidget(0, 0, 0, 12, UiText.literal("Items (drag me)")));
        left.add(itemList);

        VStack right = new VStack(0, 0, 0, 0);
        right.setGap(3).setAlignment(Alignment.STRETCH);
        right.setWeight(1f);
        right.add(favLabel);
        right.add(favZone);
        right.add(trash);

        HStack body = new HStack(0, 0, 0, 0);
        body.setGap(8).setAlignment(Alignment.STRETCH);
        body.setWeight(1f);
        body.add(left);
        body.add(right);

        WrappedText statusText = new WrappedText(0, 0, UiText.literal(status.peek())).setAlignment(Alignment.CENTER);
        statusText.bind(status, v -> statusText.setText(UiText.literal(v)));

        stack.add(title);
        stack.add(body);
        stack.add(statusText);
        panel.add(stack);
        root.add(panel);
    }

    // Источник для списка: какой элемент под точкой нажатия, тот и несём / A source for a list: whichever item is under the press point is carried
    private DragSource sourceFor(VirtualList<String> list, ListModel<String> model, boolean fromFavorites) {
        return new DragSource() {
            @Override
            public DragInfo begin(double pressX, double pressY) {
                int i = list.indexAt(pressX, pressY);
                if (i < 0) return null; // полоса прокрутки или пустое место: обычная обработка мыши / the scrollbar or empty space: regular mouse handling
                String text = model.get(i);
                return new DragInfo(new Payload(text, fromFavorites ? i : -1), DragGhosts.text(UiText.literal(text)));
            }

            @Override
            public void onDragEnd(DragSession session, boolean dropped) {
                if (!dropped) status.set("Drag cancelled");
            }
        };
    }

    private void dropToFavorites(Payload p, int index) {
        int size = favorites.size();
        if (p.favIndex() < 0) {
            if (favorites.snapshot().contains(p.text())) {
                status.set(p.text() + " is already in Favorites");
                return;
            }
            int at = index < 0 ? size : Math.min(index, size);
            favorites.add(at, p.text());
            favSel.set(at);
            status.set("Added " + p.text() + " at " + at);
        } else {
            int from = p.favIndex();
            int to = index < 0 ? size - 1 : Math.min(index, size - 1);
            if (from == to) {
                status.set(p.text() + " stayed in place");
                return;
            }
            favorites.modify(list -> list.add(to, list.remove(from)));
            favSel.set(to);
            status.set("Moved " + p.text() + " to " + to);
        }
    }

    private void dropToTrash(Payload p) {
        if (p.favIndex() < 0 || p.favIndex() >= favorites.size()) return;
        favorites.remove(p.favIndex());
        favSel.set(-1);
        status.set("Removed " + p.text());
    }
}