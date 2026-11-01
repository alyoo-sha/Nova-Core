package org.mod.nova_core.ui.style;

// Вид кнопки; paddingX и height влияют на размер в режиме WRAP / Button look; paddingX and height affect the WRAP size
public record ButtonStyle(
        int normal, int hover, int pressed, int disabled,
        int border, int focusedBorder,
        int text, int disabledText,
        int paddingX, int height, boolean textShadow, int radius) {

    public static final ButtonStyle DEFAULT = new ButtonStyle(
            0xFF303030, 0xFF454545, 0xFF202020, 0xFF1A1A1A,
            0xFF606060, 0xFFFFFFFF,
            0xFFE0E0E0, 0xFF808080,
            8, 20, true, 0);

    // Без скругления / Without rounding
    public ButtonStyle(int normal, int hover, int pressed, int disabled,
                       int border, int focusedBorder,
                       int text, int disabledText,
                       int paddingX, int height, boolean textShadow) {
        this(normal, hover, pressed, disabled, border, focusedBorder, text, disabledText, paddingX, height, textShadow, 0);
    }

    // Без скругления, тень текста включена / Without rounding, text shadow on
    public ButtonStyle(int normal, int hover, int pressed, int disabled,
                       int border, int focusedBorder,
                       int text, int disabledText,
                       int paddingX, int height) {
        this(normal, hover, pressed, disabled, border, focusedBorder, text, disabledText, paddingX, height, true, 0);
    }
}