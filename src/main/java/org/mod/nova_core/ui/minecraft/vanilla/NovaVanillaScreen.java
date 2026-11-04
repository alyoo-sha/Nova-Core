package org.mod.nova_core.ui.minecraft.vanilla;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.minecraft.container.NovaContainerScreen;
import org.mod.nova_core.ui.minecraft.container.SlotAccess;

// Экран ванильного меню: слоты механизма и игрока переставляются на клиенте под раскладку
// A screen for a vanilla menu: the machine and player slots are moved on the client to the layout
public abstract class NovaVanillaScreen<M extends AbstractContainerMenu> extends NovaContainerScreen<M> {
    protected NovaVanillaScreen(M menu, Inventory inventory, Component title, MachineLayout layout) {
        super(menu, inventory, title, layout);
    }

    // Позиции предметов слотов механизма в его панели, в порядке индексов слотов (слоты инвентаря игрока не входят)
    // Item positions of the machine slots in its panel, in slot index order (the player inventory slots are not included)
    protected abstract int[][] machineSlotPositions();

    // Индексы слотов результата: у них большая рамка / Indices of result slots: they get a large frame
    protected int[] outputSlotIndices() {
        return new int[0];
    }

    @Override
    protected boolean isLargeSlot(Slot slot) {
        for (int index : outputSlotIndices()) {
            if (index == slot.index) return true;
        }
        return false;
    }

    @Override
    protected void positionSlots(MachineLayout layout) {
        int[][] machine = machineSlotPositions();
        int machineIndex = 0;
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                // 0..8 хотбар, 9..35 основной инвентарь / 0..8 hotbar, 9..35 main inventory
                int cs = slot.getContainerSlot();
                if (cs < 9) SlotAccess.setPosition(slot, layout.inventoryX(cs), layout.hotbarY());
                else SlotAccess.setPosition(slot, layout.inventoryX((cs - 9) % 9), layout.inventoryY((cs - 9) / 9));
            } else {
                if (machineIndex < machine.length) {
                    int[] p = machine[machineIndex];
                    SlotAccess.setPosition(slot, layout.machineX(p[0]), layout.machineY(p[1]));
                }
                machineIndex++;
            }
        }
    }
}