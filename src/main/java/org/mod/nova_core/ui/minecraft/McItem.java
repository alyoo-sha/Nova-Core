package org.mod.nova_core.ui.minecraft;

import net.minecraft.world.item.ItemStack;
import org.mod.nova_core.ui.graphics.UiItem;

public record McItem(ItemStack stack) implements UiItem {
    public static McItem of(ItemStack stack) {
        return new McItem(stack);
    }

    @Override
    public boolean isEmpty() {
        return stack.isEmpty();
    }
}