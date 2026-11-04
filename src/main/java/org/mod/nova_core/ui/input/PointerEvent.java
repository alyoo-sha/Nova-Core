package org.mod.nova_core.ui.input;

// События мыши: нажатие, отпускание, движение, перетаскивание, колесо. Координаты в GUI-пикселях
// Mouse events: down, up, move, drag, scroll. Coordinates are in GUI pixels
public final class PointerEvent extends UiEvent {
    public static final int NO_BUTTON = -1;

    private final double x;
    private final double y;
    private final int button;
    private final double dragDeltaX;
    private final double dragDeltaY;
    private final double scrollDelta;
    private final int modifiers;

    private PointerEvent(EventType<PointerEvent> type, double x, double y, int button,
                         double dragDeltaX, double dragDeltaY, double scrollDelta, int modifiers) {
        super(type, type != EventType.POINTER_ENTER && type != EventType.POINTER_LEAVE
                && type != EventType.POINTER_CANCEL);
        this.x = x;
        this.y = y;
        this.button = button;
        this.dragDeltaX = dragDeltaX;
        this.dragDeltaY = dragDeltaY;
        this.scrollDelta = scrollDelta;
        this.modifiers = modifiers;
    }

    public static PointerEvent down(double x, double y, int button) { return down(x, y, button, 0); }

    public static PointerEvent down(double x, double y, int button, int modifiers) {
        return new PointerEvent(EventType.POINTER_DOWN, x, y, button, 0, 0, 0, modifiers);
    }

    public static PointerEvent up(double x, double y, int button) { return up(x, y, button, 0); }

    public static PointerEvent up(double x, double y, int button, int modifiers) {
        return new PointerEvent(EventType.POINTER_UP, x, y, button, 0, 0, 0, modifiers);
    }

    public static PointerEvent move(double x, double y) {
        return new PointerEvent(EventType.POINTER_MOVE, x, y, NO_BUTTON, 0, 0, 0, 0);
    }

    public static PointerEvent drag(double x, double y, int button, double dx, double dy) {
        return drag(x, y, button, dx, dy, 0);
    }

    public static PointerEvent drag(double x, double y, int button, double dx, double dy, int modifiers) {
        return new PointerEvent(EventType.POINTER_DRAG, x, y, button, dx, dy, 0, modifiers);
    }

    public static PointerEvent scroll(double x, double y, double delta) { return scroll(x, y, delta, 0); }

    public static PointerEvent scroll(double x, double y, double delta, int modifiers) {
        return new PointerEvent(EventType.POINTER_SCROLL, x, y, NO_BUTTON, 0, 0, delta, modifiers);
    }

    public static PointerEvent enter(double x, double y) {
        return new PointerEvent(EventType.POINTER_ENTER, x, y, NO_BUTTON, 0, 0, 0, 0);
    }

    public static PointerEvent leave(double x, double y) {
        return new PointerEvent(EventType.POINTER_LEAVE, x, y, NO_BUTTON, 0, 0, 0, 0);
    }

    // Нажатие отменено (например, начался перенос); сбросьте состояние «нажато»
    // The press was cancelled (e.g. a drag began); reset the pressed state
    public static PointerEvent cancel(double x, double y) {
        return new PointerEvent(EventType.POINTER_CANCEL, x, y, NO_BUTTON, 0, 0, 0, 0);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getButton() { return button; }
    public double getDragDeltaX() { return dragDeltaX; }
    public double getDragDeltaY() { return dragDeltaY; }
    public double getScrollDelta() { return scrollDelta; }
    public int getModifiers() { return modifiers; }

    // Были ли зажаты Shift, Ctrl, Alt; маски в Keys.MOD_* / Whether Shift, Ctrl, Alt were held; masks in Keys.MOD_*
    public boolean hasModifier(int mask) { return (modifiers & mask) != 0; }
}