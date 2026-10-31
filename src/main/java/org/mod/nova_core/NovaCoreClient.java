package org.mod.nova_core;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.mod.nova_core.ui.minecraft.DemoScreen;

public final class NovaCoreClient {
    // Создаётся сразу / Created eagerly
    public static final KeyMapping OPEN_DEMO = new KeyMapping(
            "key.nova_core.open_demo", GLFW.GLFW_KEY_K, "key.categories.nova_core");

    private NovaCoreClient() {}

    // Шина мода: регистрация клавиши / Mod bus: key registration
    @Mod.EventBusSubscriber(modid = "nova_core", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModEvents {
        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_DEMO);
        }
    }

    // Шина Forge: проверка нажатия каждый тик / Forge bus: check the press every tick
    @Mod.EventBusSubscriber(modid = "nova_core", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ForgeEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            // consumeClick() в цикле, чтобы не копились нажатия / loop consumeClick() so presses don't pile up
            while (OPEN_DEMO.consumeClick()) {
                // Открываем только из мира и без другого экрана / Open only in-world with no other screen
                if (mc.level != null && mc.screen == null) mc.setScreen(new DemoScreen());
            }
        }
    }
}