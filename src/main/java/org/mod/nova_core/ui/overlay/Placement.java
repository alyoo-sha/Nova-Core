package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.component.Widget;

import java.util.Objects;

// Где показать оверлей. Результат всегда зажимается в границы экрана / Where to show an overlay. The result is always clamped to the screen
public final class Placement {
    public enum Side { BELOW, ABOVE, RIGHT, LEFT }

    public record Position(int x, int y) {}

    private enum Kind { CENTER, POINT, POINTER, ANCHOR }

    private final Kind kind;
    private final int pointX;
    private final int pointY;
    private final Widget anchor;
    private final Side side;
    private int offsetX;
    private int offsetY;
    // Зазор между якорем и оверлеем по главной оси / Gap between the anchor and the overlay along the main axis
    private int gap = 2;

    private Placement(Kind kind, int pointX, int pointY, Widget anchor, Side side, int offsetX, int offsetY) {
        this.kind = kind;
        this.pointX = pointX;
        this.pointY = pointY;
        this.anchor = anchor;
        this.side = side;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public static Placement centered() {
        return new Placement(Kind.CENTER, 0, 0, null, Side.BELOW, 0, 0);
    }

    // Левый верхний угол в этой точке / Top-left corner at this point
    public static Placement at(int x, int y) {
        return new Placement(Kind.POINT, x, y, null, Side.BELOW, 0, 0);
    }

    // У курсора в момент показа; по умолчанию смещение (12, 14) / At the cursor at show time; default offset is (12, 14)
    public static Placement atPointer() {
        return new Placement(Kind.POINTER, 0, 0, null, Side.BELOW, 12, 14);
    }

    // Рядом с виджетом; сторона переворачивается, если не помещается / Next to a widget; the side flips if it does not fit
    public static Placement anchored(Widget anchor, Side side) {
        return new Placement(Kind.ANCHOR, 0, 0, Objects.requireNonNull(anchor), Objects.requireNonNull(side), 0, 0);
    }

    // Сдвиг (у atPointer это смещение от курсора) / Shift (for atPointer it is the offset from the cursor)
    public Placement offset(int dx, int dy) {
        this.offsetX = dx;
        this.offsetY = dy;
        return this;
    }

    public Placement gap(int gap) {
        this.gap = Math.max(0, gap);
        return this;
    }

    Widget anchor() { return anchor; }

    Position resolve(int sw, int sh, int cw, int ch, int px, int py) {
        int x;
        int y;
        switch (kind) {
            case CENTER -> {
                x = (sw - cw) / 2 + offsetX;
                y = (sh - ch) / 2 + offsetY;
            }
            case POINT -> {
                x = pointX + offsetX;
                y = pointY + offsetY;
            }
            case POINTER -> {
                x = px + offsetX;
                y = py + offsetY;
                if (x + cw > sw) x = px - offsetX - cw;
                if (y + ch > sh) y = py - offsetY - ch;
            }
            default -> {
                Position p = anchorPosition(side, cw, ch);
                if (!fitsMain(side, p, sw, sh, cw, ch)) {
                    Side opposite = opposite(side);
                    Position q = anchorPosition(opposite, cw, ch);
                    if (fitsMain(opposite, q, sw, sh, cw, ch)) p = q;
                }
                x = p.x();
                y = p.y();
            }
        }
        x = Math.max(0, Math.min(x, Math.max(0, sw - cw)));
        y = Math.max(0, Math.min(y, Math.max(0, sh - ch)));
        return new Position(x, y);
    }

    private Position anchorPosition(Side s, int cw, int ch) {
        int ax = anchor.absX();
        int ay = anchor.absY();
        int aw = anchor.getWidth();
        int ah = anchor.getHeight();
        return switch (s) {
            case BELOW -> new Position(ax + offsetX, ay + ah + gap + offsetY);
            case ABOVE -> new Position(ax + offsetX, ay - gap - ch + offsetY);
            case RIGHT -> new Position(ax + aw + gap + offsetX, ay + offsetY);
            case LEFT -> new Position(ax - gap - cw + offsetX, ay + offsetY);
        };
    }

    private static boolean fitsMain(Side s, Position p, int sw, int sh, int cw, int ch) {
        return switch (s) {
            case BELOW, ABOVE -> p.y() >= 0 && p.y() + ch <= sh;
            case RIGHT, LEFT -> p.x() >= 0 && p.x() + cw <= sw;
        };
    }

    private static Side opposite(Side s) {
        return switch (s) {
            case BELOW -> Side.ABOVE;
            case ABOVE -> Side.BELOW;
            case RIGHT -> Side.LEFT;
            case LEFT -> Side.RIGHT;
        };
    }
}