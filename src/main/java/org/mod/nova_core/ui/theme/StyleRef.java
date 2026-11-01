package org.mod.nova_core.ui.theme;

import org.mod.nova_core.ui.style.StyleKey;

// Стиль виджета: собственное значение, если задано, иначе значение текущей темы. Читается каждый раз заново,
// поэтому смена темы подхватывается сама
// A widget's style: its own value if set, otherwise the current theme's value. Read every time,
// so a theme change is picked up automatically
public final class StyleRef<T> {
    private final StyleKey<T> key;
    private T override = null;

    public StyleRef(StyleKey<T> key) {
        this.key = key;
    }

    public T get() {
        return override != null ? override : ThemeManager.current().get(key);
    }

    // null возвращает стиль темы / null goes back to the theme's style
    public void set(T value) { this.override = value; }

    public boolean isOverridden() { return override != null; }
}