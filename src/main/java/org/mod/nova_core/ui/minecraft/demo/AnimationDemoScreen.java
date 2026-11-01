package org.mod.nova_core.ui.minecraft.demo;

import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.animation.AnimatedValue;
import org.mod.nova_core.ui.animation.Easing;
import org.mod.nova_core.ui.animation.Interpolators;
import org.mod.nova_core.ui.animation.Tween;
import org.mod.nova_core.ui.api.ButtonWidget;
import org.mod.nova_core.ui.api.HStack;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.api.VStack;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.minecraft.UIScreen;

public class AnimationDemoScreen extends UIScreen {
    private static final int BLUE = 0xFF4090FF;
    private static final int ORANGE = 0xFFFF9030;
    private static final String[] EASING_NAMES = {"LINEAR", "EASE_IN", "EASE_OUT", "EASE_IN_OUT", "BACK_OUT"};
    private static final Easing[] EASINGS =
            {Easing.LINEAR, Easing.EASE_IN, Easing.EASE_OUT, Easing.EASE_IN_OUT, Easing.BACK_OUT};

    // Состояние живёт в экране, а не в build: build вызывается заново при ресайзе
    // State lives in the screen, not in build: build runs again on resize
    private final AnimatedValue<Integer> boxColor =
            new AnimatedValue<>(Interpolators.COLOR, BLUE, 300L, Easing.EASE_IN_OUT);
    private int easingIndex = 0;
    private Tween<Float> boxTween = null;
    private boolean trackShown = true;

    public AnimationDemoScreen() {
        super(Component.literal("Nova Core animation demo"));
    }

    // Бесконечно двигает квадрат туда и обратно; track должен быть смонтирован
    // Moves the square back and forth endlessly; track must be mounted
    private void startBox(Panel box, Widget track) {
        if (boxTween != null) boxTween.cancel();
        boxTween = track.animate(Tween.floats(0f, 1f)
                .duration(1200L)
                .easing(EASINGS[easingIndex])
                .yoyo(true)
                .loop()
                .onUpdate(p -> box.setPosition(Math.round(p * Math.max(0, track.getWidth() - box.getWidth())), 2)));
    }

    @Override
    protected void build(Widget root) {
        trackShown = true;

        // Панель плавно появляется при каждом монтировании / The panel fades in on every mount
        Panel panel = new Panel((root.getWidth() - 260) / 2, (root.getHeight() - 150) / 2, 260, 150) {
            @Override
            protected void onMount() {
                setOpacity(0f);
                animateOpacity(1f, 300L);
            }
        };

        VStack stack = new VStack(0, 0, 0, 0);
        stack.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        stack.setPadding(Insets.all(10)).setGap(8).setAlignment(Alignment.STRETCH);

        TextWidget title = new TextWidget(0, 0, 0, 12, UiText.literal("Animation demo"))
                .setAlignment(Alignment.CENTER);
        TextWidget hint = new TextWidget(0, 0, 0, 12, UiText.literal("Hover the buttons"))
                .setAlignment(Alignment.CENTER);

        // Цвет квадрата идёт из AnimatedValue через привязку / The square's color comes from an AnimatedValue via a binding
        Panel box = new Panel(0, 2, 16, 16);
        box.bind(boxColor.signal(), c -> box.setBackgroundColor(c));

        Widget track = new Widget(0, 0, 0, 20) {
            @Override
            protected void onMount() {
                startBox(box, this);
            }
        };
        track.add(box);

        ButtonWidget easing = new ButtonWidget(0, 0, 0, 20,
                UiText.literal("Easing: " + EASING_NAMES[easingIndex]), () -> { });
        easing.setOnClick(() -> {
            easingIndex = (easingIndex + 1) % EASINGS.length;
            easing.setLabel(UiText.literal("Easing: " + EASING_NAMES[easingIndex]));
            startBox(box, track);
        });

        ButtonWidget fade = new ButtonWidget(0, 0, 0, 20, UiText.literal("Fade out"), () -> { });
        fade.setOnClick(() -> {
            trackShown = !trackShown;
            track.animateOpacity(trackShown ? 1f : 0f, 400L);
            fade.setLabel(UiText.literal(trackShown ? "Fade out" : "Fade in"));
        });

        ButtonWidget color = new ButtonWidget(0, 0, 0, 20, UiText.literal("Color"),
                () -> boxColor.setTarget(boxColor.target() == BLUE ? ORANGE : BLUE));

        HStack row = new HStack(0, 0, 0, 20);
        row.setGap(6);
        fade.setWeight(1f);
        color.setWeight(1f);
        row.add(fade);
        row.add(color);

        stack.add(title);
        stack.add(hint);
        stack.add(track);
        stack.add(easing);
        stack.add(row);
        panel.add(stack);
        root.add(panel);
    }
}