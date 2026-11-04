package org.mod.nova_core.ui.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffect;
import org.mod.nova_core.ui.graphics.UiImage;

// Спрайт из атласа, например значок эффекта / An atlas sprite, e.g. an effect icon
public record McSprite(TextureAtlasSprite sprite) implements UiImage {
    public static McSprite effect(MobEffect effect) {
        return new McSprite(Minecraft.getInstance().getMobEffectTextures().get(effect));
    }
}