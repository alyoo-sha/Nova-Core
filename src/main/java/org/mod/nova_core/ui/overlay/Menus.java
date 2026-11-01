package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.ThemeManager;

import java.util.List;

// Меню-попап из кнопок одной ширины. Закрывается выбором пункта, кликом мимо и Escape
// A popup menu of equal-width buttons. Closes on item choice, outside click and Escape
public final class Menus {
    // Отступ списка внутри панели с каждой стороны / The list padding inside the panel on each side
    private static final int PADDING = 2;

    private Menus() {}

    // any: любой виджет того же экрана, нужен чтобы найти слой оверлеев / any: any widget of the same screen, used to find the overlay layer
    public static Overlay show(Widget any, Placement placement, List<MenuItem> items) {
        return show(any, placement, items, 0);
    }

    // minWidth: минимальная ширина всего меню, например ширина кнопки-якоря / minWidth: the minimum width of the whole menu, e.g. the anchor button's width
    public static Overlay show(Widget any, Placement placement, List<MenuItem> items, int minWidth) {
        if (items.isEmpty()) throw new IllegalArgumentException("A menu needs at least one item");

        UiPlatform platform = UiContext.platform();
        ButtonStyle buttonStyle = ThemeManager.current().get(Styles.BUTTON);
        int textWidth = 0;
        for (MenuItem item : items) textWidth = Math.max(textWidth, platform.textWidth(item.label()));
        int itemWidth = Math.max(textWidth + buttonStyle.paddingX() * 2, minWidth - PADDING * 2);

        VStack list = new VStack(0, 0, 0, 0);
        list.setSizeMode(SizeMode.WRAP, SizeMode.WRAP);
        list.setPadding(Insets.all(PADDING)).setGap(1);

        Overlay[] ref = new Overlay[1];
        for (MenuItem item : items) {
            ButtonWidget button = new ButtonWidget(0, 0, itemWidth, buttonStyle.height(), item.label(), () -> {
                if (ref[0] != null) ref[0].close();
                item.action().run();
            });
            button.setActive(item.enabled());
            list.add(button);
        }

        Panel panel = new Panel(0, 0, 0, 0, Styles.POPUP);
        panel.setSizeMode(SizeMode.WRAP, SizeMode.WRAP);
        panel.add(list);

        ref[0] = OverlayHost.of(any).show(panel, OverlayOptions.popup(placement));
        return ref[0];
    }
}