package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.api.*;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.style.Styles;

import java.util.List;

// Модальный диалог: заголовок, строка текста и ряд кнопок. Escape закрывает его без действия
// A modal dialog: a title, a line of text and a row of buttons. Escape closes it with no action
public final class Dialogs {
    private static final int WIDTH = 240;

    private Dialogs() {}

    public static Overlay show(Widget any, UiText title, UiText message, List<DialogButton> buttons) {
        if (buttons.isEmpty()) throw new IllegalArgumentException("A dialog needs at least one button");

        Panel panel = new Panel(0, 0, WIDTH, 0, Styles.POPUP);
        panel.setHeightMode(SizeMode.WRAP);

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.WRAP);
        stack.setPadding(Insets.all(10)).setGap(8).setAlignment(Alignment.STRETCH);

        TextWidget titleText = new TextWidget(0, 0, 0, 12, title).setAlignment(Alignment.CENTER);
        WrappedText messageText = new WrappedText(0, 0, message).setAlignment(Alignment.CENTER);

        HStack row = new HStack(0, 0, 0, 20);
        row.setGap(6);

        Overlay[] ref = new Overlay[1];
        for (DialogButton b : buttons) {
            ButtonWidget button = new ButtonWidget(0, 0, 0, 20, b.label(), () -> {
                if (ref[0] != null) ref[0].close();
                b.action().run();
            });
            button.setWeight(1f);
            row.add(button);
        }

        stack.add(titleText);
        stack.add(messageText);
        stack.add(row);
        panel.add(stack);

        ref[0] = OverlayHost.of(any).show(panel, OverlayOptions.modal());
        return ref[0];
    }
}