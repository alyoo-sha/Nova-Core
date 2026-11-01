package org.mod.nova_core.ui.theme;

import org.mod.nova_core.ui.style.StyleKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// Неизменяемый набор стилей. Ключ, которого нет в теме, даёт своё значение по умолчанию
// An immutable set of styles. A key missing from the theme yields its own default value
public final class Theme {
    private final String name;
    private final Map<StyleKey<?>, Object> values;

    private Theme(String name, Map<StyleKey<?>, Object> values) {
        this.name = name;
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
    }

    public static Builder builder(String name) {
        return new Builder(name, Map.of());
    }

    public String name() { return name; }

    public <T> T get(StyleKey<T> key) {
        Object value = values.get(key);
        return value == null ? key.defaultValue() : key.cast(value);
    }

    public boolean has(StyleKey<?> key) { return values.containsKey(key); }

    // Новая тема с заменённым стилем; эта тема не меняется / A new theme with one style replaced; this theme is unchanged
    public <T> Theme with(StyleKey<T> key, T value) {
        return toBuilder(name).set(key, value).build();
    }

    public Builder toBuilder(String newName) {
        return new Builder(newName, values);
    }

    @Override
    public String toString() { return "Theme[" + name + "]"; }

    public static final class Builder {
        private final String name;
        private final Map<StyleKey<?>, Object> values = new HashMap<>();

        private Builder(String name, Map<StyleKey<?>, Object> initial) {
            this.name = Objects.requireNonNull(name);
            this.values.putAll(initial);
        }

        public <T> Builder set(StyleKey<T> key, T value) {
            values.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
            return this;
        }

        public Theme build() {
            return new Theme(name, values);
        }
    }
}