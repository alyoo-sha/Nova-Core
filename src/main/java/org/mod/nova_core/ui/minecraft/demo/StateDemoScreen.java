package org.mod.nova_core.ui.minecraft.demo;

import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.HStack;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.minecraft.UIScreen;
import org.mod.nova_core.ui.state.Computed;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.state.Signals;

public class StateDemoScreen extends UIScreen {
    // Состояние живёт в экране, а не в build: build вызывается заново при ресайзе
    // State lives in the screen, not in build: build runs again on resize
    private final Signal<Integer> count = Signal.of(0);
    private final Computed<UiText> label = new Computed<>(() -> UiText.literal("Count: " + count.get()));
    private final Computed<Boolean> big = new Computed<>(() -> count.get() >= 5);

    public StateDemoScreen() {
        super(Component.literal("Nova Core state demo"));
    }

    @Override
    protected void build(Widget root) {
        Panel panel = new Panel((root.getWidth() - 240) / 2, (root.getHeight() - 120) / 2, 240, 120);

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        stack.setPadding(Insets.all(10)).setGap(6).setAlignment(Alignment.STRETCH);

        TextWidget countText = new TextWidget(0, 0, 0, 12, label.peek()).setAlignment(Alignment.CENTER);
        countText.bindText(label);

        TextWidget bigText = new TextWidget(0, 0, 0, 12, UiText.literal("5 or more!")).setAlignment(Alignment.CENTER);
        bigText.bindVisible(big);

        // Виджет вне дерева: его привязка включается, только пока он в дереве
        // A widget outside the tree: its binding is active only while it is in the tree
        TextWidget watcher = new TextWidget(0, 0, 0, 12, UiText.literal("watcher")).setAlignment(Alignment.CENTER);
        watcher.bind(count, v -> System.out.println("watcher sees " + v));

        Widget spacer = new Widget(0, 0, 0, 0);
        spacer.setWeight(1f);

        HStack row = new HStack(0, 0, 0, 20);
        row.setGap(6);
        ButtonWidget plus = new ButtonWidget(0, 0, 0, 20, UiText.literal("+1"), () -> count.update(n -> n + 1));
        ButtonWidget plus3 = new ButtonWidget(0, 0, 0, 20, UiText.literal("+3"),
                () -> Signals.batch(() -> {
                    for (int i = 0; i < 3; i++) count.update(n -> n + 1);
                }));
        ButtonWidget toggle = new ButtonWidget(0, 0, 0, 20, UiText.literal("Toggle"),
                () -> { if (watcher.getParent() == null) stack.add(watcher); else stack.remove(watcher); });
        ButtonWidget reset = new ButtonWidget(0, 0, 0, 20, UiText.literal("Reset"), () -> count.set(0));
        plus.setWeight(1f);
        plus3.setWeight(1f);
        toggle.setWeight(1f);
        reset.setWeight(1f);
        row.add(plus);
        row.add(plus3);
        row.add(toggle);
        row.add(reset);

        stack.add(countText);
        stack.add(bigText);
        stack.add(spacer);
        stack.add(row);
        panel.add(stack);

        // Логи; привязываем к panel, а не к root: root переживает пересборку, panel нет
        // Logging; bound to panel, not root: root survives a rebuild, panel does not
        panel.bind(count, v -> System.out.println("count effect: " + v));
        panel.bind(big, v -> System.out.println("big effect: " + v));

        root.add(panel);
    }
}