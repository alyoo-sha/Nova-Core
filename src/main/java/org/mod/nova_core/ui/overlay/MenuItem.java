package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.graphics.UiText;

public record MenuItem(UiText label, Runnable action, boolean enabled) {
    public static MenuItem of(UiText label, Runnable action) {
        return new MenuItem(label, action, true);
    }

    // Неактивный пункт: виден, но не нажимается / A disabled item: visible but not clickable
    public static MenuItem disabled(UiText label) {
        return new MenuItem(label, () -> { }, false);
    }
}