package org.mod.nova_core.ui.style;

// Фон и рамка панели; цвета ARGB / Panel background and border; ARGB colors
public record PanelStyle(int background, int border, int borderWidth) {
    public static final PanelStyle DEFAULT = new PanelStyle(0xC0101010, 0xFF404040, 1);

    public PanelStyle withBackground(int argb) { return new PanelStyle(argb, border, borderWidth); }
    public PanelStyle withBorder(int argb) { return new PanelStyle(background, argb, borderWidth); }
    public PanelStyle withBorderWidth(int width) { return new PanelStyle(background, border, width); }
}