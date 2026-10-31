package org.mod.nova_core.ui.graphics;

import java.util.Objects;

// Текст для UI. Платформенная реализация (например, обёртка над Component) лежит в пакете minecraft
// Text for the UI. A platform implementation (e.g. a Component wrapper) lives in the minecraft package
public interface UiText {
    // Простая строка для логов и запасного варианта / Plain string for logs and as a fallback
    String plain();

    static UiText literal(String value) {
        return new Literal(value);
    }

    record Literal(String value) implements UiText {
        public Literal {
            Objects.requireNonNull(value);
        }

        @Override
        public String plain() {
            return value;
        }
    }
}