package org.mod.nova_core.ui.minecraft.container.interaction;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;

// Точка расширения для модулей работы с инвентарём. Все методы вызываются на клиентском потоке.
// slot равен null, если курсор не над слотом. Возврат true означает «событие поглощено»: ванильная обработка пропускается.
// Для press/drag/scroll вызов идёт только когда нет открытых оверлеев и экран не отключил расширения.
// release приходит всегда, чтобы модуль мог завершить жест.
// An extension point for inventory-interaction modules. All methods run on the client thread.
// slot is null when the cursor is not over a slot. Returning true means "event consumed": vanilla handling is skipped.
// press/drag/scroll are delivered only when no overlay is open and the screen has not opted out.
// release is always delivered so the module can finish its gesture.
public interface InventoryInteraction {

    // Порядок вызова: больше значение, раньше вызов / Call order: a higher value is called earlier
    default int priority() { return 0; }

    // Для каких экранов модуль работает / Which screens the module works with
    default boolean appliesTo(AbstractContainerScreen<?> screen) { return true; }

    default boolean onSlotPress(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) { return false; }

    default boolean onSlotDrag(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) { return false; }

    default boolean onSlotRelease(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) { return false; }

    default boolean onSlotScroll(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) { return false; }
}