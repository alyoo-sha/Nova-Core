package org.mod.nova_core;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import org.mod.nova_core.menu.NovaWorkbenchMenu;

@Mod.EventBusSubscriber(modid = Nova_core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NovaCoreCommands {
    private NovaCoreCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("novacore")
                .then(Commands.literal("workbench").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    NetworkHooks.openScreen(player, new SimpleMenuProvider(
                            (id, inventory, p) -> new NovaWorkbenchMenu(id, inventory),
                            Component.translatable("menu.nova_core.workbench")));
                    return 1;
                })));
    }
}