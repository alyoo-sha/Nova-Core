package org.mod.nova_core.ui.minecraft.demo;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.animation.SpringValue;
import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.Dropdown;
import org.mod.nova_core.ui.api.GridPane;
import org.mod.nova_core.ui.api.HStack;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextCell;
import org.mod.nova_core.ui.api.TextField;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.api.VirtualList;
import org.mod.nova_core.ui.api.WrappedText;
import org.mod.nova_core.ui.collection.ListModel;
import org.mod.nova_core.ui.collection.MultiSelection;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.minecraft.McText;
import org.mod.nova_core.ui.minecraft.UIScreen;
import org.mod.nova_core.ui.state.AsyncValue;
import org.mod.nova_core.ui.state.Computed;
import org.mod.nova_core.ui.state.Signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LimitsDemoScreen extends UIScreen {
    // Состояние живёт в экране, а не в build: build вызывается заново при ресайзе / State lives in the screen, not in build: build runs again on resize
    private final MultiSelection selection = new MultiSelection();
    private final ListModel<String> items = new ListModel<>(generate(300));
    private final Signal<Integer> primary = Signal.of(-1);
    private final Signal<Integer> mode = Signal.of(3);
    private final SpringValue spring = new SpringValue(0f);
    private final AsyncValue<String> async = new AsyncValue<>();
    private final Computed<UiText> status = new Computed<>(() -> {
        selection.changes().get();
        String loaded = async.value().get();
        return UiText.literal("selected " + selection.size() + " | async " + async.status().get()
                + (loaded == null ? "" : " (" + loaded + ")"));
    });

    private TextField nameField;

    public LimitsDemoScreen() {
        super(Component.literal("Nova Core limits demo"));
    }

    private static List<String> generate(int count) {
        List<String> out = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) out.add(String.format(Locale.ROOT, "Row %03d", i));
        return out;
    }

    @Override
    protected void build(Widget root) {
        Panel panel = new Panel((root.getWidth() - 360) / 2, (root.getHeight() - 290) / 2, 360, 290);

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        stack.setPadding(Insets.all(8)).setGap(5).setAlignment(Alignment.STRETCH);

        TextWidget title = new TextWidget(0, 0, 0, 12, UiText.literal("Limits demo")).setAlignment(Alignment.CENTER);

        // Простая сетка формы с одинаковыми колонками / A simple form grid with equal columns
        nameField = new TextField(0, 0, 0, 20);
        nameField.setPlaceholder(UiText.literal("double click selects a word"));
        List<UiText> modes = new ArrayList<>();
        for (int i = 1; i <= 30; i++) modes.add(UiText.literal("Option " + i));
        Dropdown dropdown = new Dropdown(0, 0, 0, 20, modes, mode);

        GridPane grid = new GridPane(2);
        grid.setEqualColumns(true).setGaps(6, 4);
        grid.setSizeMode(SizeMode.FILL, SizeMode.WRAP);
        grid.add(new TextWidget(0, 0, UiText.literal("Name")));
        grid.add(nameField);
        grid.add(new TextWidget(0, 0, UiText.literal("Mode (30 options)")));
        grid.add(dropdown);

        // Текст со стилями переносится, не теряя цветов / Styled text wraps without losing its colors
        Component styled = Component.literal("Styled ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("wrapped text keeps its colors across lines ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("and bold parts.").withStyle(ChatFormatting.BOLD));
        WrappedText wrapped = new WrappedText(0, 0, McText.of(styled));

        // Список с мультивыбором: Ctrl, Shift, Ctrl+A / A list with multi-selection: Ctrl, Shift, Ctrl+A
        VirtualList<String> list = new VirtualList<>(0, 0, 0, 0, 14, items,
                TextCell.<String>adapter(UiText::literal), primary);
        list.setMultiSelection(selection);
        list.setWeight(1f);

        // Пружина / The spring
        Widget track = new Widget(0, 0, 0, 16);
        Panel box = new Panel(0, 2, 12, 12);
        box.bind(spring.signal(), v -> box.moveTo(Math.round(v), 2));
        track.add(box);

        TextWidget statusText = new TextWidget(0, 0, 0, 12, status.peek()).setAlignment(Alignment.CENTER);
        statusText.bindText(status);

        HStack buttons = new HStack(0, 0, 0, 20);
        buttons.setGap(6);
        ButtonWidget springButton = new ButtonWidget(0, 0, 0, 20, UiText.literal("Spring"),
                () -> spring.setTarget(spring.target() < 10f ? Math.max(0, track.getWidth() - 12) : 0f));
        ButtonWidget loadButton = new ButtonWidget(0, 0, 0, 20, UiText.literal("Async load"), () -> async.load(() -> {
            try {
                Thread.sleep(800);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return "at " + System.currentTimeMillis() % 100000;
        }));
        springButton.setWeight(1f);
        loadButton.setWeight(1f);
        buttons.add(springButton);
        buttons.add(loadButton);

        stack.add(title);
        stack.add(grid);
        stack.add(wrapped);
        stack.add(list);
        stack.add(track);
        stack.add(statusText);
        stack.add(buttons);
        panel.add(stack);
        root.add(panel);
    }

    @Override
    protected Widget initialFocus() {
        return nameField;
    }
}