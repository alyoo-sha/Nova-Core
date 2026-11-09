package org.mod.nova_core.ui.style;

// Вид рамки слота; цвета ARGB. inset сжимает рамку со всех сторон, давая зазоры; highlight цвет подсветки при наведении
// Slot frame look; ARGB colors. inset shrinks the frame on all sides, leaving gaps; highlight is the hover color
public record SlotStyle(int background, int border, int outputBorder, int radius, float inset, int highlight) {
    public static final SlotStyle DEFAULT = new SlotStyle(0xFF161616, 0xFF484848, 0xFF9A7B2C);

    public SlotStyle(int background, int border, int outputBorder, int radius, float inset) {
        this(background, border, outputBorder, radius, inset, 0x50FFFFFF);
    }

    // Прямые углы без зазоров / Square corners with no gaps
    public SlotStyle(int background, int border, int outputBorder) {
        this(background, border, outputBorder, 0, 0f, 0x50FFFFFF);
    }
}