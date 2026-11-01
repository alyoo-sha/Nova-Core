package org.mod.nova_core.ui.style;

public record SliderStyle(int track, int fill, int thumb, int thumbHover,
                          int trackHeight, int thumbWidth, int thumbHeight) {
    public static final SliderStyle DEFAULT = new SliderStyle(0xFF202020, 0xFF3A78C8, 0xFFB0B0B0, 0xFFE0E0E0, 4, 8, 14);
}