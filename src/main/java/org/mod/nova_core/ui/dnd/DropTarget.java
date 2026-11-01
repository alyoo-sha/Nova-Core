package org.mod.nova_core.ui.dnd;

// Цель переноса. Регистрируется через Dnd.target / A drop target. Registered via Dnd.target
public interface DropTarget {
    // Может ли цель принять эти данные. Вызывается каждый кадр во время переноса, держите дёшево
    // Can the target accept this data. Called every frame during a drag, keep it cheap
    boolean canAccept(DragSession session);

    // Данные отпущены над целью (x, y: экранные координаты) / The data was released over the target (x, y: screen coordinates)
    void onDrop(DragSession session, double x, double y);

    default void onDragEnter(DragSession session) {}

    default void onDragOver(DragSession session, double x, double y) {}

    // Вызывается и после drop, и при отмене, поэтому здесь всегда можно сбросить состояние наведения
    // Called after a drop and on cancel as well, so the hover state can always be reset here
    default void onDragLeave(DragSession session) {}
}