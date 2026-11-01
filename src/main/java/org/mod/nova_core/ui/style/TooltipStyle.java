package org.mod.nova_core.ui.style;

public record TooltipStyle(int background, int border, int text, int paddingX, int paddingY, boolean textShadow) {
    public static final TooltipStyle DEFAULT = new TooltipStyle(0xF0101018, 0xFF404060, 0xFFFFFFFF, 5, 3, true);

    // Прежняя форма без textShadow: тень включена / The old form without textShadow: shadow on
    public TooltipStyle(int background, int border, int text, int paddingX, int paddingY) {
        this(background, border, text, paddingX, paddingY, true);
    }
}