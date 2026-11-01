package org.mod.nova_core.ui.form;

import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.state.Computed;
import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.state.Signals;
import org.mod.nova_core.ui.state.Subscription;

import java.util.List;
import java.util.Objects;

// Поле формы: значение, проверка и состояние «тронуто». Создаётся через Form.field(...)
// A form field: the value, validation and the "touched" state. Created via Form.field(...)
public final class FormField<T> implements Subscription {
    private final Signal<T> value;
    private final T initial;
    private final Signal<Boolean> touched = Signal.of(false);
    // Первая найденная ошибка всегда / The first error found, always
    private final Computed<UiText> validity;
    // Та же ошибка, но только после касания или попытки отправки / The same error, but only after a touch or a submit attempt
    private final Computed<UiText> visibleError;
    private final Computed<Boolean> dirty;

    FormField(Signal<T> value, List<Validator<? super T>> validators, ReadableSignal<Boolean> submitAttempted) {
        this.value = Objects.requireNonNull(value);
        this.initial = value.peek();
        List<Validator<? super T>> list = List.copyOf(validators);

        this.validity = new Computed<>(() -> {
            T v = value.get();
            for (Validator<? super T> validator : list) {
                UiText error = validator.validate(v);
                if (error != null) return error;
            }
            return null;
        });
        this.visibleError = new Computed<>(() -> touched.get() || submitAttempted.get() ? validity.get() : null);
        this.dirty = new Computed<>(() -> !Objects.equals(value.get(), initial));
    }

    public Signal<T> signal() { return value; }

    public T get() { return value.peek(); }

    public void set(T next) { value.set(next); }

    // Ошибка для показа пользователю; null = нечего показывать / The error to show to the user; null = nothing to show
    public ReadableSignal<UiText> error() { return visibleError; }

    // Ошибка независимо от касаний / The error regardless of touches
    public ReadableSignal<UiText> validity() { return validity; }

    // Отличается ли значение от начального / Whether the value differs from the initial one
    public ReadableSignal<Boolean> dirty() { return dirty; }

    public boolean isValid() { return validity.peek() == null; }

    public boolean isTouched() { return touched.peek(); }

    public void touch() { touched.set(true); }

    public void reset() {
        Signals.batch(() -> {
            value.set(initial);
            touched.set(false);
        });
    }

    @Override
    public void cancel() {
        visibleError.cancel();
        validity.cancel();
        dirty.cancel();
    }
}