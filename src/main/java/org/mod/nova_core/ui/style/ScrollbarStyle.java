package org.mod.nova_core.ui.style;

public record ScrollbarStyle(int width, int track, int thumb, int thumbHover, int minThumbHeight, int radius) {
    public static final ScrollbarStyle DEFAULT = new ScrollbarStyle(6, 0x40000000, 0xFF707070, 0xFFA0A0A0, 16);

    // Без скругления / Without rounding
    public ScrollbarStyle(int width, int track, int thumb, int thumbHover, int minThumbHeight) {
        this(width, track, thumb, thumbHover, minThumbHeight, 0);
    }

    public ScrollbarStyle withWidth(int width) {
        return new ScrollbarStyle(width, track, thumb, thumbHover, minThumbHeight, radius);
    }
}