package org.mod.nova_core.ui.form;

import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.SizeMode;

// Строка формы: подпись (может быть null), виджет ввода и текст ошибки под ним
// A form row: a label (may be null), the input widget and the error text below it
public class FormRow extends VStack {
    private static final int ERROR_COLOR = 0xFFE05050;

    public FormRow(Form form, FormField<?> field, UiText label, Widget input) {
        super(0, 0, 0, 0);
        setSizeMode(SizeMode.FILL, SizeMode.WRAP);
        setAlignment(Alignment.STRETCH);
        setGap(2);

        if (label != null) add(new TextWidget(0, 0, label));
        add(input);

        TextWidget errorText = new TextWidget(0, 0, UiText.literal(""));
        errorText.setColor(ERROR_COLOR);
        errorText.setVisible(false);
        add(errorText);

        form.registerInput(field, input);
        // Потеря фокуса «трогает» поле: с этого момента показывается его ошибка / Losing focus "touches" the field: its error shows from now on
        input.addFocusListener(focused -> {
            if (!focused) field.touch();
        });
        bind(field.error(), e -> {
            errorText.setVisible(e != null);
            if (e != null) errorText.setText(e);
        });
    }
}