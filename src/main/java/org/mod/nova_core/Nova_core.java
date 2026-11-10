package org.mod.nova_core;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static org.mod.nova_core.Nova_core.MOD_ID;

@Mod(MOD_ID)
public class Nova_core {
    public static final String MOD_ID = "nova_core";
    public static final String MOD_NAME = "Nova Core";
    public static IEventBus modEventBus;

    public Nova_core(FMLJavaModLoadingContext context) {
        modEventBus = context.getModEventBus();
        // Регистрация типов меню (верстак и будущие механизмы) / Registers the menu types (the workbench and future machines)
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, NovaCoreConfig.SPEC);
    }

    public static ResourceLocation parse(String path) {
        return ResourceLocation.parse(path);
    }

    public static ResourceLocation makeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static ResourceLocation makeId(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

}
