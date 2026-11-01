package org.mod.nova_core.ui.style;

// Вид текстового поля; цвета ARGB / Text field look; ARGB colors
public record FieldStyle(
        int background, int border, int focusedBorder,
        int text, int disabledText, int placeholder,
        int selection, int caret,
        int paddingX, int height, boolean textShadow) {

    public static final FieldStyle DEFAULT = new FieldStyle(
            0xFF101010, 0xFF606060, 0xFFFFFFFF,
            0xFFE0E0E0, 0xFF808080, 0xFF707070,
            0xA03060C0, 0xFFFFFFFF,
            4, 20, true);
}