package org.mod.nova_core.ui.minecraft.demo;

import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.EventType;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.minecraft.UIScreen;
import org.mod.nova_core.ui.overlay.DialogButton;
import org.mod.nova_core.ui.overlay.Dialogs;
import org.mod.nova_core.ui.overlay.MenuItem;
import org.mod.nova_core.ui.overlay.Menus;
import org.mod.nova_core.ui.overlay.Placement;
import org.mod.nova_core.ui.overlay.Tooltips;

import java.util.List;

public class OverlayDemoScreen extends UIScreen {
    public OverlayDemoScreen() {
        super(Component.literal("Nova Core overlay demo"));
    }

    @Override
    protected void build(Widget root) {
        Panel panel = new Panel((root.getWidth() - 260) / 2, (root.getHeight() - 180) / 2, 260, 180);

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        stack.setPadding(Insets.all(10)).setGap(6).setAlignment(Alignment.STRETCH);

        TextWidget title = new TextWidget(0, 0, 0, 12, UiText.literal("Overlay demo"))
                .setAlignment(Alignment.CENTER);
        TextWidget status = new TextWidget(0, 0, 0, 12, UiText.literal("Right-click the panel"))
                .setAlignment(Alignment.CENTER);

        Widget spacer = new Widget(0, 0, 0, 0);
        spacer.setWeight(1f);

        // Тултип с задержкой наведения / A tooltip with a hover delay
        ButtonWidget hover = new ButtonWidget(0, 0, 0, 20, UiText.literal("Hover me"),
                () -> status.setText(UiText.literal("Hover button clicked")));
        Tooltips.attach(hover, UiText.literal("I am a tooltip"));

        // Попап у кнопки / A popup next to a button
        ButtonWidget popup = new ButtonWidget(0, 0, 0, 20, UiText.literal("Popup menu"), () -> { });
        popup.setOnClick(() -> Menus.show(popup, Placement.anchored(popup, Placement.Side.BELOW), List.of(
                MenuItem.of(UiText.literal("Copy"), () -> status.setText(UiText.literal("Copy chosen"))),
                MenuItem.of(UiText.literal("Paste"), () -> status.setText(UiText.literal("Paste chosen"))),
                MenuItem.disabled(UiText.literal("Delete")))));

        // Модальный диалог / A modal dialog
        ButtonWidget modal = new ButtonWidget(0, 0, 0, 20, UiText.literal("Modal dialog"),
                () -> Dialogs.show(panel, UiText.literal("Confirm"), UiText.literal("Do the thing?"), List.of(
                        new DialogButton(UiText.literal("Yes"), () -> status.setText(UiText.literal("Dialog: yes"))),
                        new DialogButton(UiText.literal("No"), () -> status.setText(UiText.literal("Dialog: no"))))));

        stack.add(title);
        stack.add(status);
        stack.add(spacer);
        stack.add(hover);
        stack.add(popup);
        stack.add(modal);
        panel.add(stack);

        // Контекстное меню по правой кнопке в точке клика / A context menu on right click at the click point
        panel.addListener(EventType.POINTER_DOWN, e -> {
            if (e.getButton() != MouseButton.RIGHT) return;
            Menus.show(panel, Placement.at((int) e.getX(), (int) e.getY()), List.of(
                    MenuItem.of(UiText.literal("Say hi"), () -> status.setText(UiText.literal("Hi!"))),
                    MenuItem.of(UiText.literal("Clear"), () -> status.setText(UiText.literal("Cleared")))));
            e.stopPropagation();
        });

        root.add(panel);
    }
}