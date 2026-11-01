package org.mod.nova_core.ui.dnd;

// Источник переноса. Регистрируется через Dnd.source / A drag source. Registered via Dnd.source
@FunctionalInterface
public interface DragSource {
    // Вызывается, когда курсор ушёл от точки нажатия (pressX, pressY, экранные координаты).
    // null = переноса нет, и обычная обработка мыши продолжается (например, прокрутка полосой)
    // Called when the cursor moved away from the press point (pressX, pressY, screen coordinates).
    // null = no drag, and the regular mouse handling goes on (e.g. scrollbar dragging)
    DragInfo begin(double pressX, double pressY);

    // Перенос закончился. dropped = true, если цель приняла данные / The drag ended. dropped = true if a target accepted the data
    default void onDragEnd(DragSession session, boolean dropped) {}
}