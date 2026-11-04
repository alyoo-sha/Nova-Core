package org.mod.nova_core.ui.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.mod.nova_core.ui.graphics.UiText;

import java.util.Optional;

// UiText поверх Component: переводы, стили, цвета / UiText over Component: translations, styles, colors
public record McText(Component component) implements UiText {
    public static McText of(Component component) {
        return new McText(component);
    }

    @Override
    public String plain() {
        return component.getString();
    }

    // Любой UiText в Component / Any UiText to a Component
    public static Component toComponent(UiText text) {
        return text instanceof McText m ? m.component : Component.literal(text.plain());
    }

    // FormattedText в Component с сохранением стилей и шрифтов / FormattedText to a Component keeping styles and fonts
    public static Component fromFormatted(FormattedText text) {
        MutableComponent result = Component.empty();
        text.visit((style, s) -> {
            result.append(Component.literal(s).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }
}