package org.mod.nova_core.ui.layout;

// Отступы / Padding
public record Insets(int left, int top, int right, int bottom) {
    public static final Insets ZERO = new Insets(0, 0, 0, 0);

    public static Insets all(int v) { return new Insets(v, v, v, v); }
    public static Insets symmetric(int horizontal, int vertical) { return new Insets(horizontal, vertical, horizontal, vertical); }
}