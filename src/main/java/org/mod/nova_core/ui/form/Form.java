package org.mod.nova_core.ui.form;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.navigation.FocusNavigator;
import org.mod.nova_core.ui.state.Computed;
import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.state.Signals;
import org.mod.nova_core.ui.state.Subscription;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Группа полей с общей проверкой, отправкой и сбросом. Храните форму в экране, а не в build():
// build() вызывается заново при ресайзе, а значения должны сохраняться
// A group of fields with shared validation, submit and reset. Keep the form in the screen, not in build():
// build() runs again on resize, and the values must survive it
public final class Form implements Subscription {
    private final List<FormField<?>> fields = new ArrayList<>();
    private final Map<FormField<?>, Widget> inputs = new IdentityHashMap<>();
    // Растёт при добавлении поля, чтобы вычисляемые значения пересчитались / Grows when a field is added so the computed values recompute
    private final Signal<Integer> structure = Signal.of(0);
    private final Signal<Boolean> submitAttempted = Signal.of(false);

    private final Computed<Boolean> valid = new Computed<>(() -> {
        structure.get();
        for (FormField<?> f : fields) {
            if (f.validity().get() != null) return false;
        }
        return true;
    });

    private final Computed<Boolean> dirty = new Computed<>(() -> {
        structure.get();
        for (FormField<?> f : fields) {
            if (Boolean.TRUE.equals(f.dirty().get())) return true;
        }
        return false;
    });

    // Новое поле с начальным значением и проверками; первая ошибка по порядку побеждает
    // A new field with an initial value and validators; the first error in order wins
    @SafeVarargs
    public final <T> FormField<T> field(T initial, Validator<? super T>... validators) {
        return fieldFor(Signal.of(initial), validators);
    }

    // То же для готового сигнала, который делится с другими виджетами / The same for an existing signal shared with other widgets
    @SafeVarargs
    public final <T> FormField<T> fieldFor(Signal<T> signal, Validator<? super T>... validators) {
        FormField<T> field = new FormField<>(signal, List.of(validators), submitAttempted);
        fields.add(field);
        structure.update(n -> n + 1);
        return field;
    }

    public ReadableSignal<Boolean> valid() { return valid; }

    public ReadableSignal<Boolean> dirty() { return dirty; }

    public ReadableSignal<Boolean> submitAttempted() { return submitAttempted; }

    public boolean isValid() { return valid.peek(); }

    // Связывает поле с виджетом ввода, чтобы можно было сфокусировать первое неверное; FormRow делает это сам
    // Ties a field to its input widget so the first invalid one can be focused; FormRow does it itself
    public void registerInput(FormField<?> field, Widget input) {
        inputs.put(field, input);
    }

    // Показывает все ошибки; если всё верно, вызывает onValid и возвращает true / Shows all errors; if all is valid, calls onValid and returns true
    public boolean submit(Consumer<? super Form> onValid) {
        submitAttempted.set(true);
        if (!valid.peek()) {
            focusFirstInvalid();
            return false;
        }
        onValid.accept(this);
        return true;
    }

    public boolean focusFirstInvalid() {
        for (FormField<?> f : fields) {
            if (f.isValid()) continue;
            Widget input = inputs.get(f);
            if (input != null) return FocusNavigator.focus(input);
        }
        return false;
    }

    public void reset() {
        Signals.batch(() -> {
            for (FormField<?> f : fields) f.reset();
            submitAttempted.set(false);
        });
    }

    @Override
    public void cancel() {
        valid.cancel();
        dirty.cancel();
        for (FormField<?> f : fields) f.cancel();
    }
}