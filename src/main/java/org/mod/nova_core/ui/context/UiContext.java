package org.mod.nova_core.ui.context;

import java.util.Objects;

// Хранит установленную платформу; ставится один раз при старте клиента
// Holds the installed platform; installed once on client startup
public final class UiContext {
    private static volatile UiPlatform platform = null;

    private UiContext() {}

    public static void install(UiPlatform p) {
        platform = Objects.requireNonNull(p);
    }

    public static UiPlatform platform() {
        UiPlatform p = platform;
        if (p == null) throw new IllegalStateException("Nova Core UI platform is not installed");
        return p;
    }
}