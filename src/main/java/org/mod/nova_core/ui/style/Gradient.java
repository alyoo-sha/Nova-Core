package org.mod.nova_core.ui.style;

// Цвета четырёх углов, между которыми идёт плавный переход / The colors of four corners with a smooth transition between them
public record Gradient(int topLeft, int topRight, int bottomLeft, int bottomRight) {
    public static Gradient solid(int argb) {
        return new Gradient(argb, argb, argb, argb);
    }

    public boolean isSolid() {
        return topLeft == topRight && topLeft == bottomLeft && topLeft == bottomRight;
    }
}