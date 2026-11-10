package org.mod.nova_core.ui.minecraft.container.interaction;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntConsumer;

// Реестр модулей работы с инвентарём; его опрашивают экраны на базе NovaContainerScreen
// A registry of inventory-interaction modules; screens based on NovaContainerScreen query it
public final class InventoryInteractions {
    private static final List<InventoryInteraction> ENTRIES = new CopyOnWriteArrayList<>();

    private InventoryInteractions() {}

    // Добавляет модуль с учётом priority (стабильно: равные идут в порядке регистрации)
    // Adds a module respecting priority (stable: equals keep registration order)
    public static synchronized void register(InventoryInteraction interaction) {
        Objects.requireNonNull(interaction, "interaction");
        if (ENTRIES.contains(interaction)) return;
        int index = ENTRIES.size();
        for (int i = 0; i < ENTRIES.size(); i++) {
            if (ENTRIES.get(i).priority() < interaction.priority()) {
                index = i;
                break;
            }
        }
        ENTRIES.add(index, interaction);
    }

    public static synchronized void unregister(InventoryInteraction interaction) {
        ENTRIES.remove(interaction);
    }

    public static boolean press(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) {
        for (InventoryInteraction i : ENTRIES) {
            if (i.appliesTo(screen) && i.onSlotPress(screen, slot, event)) return true;
        }
        return false;
    }

    public static boolean drag(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) {
        for (InventoryInteraction i : ENTRIES) {
            if (i.appliesTo(screen) && i.onSlotDrag(screen, slot, event)) return true;
        }
        return false;
    }

    public static boolean scroll(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) {
        for (InventoryInteraction i : ENTRIES) {
            if (i.appliesTo(screen) && i.onSlotScroll(screen, slot, event)) return true;
        }
        return false;
    }

    // Отпускание получают все модули, чтобы каждый завершил свой жест; результат — «хоть один поглотил»
    // Every module receives the release so each can finish its gesture; the result is "at least one consumed"
    public static boolean release(AbstractContainerScreen<?> screen, @Nullable Slot slot, InventoryMouseEvent event) {
        boolean consumed = false;
        for (InventoryInteraction i : ENTRIES) {
            if (i.appliesTo(screen) && i.onSlotRelease(screen, slot, event)) consumed = true;
        }
        return consumed;
    }

    // Собирает номера слотов, которые модули просят подсветить / Collects the slot numbers that modules ask to highlight
    public static void collectHighlights(AbstractContainerScreen<?> screen, IntConsumer sink) {
        for (InventoryInteraction i : ENTRIES) {
            if (!i.appliesTo(screen)) continue;
            for (int id : i.highlightedSlots(screen)) sink.accept(id);
        }
    }
}