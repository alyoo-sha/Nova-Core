package org.mod.nova_core.ui.input;

// Тип события; параметр связывает тип с классом события, поэтому слушатели типобезопасны
// Event type; the parameter ties the type to the event class, so listeners are type-safe
public final class EventType<E extends UiEvent> {
    public static final EventType<PointerEvent> POINTER_DOWN = new EventType<>(PointerEvent.class, "pointer_down");
    public static final EventType<PointerEvent> POINTER_UP = new EventType<>(PointerEvent.class, "pointer_up");
    public static final EventType<PointerEvent> POINTER_MOVE = new EventType<>(PointerEvent.class, "pointer_move");
    public static final EventType<PointerEvent> POINTER_DRAG = new EventType<>(PointerEvent.class, "pointer_drag");
    public static final EventType<PointerEvent> POINTER_SCROLL = new EventType<>(PointerEvent.class, "pointer_scroll");
    public static final EventType<KeyboardEvent> KEY_DOWN = new EventType<>(KeyboardEvent.class, "key_down");
    public static final EventType<KeyboardEvent> KEY_UP = new EventType<>(KeyboardEvent.class, "key_up");
    public static final EventType<KeyboardEvent> CHAR = new EventType<>(KeyboardEvent.class, "char");
    public static final EventType<PointerEvent> POINTER_ENTER = new EventType<>(PointerEvent.class, "pointer_enter");
    public static final EventType<PointerEvent> POINTER_LEAVE = new EventType<>(PointerEvent.class, "pointer_leave");
    public static final EventType<PointerEvent> POINTER_CANCEL = new EventType<>(PointerEvent.class, "pointer_cancel");

    private final Class<E> eventClass;
    private final String name;

    private EventType(Class<E> eventClass, String name) {
        this.eventClass = eventClass;
        this.name = name;
    }

    public E cast(UiEvent event) { return eventClass.cast(event); }
    public String getName() { return name; }

    @Override
    public String toString() { return name; }
}