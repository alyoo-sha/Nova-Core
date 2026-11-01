package org.mod.nova_core.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

// Меню механизма: сначала слоты механизма, затем 36 слотов инвентаря игрока в позициях MachineLayout.
// В конструкторе потомка: добавьте слоты механизма, затем вызовите addPlayerInventory
// A machine menu: machine slots first, then the 36 player inventory slots at MachineLayout positions.
// In a subclass constructor: add the machine slots, then call addPlayerInventory
public abstract class NovaMachineMenu extends AbstractContainerMenu {
    private final MachineLayout layout;
    private int machineSlotCount = -1;

    protected NovaMachineMenu(MenuType<?> type, int containerId, MachineLayout layout) {
        super(type, containerId);
        this.layout = layout;
    }

    public MachineLayout layout() { return layout; }

    public int machineSlotCount() { return machineSlotCount; }

    protected final void addPlayerInventory(Inventory inventory) {
        machineSlotCount = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, layout.inventoryX(col), layout.inventoryY(row)));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, layout.inventoryX(col), layout.hotbarY()));
        }
    }

    // Shift+клик: из механизма в инвентарь, из инвентаря в механизм, иначе между основным и хотбаром
    // Shift+click: from the machine into the inventory, from the inventory into the machine, otherwise between the main part and the hotbar
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (machineSlotCount < 0) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int total = slots.size();
        int mainEnd = machineSlotCount + 27;

        if (index < machineSlotCount) {
            if (!moveItemStackTo(stack, machineSlotCount, total, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, original);
        } else if (!moveItemStackTo(stack, 0, machineSlotCount, false)) {
            boolean moved = index < mainEnd
                    ? moveItemStackTo(stack, mainEnd, total, false)
                    : moveItemStackTo(stack, machineSlotCount, mainEnd, false);
            if (!moved) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}