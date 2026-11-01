package org.mod.nova_core.ui.theme;

import org.mod.nova_core.ui.style.ButtonStyle;
import org.mod.nova_core.ui.style.FieldStyle;
import org.mod.nova_core.ui.style.Gradient;
import org.mod.nova_core.ui.style.PanelStyle;
import org.mod.nova_core.ui.style.ScrollbarStyle;
import org.mod.nova_core.ui.style.SliderStyle;
import org.mod.nova_core.ui.style.SlotStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.style.TextStyle;
import org.mod.nova_core.ui.style.TooltipStyle;

import java.util.List;

// Встроенные темы / Built-in themes
public final class Themes {
    // Полупрозрачные сине-серые панели со скруглением, тёмные скруглённые слоты, чистый белый текст
    // Translucent slate-blue rounded panels, dark rounded slots, clean white text
    public static final Theme GLASS = Theme.builder("glass")
            .set(Styles.PANEL, new PanelStyle(
                    new Gradient(0xE05F6B82, 0xE0717D95, 0xE04E586C, 0xE0646F87), 0x30FFFFFF, 1, 8))
            .set(Styles.POPUP, new PanelStyle(
                    new Gradient(0xF2566178, 0xF2687489, 0xF2464F62, 0xF25A657B), 0x40FFFFFF, 1, 8))
            .set(Styles.TEXT, new TextStyle(0xFFFFFFFF, false))
            .set(Styles.BUTTON, new ButtonStyle(
                    0x66141B28, 0x8C1E2738, 0x99101520, 0x33141B28,
                    0x24FFFFFF, 0xFFFFFFFF,
                    0xFFFFFFFF, 0x80FFFFFF,
                    8, 20, false, 4))
            .set(Styles.SCROLLBAR, new ScrollbarStyle(5, 0x22000000, 0x66FFFFFF, 0xA0FFFFFF, 16, 2))
            .set(Styles.TOOLTIP, new TooltipStyle(0xF0434C5E, 0x40FFFFFF, 0xFFFFFFFF, 5, 3, false, 4))
            .set(Styles.FIELD, new FieldStyle(
                    0x66141B28, 0x30FFFFFF, 0xFFFFFFFF,
                    0xFFFFFFFF, 0x80FFFFFF, 0x80FFFFFF,
                    0x80608CD8, 0xFFFFFFFF,
                    4, 20, false, 4))
            .set(Styles.SLIDER, new SliderStyle(0x66141B28, 0xFF7AA2F0, 0xFFE8EEF8, 0xFFFFFFFF, 4, 8, 14, 2))
            .set(Styles.SLOT, new SlotStyle(0x55141B28, 0x00000000, 0xFFD8B24A, 3, 0.5f))
            .build();

    // Пустая тема: все стили берутся из значений по умолчанию их ключей / An empty theme: every style comes from its key's default
    public static final Theme DARK = Theme.builder("dark").build();

    public static final Theme LIGHT = Theme.builder("light")
            .set(Styles.PANEL, new PanelStyle(0xF0E8E8E8, 0xFF909090, 1))
            .set(Styles.POPUP, new PanelStyle(0xFFEDEDED, 0xFF909090, 1))
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
            .set(Styles.SLOT, new SlotStyle(0xFFC6C6C6, 0xFF8A8A8A, 0xFFB08020))
            .build();

    // Порядок переключения / The switching order
    public static final List<Theme> ALL = List.of(GLASS, DARK, LIGHT);

    private Themes() {}

    // Следующая тема по кругу / The next theme in a loop
    public static Theme next(Theme current) {
        int i = ALL.indexOf(current);
        return ALL.get((i + 1) % ALL.size());
    }
}