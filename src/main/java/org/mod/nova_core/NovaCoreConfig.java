package org.mod.nova_core;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.mod.nova_core.ui.theme.ThemeManager;
import org.mod.nova_core.ui.theme.Themes;

// Клиентский конфиг: выбранная тема запоминается между запусками / The client config: the chosen theme is remembered between launches
@Mod.EventBusSubscriber(modid = Nova_core.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NovaCoreConfig {
    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.ConfigValue<String> THEME;
    private static boolean tracking = false;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        THEME = builder.comment("UI theme: glass, dark or light").define("theme", "glass");
        SPEC = builder.build();
    }

    private NovaCoreConfig() {}

    @SubscribeEvent
    public static void onConfig(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        ThemeManager.set(Themes.byName(THEME.get()));
        if (!tracking) {
            tracking = true;
            ThemeManager.signal().subscribe(theme -> {
                THEME.set(theme.name());
                SPEC.save();
            });
        }
    }
}