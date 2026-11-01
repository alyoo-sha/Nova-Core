package org.mod.nova_core.ui.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.UiText;

public final class McPlatform implements UiPlatform {
    @Override
    public int textWidth(UiText text) {
        return Minecraft.getInstance().font.width(McText.toComponent(text));
    }

    @Override
    public int lineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    @Override
    public void playClickSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public String getClipboard() {
        String value = Minecraft.getInstance().keyboardHandler.getClipboard();
        return value == null ? "" : value;
    }

    @Override
    public void setClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }
}