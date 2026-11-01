package org.mod.nova_core.ui.minecraft.demo;

import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.Checkbox;
import org.mod.nova_core.ui.api.HStack;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextField;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.api.WrappedText;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.form.Form;
import org.mod.nova_core.ui.form.FormField;
import org.mod.nova_core.ui.form.FormRow;
import org.mod.nova_core.ui.form.Validators;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.minecraft.UIScreen;
import org.mod.nova_core.ui.state.Computed;
import org.mod.nova_core.ui.state.Signal;

public class FormDemoScreen extends UIScreen {
    // Состояние живёт в экране, а не в build: build вызывается заново при ресайзе / State lives in the screen, not in build: build runs again on resize
    private final Form form = new Form();
    private final FormField<String> name = form.field("",
            Validators.required(UiText.literal("Name is required")),
            Validators.minLength(3, UiText.literal("At least 3 characters")),
            Validators.maxLength(12, UiText.literal("At most 12 characters")));
    private final FormField<String> age = form.field("",
            Validators.required(UiText.literal("Age is required")),
            Validators.intInRange(1, 120, UiText.literal("Age must be 1..120")));
    private final FormField<Boolean> terms = form.field(false,
            Validators.isTrue(UiText.literal("You must accept the terms")));

    private final Signal<String> result = Signal.of("Fill the form and press Submit");
    private final Computed<UiText> state = new Computed<>(() -> UiText.literal(
            (form.valid().get() ? "Form is valid" : "Form has errors")
                    + (form.dirty().get() ? " | edited" : "")));

    private TextField nameField;

    public FormDemoScreen() {
        super(Component.literal("Nova Core form demo"));
    }

    @Override
    protected void build(Widget root) {
        Panel panel = new Panel((root.getWidth() - 300) / 2, (root.getHeight() - 250) / 2, 300, 250);

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        stack.setPadding(Insets.all(8)).setGap(5).setAlignment(Alignment.STRETCH);

        TextWidget title = new TextWidget(0, 0, 0, 12, UiText.literal("Form demo")).setAlignment(Alignment.CENTER);

        nameField = new TextField(0, 0, 0, 20, name.signal());
        nameField.setPlaceholder(UiText.literal("3-12 characters"));

        TextField ageField = new TextField(0, 0, 0, 20, age.signal());
        ageField.setFilter(c -> Character.isDigit(c));
        ageField.setMaxLength(3);

        Checkbox termsBox = new Checkbox(0, 0, UiText.literal("I accept the terms"), terms.signal());

        TextWidget stateText = new TextWidget(0, 0, 0, 12, state.peek()).setAlignment(Alignment.CENTER);
        stateText.bindText(state);

        WrappedText resultText = new WrappedText(0, 0, UiText.literal(result.peek())).setAlignment(Alignment.CENTER);
        resultText.bind(result, v -> resultText.setText(UiText.literal(v)));

        Widget spacer = new Widget(0, 0, 0, 0);
        spacer.setWeight(1f);

        HStack buttons = new HStack(0, 0, 0, 20);
        buttons.setGap(6);
        ButtonWidget submit = new ButtonWidget(0, 0, 0, 20, UiText.literal("Submit"), () -> {
            boolean ok = form.submit(f -> result.set("Submitted: " + name.get() + ", age " + age.get()));
            if (!ok) result.set("Fix the errors first");
        });
        ButtonWidget reset = new ButtonWidget(0, 0, 0, 20, UiText.literal("Reset"), () -> {
            form.reset();
            result.set("Form reset");
        });
        submit.setWeight(1f);
        reset.setWeight(1f);
        buttons.add(submit);
        buttons.add(reset);

        stack.add(title);
        stack.add(new FormRow(form, name, UiText.literal("Name"), nameField));
        stack.add(new FormRow(form, age, UiText.literal("Age"), ageField));
        stack.add(new FormRow(form, terms, null, termsBox));
        stack.add(stateText);
        stack.add(resultText);
        stack.add(spacer);
        stack.add(buttons);
        panel.add(stack);
        root.add(panel);
    }

    @Override
    protected Widget initialFocus() {
        return nameField;
    }
}