package org.mod.nova_core.ui.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.UiText;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    // Перенос с сохранением цветов и стилей; слишком длинные слова режутся / Wrapping that keeps colors and styles; words that are too long are cut
    @Override
    public List<UiText> wrap(UiText text, int maxWidth) {
        int width = (maxWidth <= 0 || maxWidth == Integer.MAX_VALUE) ? 1 << 20 : maxWidth;
        List<FormattedText> parts = Minecraft.getInstance().font.getSplitter()
                .splitLines(McText.toComponent(text), width, Style.EMPTY);
        List<UiText> out = new ArrayList<>(parts.size());
        for (FormattedText part : parts) out.add(McText.of(toComponent(part)));
        if (out.isEmpty()) out.add(text);
        return out;
    }

    @Override
    public void post(Runnable task) {
        Minecraft.getInstance().execute(task);
    }

    private static Component toComponent(FormattedText text) {
        MutableComponent result = Component.empty();
        text.visit((style, s) -> {
            result.append(Component.literal(s).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}