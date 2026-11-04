package org.mod.nova_core.menu.minecraft;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.ArrowWidget;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.PlayerPreview;
import org.mod.nova_core.ui.minecraft.container.SlotAccess;
import org.mod.nova_core.ui.minecraft.vanilla.NovaVanillaScreen;

import java.util.HashMap;
import java.util.Map;

// Инвентарь выживания (клавиша E). Верхняя панель: броня, предпросмотр игрока, второй слот, крафт 2x2.
// Меню InventoryMenu живёт всё время игры, поэтому исходные позиции слотов возвращаются при закрытии
// The survival inventory (the E key). The top panel: armor, the player preview, the offhand slot, 2x2 crafting.
// InventoryMenu lives for the whole game, so the original slot positions are restored on close
public class NovaInventoryScreen extends NovaVanillaScreen<InventoryMenu> {
    public static final MachineLayout LAYOUT = new MachineLayout(200, 98);

    private final Map<Slot, int[]> originalPositions = new HashMap<>();

    public NovaInventoryScreen(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, LAYOUT);
    }

    // Позиции задаются в positionSlots по номерам слотов InventoryMenu / Positions are set in positionSlots by InventoryMenu slot numbers
    @Override
    protected int[][] machineSlotPositions() {
        return new int[0][];
    }

    @Override
    protected int[] outputSlotIndices() {
        return new int[] {0};
    }

    // Номера слотов: 0 результат, 1..4 сетка 2x2, 5..8 броня (голова, грудь, ноги, ступни), 9..35 основной инвентарь,
    // 36..44 хотбар, 45 второй слот
    // Slot numbers: 0 result, 1..4 the 2x2 grid, 5..8 armor (head, chest, legs, feet), 9..35 main inventory,
    // 36..44 hotbar, 45 offhand
    @Override
    protected void positionSlots(MachineLayout layout) {
        if (originalPositions.isEmpty()) {
            for (Slot slot : menu.slots) originalPositions.put(slot, new int[] {slot.x, slot.y});
        }
        for (Slot slot : menu.slots) {
            int i = slot.index;
            if (i == 0) {
                SlotAccess.setPosition(slot, layout.machineX(171), layout.machineY(38));
            } else if (i <= 4) {
                int cell = i - 1;
                SlotAccess.setPosition(slot, layout.machineX(101 + (cell % 2) * 18), layout.machineY(29 + (cell / 2) * 18));
            } else if (i <= 8) {
                SlotAccess.setPosition(slot, layout.machineX(9), layout.machineY(19 + (i - 5) * 18));
            } else if (i <= 35) {
                int m = i - 9;
                SlotAccess.setPosition(slot, layout.inventoryX(m % 9), layout.inventoryY(m / 9));
            } else if (i <= 44) {
                SlotAccess.setPosition(slot, layout.inventoryX(i - 36), layout.hotbarY());
            } else if (i == 45) {
                SlotAccess.setPosition(slot, layout.machineX(101), layout.machineY(73));
            }
        }
    }

    @Override
    protected void buildMachine(Panel panel) {
        panel.add(new PlayerPreview(30, 18, 62, 76));
        panel.add(new ArrowWidget(140, 39));
    }

    @Override
    public void removed() {
        for (Map.Entry<Slot, int[]> entry : originalPositions.entrySet()) {
            SlotAccess.setPosition(entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
        }
        originalPositions.clear();
        super.removed();
    }
}