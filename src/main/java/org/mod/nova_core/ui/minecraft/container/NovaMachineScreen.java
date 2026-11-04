package org.mod.nova_core.ui.minecraft.container;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.mod.nova_core.menu.NovaMachineMenu;

// Экран для меню на базе NovaMachineMenu: раскладку берёт из меню
// A screen for NovaMachineMenu-based menus: takes the layout from the menu
public abstract class NovaMachineScreen<M extends NovaMachineMenu> extends NovaContainerScreen<M> {
    protected NovaMachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.layout());
    }
}