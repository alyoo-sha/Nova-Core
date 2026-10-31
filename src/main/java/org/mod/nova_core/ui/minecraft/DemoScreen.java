package org.mod.nova_core.ui.minecraft;

import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.api.*;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;

public class DemoScreen extends UIScreen {
    private int clicks = 0;
    
    public DemoScreen() {
        super(Component.literal("Nova Core demo"));
    }

    @Override
    protected void build(Widget root) {
        Panel panel = new Panel((root.getWidth() - 200) / 2, (root.getHeight() - 110) / 2, 200, 110);

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        stack.setPadding(Insets.all(10)).setGap(6).setAlignment(Alignment.STRETCH);

        TextWidget title = new TextWidget(0, 0, 0, 12, Component.literal("Nova Core"))
                .setAlignment(Alignment.CENTER);
        TextWidget counter = new TextWidget(0, 0, 0, 12, Component.literal("Clicks: 0"))
                .setAlignment(Alignment.CENTER);

        // Пустой виджет с весом забирает всё свободное место / An empty widget with weight takes all free space
        Widget spacer = new Widget(0, 0, 0, 0);
        spacer.setWeight(1f);

        HStack row = new HStack(0, 0, 0, 20);
        row.setGap(6);
        ButtonWidget plus = new ButtonWidget(0, 0, 0, 20, Component.literal("+1"),
                () -> counter.setText(Component.literal("Clicks: " + (++clicks))));
        ButtonWidget reset = new ButtonWidget(0, 0, 0, 20, Component.literal("Reset"),
                () -> { clicks = 0; counter.setText(Component.literal("Clicks: 0")); });
        plus.setWeight(1f);
        reset.setWeight(1f);
        row.add(plus);
        row.add(reset);

        stack.add(title);
        stack.add(counter);
        stack.add(spacer);
        stack.add(row);
        panel.add(stack);
        root.add(panel);
    }
}