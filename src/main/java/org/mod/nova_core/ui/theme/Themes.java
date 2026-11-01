package org.mod.nova_core.ui.theme;

import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.FieldStyle;
import org.mod.nova_core.ui.style.PanelStyle;
import org.mod.nova_core.ui.style.ScrollbarStyle;
import org.mod.nova_core.ui.style.SliderStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.style.TextStyle;
import org.mod.nova_core.ui.style.TooltipStyle;

// Встроенные темы / Built-in themes
public final class Themes {
    // Пустая тема: все стили берутся из значений по умолчанию их ключей / An empty theme: every style comes from its key's default
    public static final Theme DARK = Theme.builder("dark").build();

    public static final Theme LIGHT = Theme.builder("light")
            .set(Styles.PANEL, new PanelStyle(0xF0E8E8E8, 0xFF909090, 1))
            .set(Styles.TEXT, new TextStyle(0xFF202020, false))
            .set(Styles.BUTTON, new ButtonStyle(
                    0xFFD0D0D0, 0xFFE4E4E4, 0xFFB8B8B8, 0xFFE8E8E8,
                    0xFF808080, 0xFF000000,
                    0xFF202020, 0xFF909090,
                    8, 20, false))
            .set(Styles.SCROLLBAR, new ScrollbarStyle(6, 0x30000000, 0xFF909090, 0xFF606060, 16))
            .set(Styles.TOOLTIP, new TooltipStyle(0xF8F4F4F4, 0xFF808080, 0xFF202020, 5, 3, false))
            .set(Styles.FIELD, new FieldStyle(
                    0xFFFFFFFF, 0xFF808080, 0xFF000000,
                    0xFF202020, 0xFF909090, 0xFF909090,
                    0x803060C0, 0xFF000000,
                    4, 20, false))
            .set(Styles.SLIDER, new SliderStyle(0xFFB0B0B0, 0xFF3A78C8, 0xFF606060, 0xFF303030, 4, 8, 14))
            .set(Styles.POPUP, new PanelStyle(0xFFEDEDED, 0xFF909090, 1))
            .build();

    private Themes() {}
}