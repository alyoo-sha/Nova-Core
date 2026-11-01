package org.mod.nova_core.ui.style;

public record TextStyle(int color, boolean shadow) {
    public static final TextStyle DEFAULT = new TextStyle(0xFFFFFFFF, true);

    public TextStyle withColor(int argb) { return new TextStyle(argb, shadow); }
    public TextStyle withShadow(boolean shadow) { return new TextStyle(color, shadow); }
}