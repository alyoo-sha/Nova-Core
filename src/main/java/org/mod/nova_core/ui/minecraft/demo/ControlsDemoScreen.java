package org.mod.nova_core.ui.minecraft.demo;

import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.Checkbox;
import org.mod.nova_core.ui.api.Dropdown;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.Slider;
import org.mod.nova_core.ui.api.TextField;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.api.WrappedText;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.minecraft.UIScreen;
import org.mod.nova_core.ui.state.Computed;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.theme.ThemeManager;
import org.mod.nova_core.ui.theme.Themes;

import java.util.List;

public class ControlsDemoScreen extends UIScreen {
    private static final List<UiText> MODES = List.of(
            UiText.literal("Survival"), UiText.literal("Creative"), UiText.literal("Adventure"));

    // Состояние живёт в экране, а не в build: build вызывается заново при ресайзе, а значения должны сохраняться
    // State lives in the screen, not in build: build runs again on resize, and the values must survive it
    private final Signal<String> name = Signal.of("");
    private final Signal<Boolean> music = Signal.of(true);
    private final Signal<Float> volume = Signal.of(50f);
    private final Signal<Integer> mode = Signal.of(0);
    private final Computed<UiText> summary = new Computed<>(() -> UiText.literal(
            (name.get().isEmpty() ? "stranger" : name.get())
                    + " | music " + (music.get() ? "on" : "off")
                    + " | volume " + Math.round(volume.get())
                    + " | " + MODES.get(mode.get()).plain()));

    private TextField nameField;

    public ControlsDemoScreen() {
        super(Component.literal("Nova Core controls demo"));
    }

    @Override
    protected void build(Widget root) {
        Panel panel = new Panel((root.getWidth() - 300) / 2, (root.getHeight() - 230) / 2, 300, 230);

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        stack.setPadding(Insets.all(10)).setGap(5).setAlignment(Alignment.STRETCH);

        TextWidget title = new TextWidget(0, 0, 0, 12, UiText.literal("Controls demo"))
                .setAlignment(Alignment.CENTER);

        TextField field = new TextField(0, 0, 0, 20, name);
        field.setPlaceholder(UiText.literal("Your name..."));
        nameField = field;

        Checkbox checkbox = new Checkbox(0, 0, UiText.literal("Enable music"), music);

        Slider slider = new Slider(0, 0, 0, 16, 0f, 100f, volume);
        slider.setStep(5f);

        Dropdown dropdown = new Dropdown(0, 0, 0, 20, MODES, mode);

        WrappedText summaryText = new WrappedText(0, 0, summary.peek()).setAlignment(Alignment.CENTER);
        summaryText.bindText(summary);

        WrappedText note = new WrappedText(0, 0, UiText.literal(
                "This paragraph wraps to the panel width and even breaks very long words: "
                        + "Supercalifragilisticexpialidocious_Supercalifragilisticexpialidocious."));

        Widget spacer = new Widget(0, 0, 0, 0);
        spacer.setWeight(1f);

        ButtonWidget theme = new ButtonWidget(0, 0, 0, 20, UiText.literal("Theme"),
                ThemeManager::cycle);

        stack.add(title);
        stack.add(field);
        stack.add(checkbox);
        stack.add(slider);
        stack.add(dropdown);
        stack.add(summaryText);
        stack.add(note);
        stack.add(spacer);
        stack.add(theme);
        panel.add(stack);
        root.add(panel);
    }

    @Override
    protected Widget initialFocus() {
        return nameField;
    }
}