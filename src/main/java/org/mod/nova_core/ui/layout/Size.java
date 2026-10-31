package org.mod.nova_core.ui.layout;

// Размер в пикселях / Size in pixels
public record Size(int width, int height) {
    public static final Size ZERO = new Size(0, 0);
}