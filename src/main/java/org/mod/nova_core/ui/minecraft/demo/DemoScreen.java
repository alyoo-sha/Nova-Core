package org.mod.nova_core.ui.minecraft.demo;

import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.api.*;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.EventType;
import org.mod.nova_core.ui.layout.*;
import org.mod.nova_core.ui.minecraft.UIScreen;
import org.mod.nova_core.ui.theme.ThemeManager;
import org.mod.nova_core.ui.theme.Themes;

public class DemoScreen extends UIScreen {
    private int clicks = 0;

    public DemoScreen() {
        super(Component.literal("Nova Core demo"));
    }

    private static final class Probe extends TextWidget {
        Probe() { super(0, 0, 0, 12, UiText.literal("probe")); }

        @Override
        protected void onMount() {
            System.out.println("probe: onMount, mounted=" + isMounted());
            addCleanup(() -> System.out.println("probe: cleanup"));
        }

        @Override
        protected void onUnmount() {
            System.out.println("probe: onUnmount, mounted=" + isMounted());
        }
    }

    @Override
    protected void build(Widget root) {
        Panel panel = new Panel((root.getWidth() - 200) / 2, (root.getHeight() - 110) / 2, 200, 110);
        ButtonWidget themeButton = new ButtonWidget(0, 0, 0, 20, UiText.literal("Theme"),
                () -> ThemeManager.set(ThemeManager.current() == Themes.DARK ? Themes.LIGHT : Themes.DARK));

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        stack.setPadding(Insets.all(10)).setGap(6).setAlignment(Alignment.STRETCH);

        TextWidget title = new TextWidget(0, 0, 0, 12, UiText.literal("Nova Core"))
                .setAlignment(Alignment.CENTER);
        TextWidget counter = new TextWidget(0, 0, 0, 12, UiText.literal("Clicks: 0"))
                .setAlignment(Alignment.CENTER);

        // Пустой виджет с весом забирает всё свободное место / An empty widget with weight takes all free space
        Widget spacer = new Widget(0, 0, 0, 0);
        spacer.setWeight(1f);

        HStack row = new HStack(0, 0, 0, 20);
        row.setGap(6);
        ButtonWidget plus = new ButtonWidget(0, 0, 0, 20, UiText.literal("+1"),
                () -> counter.setText(UiText.literal("Clicks: " + (++clicks))));
        ButtonWidget reset = new ButtonWidget(0, 0, 0, 20, UiText.literal("Reset"),
                () -> { clicks = 0; counter.setText(UiText.literal("Clicks: 0")); });
        plus.setWeight(1f);
        reset.setWeight(1f);

        Probe probe = new Probe();
        ButtonWidget toggle = new ButtonWidget(0, 0, 0, 20, UiText.literal("Toggle"),
                () -> { if (probe.getParent() == null) stack.add(probe); else stack.remove(probe); });
        toggle.setWeight(1f);
        row.add(toggle);

        row.add(plus);
        row.add(reset);

        stack.add(title);
        stack.add(counter);
        stack.add(spacer);
        stack.add(row);
        panel.add(stack);
        stack.add(themeButton);

        // Phase-check listeners: CAPTURE fires top-down (root → target),
        // BUBBLE fires bottom-up (target → root).
        panel.addListener(EventType.POINTER_DOWN,
                e -> System.out.println("panel CAPTURE, target=" + e.getTarget().getClass().getSimpleName()), true);
        panel.addListener(EventType.POINTER_DOWN,
                e -> System.out.println("panel BUBBLE"), false);

        root.add(panel);
    }
}