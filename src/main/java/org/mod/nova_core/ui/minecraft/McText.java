package org.mod.nova_core.ui.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import org.mod.nova_core.Nova_core;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.ThemeManager;

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

    private static String cachedFont = "";
    private static ResourceLocation cachedFontId = null;
    private static boolean cachedFontExists = false;

    public static Component toComponent(UiText text) {
        Component base = text instanceof McText m ? m.component : Component.literal(text.plain());
        String font = ThemeManager.current().get(Styles.FONT).font();
        if (font.isEmpty()) return base;
        if (!font.equals(cachedFont)) {
            cachedFont = font;
            cachedFontId = Nova_core.parse(font);
            cachedFontExists = Minecraft.getInstance().getResourceManager().getResource(
                    Nova_core.makeId(cachedFontId.getNamespace(), "font/" + cachedFontId.getPath() + ".json")).isPresent();
        }
        if (!cachedFontExists) return base;
        ResourceLocation id = cachedFontId;
        return base.copy().withStyle(s -> s.getFont().equals(Style.DEFAULT_FONT) ? s.withFont(id) : s);
    }

    // Тёмно-серый ванильных строк (идентификатор, NBT) плохо виден на нашем фоне / Vanilla's dark gray lines (id, NBT) are hard to see on our background
    public static Component brightenDarkGray(Component component) {
        MutableComponent out = Component.empty();
        component.visit((style, s) -> {
            TextColor color = style.getColor();
            Style fixed = color != null && color.getValue() == 0x555555 ? style.withColor(0xA9B2C6) : style;
            out.append(Component.literal(s).withStyle(fixed));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
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