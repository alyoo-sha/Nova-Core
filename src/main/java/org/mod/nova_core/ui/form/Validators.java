package org.mod.nova_core.ui.form;

import org.mod.nova_core.ui.graphics.UiText;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class Validators {
    private Validators() {}

    public static <T> Validator<T> custom(Predicate<? super T> ok, UiText message) {
        return v -> ok.test(v) ? null : message;
    }

    public static Validator<String> required(UiText message) {
        return v -> v == null || v.isBlank() ? message : null;
    }

    public static Validator<String> minLength(int min, UiText message) {
        return v -> v == null || v.length() < min ? message : null;
    }

    public static Validator<String> maxLength(int max, UiText message) {
        return v -> v != null && v.length() > max ? message : null;
    }

    // Строка должна целиком совпасть с регулярным выражением / The whole string must match the regex
    public static Validator<String> matches(String regex, UiText message) {
        Pattern pattern = Pattern.compile(regex);
        return v -> v != null && pattern.matcher(v).matches() ? null : message;
    }

    public static Validator<String> intInRange(int min, int max, UiText message) {
        return v -> {
            try {
                int n = Integer.parseInt(v.trim());
                return n >= min && n <= max ? null : message;
            } catch (RuntimeException ex) {
                return message;
            }
        };
    }

    public static Validator<Boolean> isTrue(UiText message) {
        return v -> Boolean.TRUE.equals(v) ? null : message;
    }
}