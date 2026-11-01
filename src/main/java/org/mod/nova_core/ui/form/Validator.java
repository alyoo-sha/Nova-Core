package org.mod.nova_core.ui.form;

import org.mod.nova_core.ui.graphics.UiText;

@FunctionalInterface
public interface Validator<T> {
    // null = значение допустимо, иначе текст ошибки / null = the value is valid, otherwise the error text
    UiText validate(T value);
}