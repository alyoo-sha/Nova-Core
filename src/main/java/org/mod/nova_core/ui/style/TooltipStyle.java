package org.mod.nova_core.ui.style;

public record TooltipStyle(int background, int border, int text, int paddingX, int paddingY,
                           boolean textShadow, int radius) {
    public static final TooltipStyle DEFAULT = new TooltipStyle(0xF0101018, 0xFF404060, 0xFFFFFFFF, 5, 3, true, 0);

    // Без скругления / Without rounding
    public TooltipStyle(int background, int border, int text, int paddingX, int paddingY, boolean textShadow) {
        this(background, border, text, paddingX, paddingY, textShadow, 0);
    }

    // Без скругления, тень текста включена / Without rounding, text shadow on
    public TooltipStyle(int background, int border, int text, int paddingX, int paddingY) {
        this(background, border, text, paddingX, paddingY, true, 0);
    }
}