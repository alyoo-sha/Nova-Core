package org.mod.nova_core.ui.layout;

// Допустимые границы размера, которые родитель даёт ребёнку / Size limits a parent gives to a child
public record Constraints(int minWidth, int maxWidth, int minHeight, int maxHeight) {
    // Нет ограничения сверху / No upper limit
    public static final int UNBOUNDED = Integer.MAX_VALUE;
    public static final Constraints UNCONSTRAINED = new Constraints(0, UNBOUNDED, 0, UNBOUNDED);

    // Ровно такой размер / Exactly this size
    public static Constraints tight(int width, int height) {
        return new Constraints(width, width, height, height);
    }

    // Не больше такого размера / At most this size
    public static Constraints loose(int maxWidth, int maxHeight) {
        return new Constraints(0, maxWidth, 0, maxHeight);
    }

    // Уменьшить максимум на d, не трогая UNBOUNDED / Reduce a maximum by d, leaving UNBOUNDED alone
    public static int shrink(int max, int d) {
        return max == UNBOUNDED ? UNBOUNDED : Math.max(0, max - d);
    }

    public boolean hasBoundedWidth() { return maxWidth != UNBOUNDED; }
    public boolean hasBoundedHeight() { return maxHeight != UNBOUNDED; }

    public int constrainWidth(int w) { return Math.max(minWidth, Math.min(maxWidth, w)); }
    public int constrainHeight(int h) { return Math.max(minHeight, Math.min(maxHeight, h)); }
}