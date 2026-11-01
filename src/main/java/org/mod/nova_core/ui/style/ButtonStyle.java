package org.mod.nova_core.ui.style;

// Вид кнопки; paddingX и height влияют на размер в режиме WRAP / Button look; paddingX and height affect the WRAP size
public record ButtonStyle(
        int normal, int hover, int pressed, int disabled,
        int border, int focusedBorder,
        int text, int disabledText,
        int paddingX, int height, boolean textShadow) {

    public static final ButtonStyle DEFAULT = new ButtonStyle(
            0xFF303030, 0xFF454545, 0xFF202020, 0xFF1A1A1A,
            0xFF606060, 0xFFFFFFFF,
            0xFFE0E0E0, 0xFF808080,
            8, 20, true);

    // Прежняя форма без textShadow: тень включена / The old form without textShadow: shadow on
    public ButtonStyle(int normal, int hover, int pressed, int disabled,
                       int border, int focusedBorder,
                       int text, int disabledText,
                       int paddingX, int height) {
        this(normal, hover, pressed, disabled, border, focusedBorder, text, disabledText, paddingX, height, true);
    }
}