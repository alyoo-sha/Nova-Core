package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.ScrollView;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.ScrollbarStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.ThemeManager;

import java.util.List;

// Меню-попап из кнопок одной ширины. Закрывается выбором пункта, кликом мимо и Escape.
// Длинное меню прокручивается, выбранный пункт подсвечивается и показывается сразу
// A popup menu of equal-width buttons. Closes on item choice, outside click and Escape.
// A long menu scrolls, the chosen item is highlighted and brought into view
public final class Menus {
    // Отступ списка внутри панели и зазор между пунктами / The list padding inside the panel and the gap between items
    private static final int PADDING = 2;
    private static final int GAP = 1;
    private static final int DEFAULT_MAX_HEIGHT = 160;

    private Menus() {}

    // any: любой виджет того же экрана, нужен чтобы найти слой оверлеев / any: any widget of the same screen, used to find the overlay layer
    public static Overlay show(Widget any, Placement placement, List<MenuItem> items) {
        return show(any, placement, items, 0, DEFAULT_MAX_HEIGHT, -1);
    }

    public static Overlay show(Widget any, Placement placement, List<MenuItem> items, int minWidth) {
        return show(any, placement, items, minWidth, DEFAULT_MAX_HEIGHT, -1);
    }

    // minWidth: минимальная ширина меню; maxHeight: выше этого меню прокручивается; highlighted: индекс подсвеченного пункта или -1
    // minWidth: the minimum menu width; maxHeight: taller than this the menu scrolls; highlighted: the index of the highlighted item or -1
    public static Overlay show(Widget any, Placement placement, List<MenuItem> items,
                               int minWidth, int maxHeight, int highlighted) {
        if (items.isEmpty()) throw new IllegalArgumentException("A menu needs at least one item");

        UiPlatform platform = UiContext.platform();
        ButtonStyle base = ThemeManager.current().get(Styles.BUTTON);
        ScrollbarStyle bar = ThemeManager.current().get(Styles.SCROLLBAR);

        int n = items.size();
        int rowHeight = base.height();
        int contentHeight = n * rowHeight + (n - 1) * GAP + PADDING * 2;
        boolean scrolling = contentHeight > maxHeight;
        int barWidth = scrolling ? bar.width() : 0;

        int textWidth = 0;
        for (MenuItem item : items) textWidth = Math.max(textWidth, platform.textWidth(item.label()));
        int itemWidth = Math.max(textWidth + base.paddingX() * 2, minWidth - PADDING * 2 - barWidth);

        // Подсвеченный пункт всегда рисуется цветом наведения / The highlighted item is always drawn in the hover color
        ButtonStyle selectedStyle = new ButtonStyle(
                base.hover(), base.hover(), base.pressed(), base.disabled(),
                base.border(), base.focusedBorder(), base.text(), base.disabledText(),
                base.paddingX(), base.height(), base.textShadow(), base.radius());

        VStack list = new VStack(0, 0, 0, 0);
        list.setSizeMode(SizeMode.WRAP, SizeMode.WRAP);
        list.setPadding(Insets.all(PADDING)).setGap(GAP);

        Overlay[] ref = new Overlay[1];
        for (int i = 0; i < n; i++) {
            MenuItem item = items.get(i);
            ButtonWidget button = new ButtonWidget(0, 0, itemWidth, rowHeight, item.label(), () -> {
                if (ref[0] != null) ref[0].close();
                item.action().run();
            });
            button.setActive(item.enabled());
            if (i == highlighted) button.setStyle(selectedStyle);
            list.add(button);
        }

        Panel panel = new Panel(0, 0, 0, 0, Styles.POPUP);
        panel.setSizeMode(SizeMode.WRAP, SizeMode.WRAP);
        if (scrolling) {
            ScrollView scroll = new ScrollView(0, 0, itemWidth + PADDING * 2 + barWidth, maxHeight);
            scroll.setContent(list);
            if (highlighted >= 0) {
                scroll.setInitialScroll(PADDING + highlighted * (rowHeight + GAP) - maxHeight / 2 + rowHeight / 2);
            }
            panel.add(scroll);
        } else {
            panel.add(list);
        }

        ref[0] = OverlayHost.of(any).show(panel, OverlayOptions.popup(placement));
        return ref[0];
    }
}