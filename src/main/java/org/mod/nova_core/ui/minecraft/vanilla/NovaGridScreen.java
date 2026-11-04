package org.mod.nova_core.ui.minecraft.vanilla;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.mod.nova_core.menu.MachineLayout;
import org.mod.nova_core.ui.api.Panel;

// Основа для контейнеров с сеткой слотов (сундук, шалкер, раздатчик, воронка): панель подгоняется под размер сетки
// A base for containers with a grid of slots (chest, shulker, dispenser, hopper): the panel fits the grid size
public abstract class NovaGridScreen<M extends AbstractContainerMenu> extends NovaVanillaScreen<M> {
    private final int cols;
    private final int rows;

    protected NovaGridScreen(M menu, Inventory inventory, Component title, int cols, int rows) {
        super(menu, inventory, title, new MachineLayout(Math.max(14 + cols * 18, 120), 25 + rows * 18));
        this.cols = cols;
        this.rows = rows;
    }

    @Override
    protected int[][] machineSlotPositions() {
        int left = (layout().machineWidth() - cols * 18) / 2 + 1;
        int[][] positions = new int[cols * rows][];
        for (int i = 0; i < positions.length; i++) {
            positions[i] = new int[] {left + (i % cols) * 18, 19 + (i / cols) * 18};
        }
        return positions;
    }

    // Дополнительные виджеты в потомках / Extra widgets in subclasses
    @Override
    protected void buildMachine(Panel panel) {}
}