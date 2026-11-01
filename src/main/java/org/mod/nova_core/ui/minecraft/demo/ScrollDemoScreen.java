package org.mod.nova_core.ui.minecraft.demo;

import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.ScrollView;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.minecraft.UIScreen;

public class ScrollDemoScreen extends UIScreen {
    public ScrollDemoScreen() {
        super(Component.literal("Nova Core scroll demo"));
    }

    @Override
    protected void build(Widget root) {
        Panel panel = new Panel((root.getWidth() - 200) / 2, (root.getHeight() - 160) / 2, 200, 160);

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        stack.setPadding(Insets.all(10)).setGap(6).setAlignment(Alignment.STRETCH);

        TextWidget status = new TextWidget(0, 0, 0, 12, UiText.literal("Scroll the list"))
                .setAlignment(Alignment.CENTER);

        // Контент: высота считается сама (WRAP) / Content: height is computed automatically (WRAP)
        VStack list = new VStack(0, 0, 0, 0);
        list.setHeightMode(SizeMode.WRAP);
        list.setPadding(Insets.all(2)).setGap(2).setAlignment(Alignment.STRETCH);
        for (int i = 1; i <= 30; i++) {
            final int n = i;
            if (n % 5 == 0) {
                list.add(new ButtonWidget(0, 0, 0, 20, UiText.literal("Button " + n),
                        () -> status.setText(UiText.literal("Clicked " + n))));
            } else {
                list.add(new TextWidget(0, 0, 0, 14, UiText.literal("Item " + n)));
            }
        }

        // Окно прокрутки забирает всё свободное место под заголовком / The scroll view takes all free space under the status
        ScrollView scroll = new ScrollView(0, 0, 0, 0);
        scroll.setContent(list);
        scroll.setWeight(1f);

        stack.add(status);
        stack.add(scroll);
        panel.add(stack);
        root.add(panel);
    }
}