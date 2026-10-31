package org.mod.nova_core.ui.minecraft;

import net.minecraft.network.chat.Component;
import org.mod.nova_core.ui.graphics.UiText;

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
}