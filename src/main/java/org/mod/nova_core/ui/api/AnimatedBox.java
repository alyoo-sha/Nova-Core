package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.animation.AnimatedValue;
import org.mod.nova_core.ui.animation.Easing;
import org.mod.nova_core.ui.animation.Interpolators;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;

// Контейнер, размер которого плавно идёт к заданному: раскладка вокруг него подстраивается сама
// A container whose size glides to a given one: the layout around it adapts by itself
public class AnimatedBox extends Widget {
    private final AnimatedValue<Float> animWidth;
    private final AnimatedValue<Float> animHeight;

    public AnimatedBox(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.animWidth = new AnimatedValue<>(Interpolators.FLOAT, (float) width, 200L, Easing.EASE_OUT);
        this.animHeight = new AnimatedValue<>(Interpolators.FLOAT, (float) height, 200L, Easing.EASE_OUT);
    }

    public AnimatedBox setDuration(long millis) {
        animWidth.setDuration(millis);
        animHeight.setDuration(millis);
        return this;
    }

    public void animateTo(int width, int height) {
        animWidth.setTarget((float) width);
        animHeight.setTarget((float) height);
    }

    // Мгновенно, без движения / Instantly, with no motion
    public void jumpTo(int width, int height) {
        animWidth.jumpTo((float) width);
        animHeight.jumpTo((float) height);
        setSize(width, height);
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        int w = Math.round(animWidth.peek());
        int h = Math.round(animHeight.peek());
        if (w != getWidth() || h != getHeight()) setSize(w, h); // раскладка пересчитается в следующем кадре / the layout is recomputed next frame
    }
}