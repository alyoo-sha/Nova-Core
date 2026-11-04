package org.mod.nova_core.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreativeModeInventoryScreen.class)
public interface CreativeScreenAccessor {
    @Accessor("scrollOffs")
    float nova$getScrollOffs();

    @Accessor("searchBox")
    EditBox nova$getSearchBox();

    @Accessor("selectedTab")
    static CreativeModeTab nova$getSelectedTab() {
        throw new AssertionError();
    }
}