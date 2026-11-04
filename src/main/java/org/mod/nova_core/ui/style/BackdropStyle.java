package org.mod.nova_core.ui.style;

// Фон за экраном вместо ванильного затемнения / The backdrop behind a screen instead of the vanilla dimming
public record BackdropStyle(Gradient fill) {
    public static final BackdropStyle DEFAULT = new BackdropStyle(
            new Gradient(0xC0101010, 0xC0101010, 0xD0101010, 0xD0101010));
}