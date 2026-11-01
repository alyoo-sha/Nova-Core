package org.mod.nova_core.ui.style;

import java.util.Objects;

// Ключ стиля: имя, тип и значение по умолчанию. Сравнивается по ссылке, объявляйте как static final
// A style key: name, type and default value. Compared by reference, declare as static final
public final class StyleKey<T> {
    private final String name;
    private final Class<T> type;
    private final T defaultValue;

    private StyleKey(String name, Class<T> type, T defaultValue) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    // Имя лучше с префиксом мода: "nova_core:button" / Prefix the name with the mod id: "nova_core:button"
    public static <T> StyleKey<T> of(String name, Class<T> type, T defaultValue) {
        return new StyleKey<>(Objects.requireNonNull(name), Objects.requireNonNull(type), Objects.requireNonNull(defaultValue));
    }

    public String name() { return name; }
    public T defaultValue() { return defaultValue; }
    public T cast(Object value) { return type.cast(value); }

    @Override
    public String toString() { return name; }
}