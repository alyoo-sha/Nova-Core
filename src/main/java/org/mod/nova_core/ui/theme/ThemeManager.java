package org.mod.nova_core.ui.theme;

import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.state.Signal;

import java.util.Objects;

// Текущая тема приложения; хранится в Signal, поэтому на неё можно подписываться
// The application's current theme; stored in a Signal, so it can be observed
public final class ThemeManager {
    private static final Signal<Theme> CURRENT = Signal.of(Themes.DARK);

    private ThemeManager() {}

    public static Theme current() { return CURRENT.peek(); }

    public static ReadableSignal<Theme> signal() { return CURRENT; }

    public static void set(Theme theme) {
        CURRENT.set(Objects.requireNonNull(theme));
    }
}