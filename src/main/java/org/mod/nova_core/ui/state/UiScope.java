package org.mod.nova_core.ui.state;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Владеет подписками, эффектами и вычислениями: close() отменяет всё разом, после чего областью можно пользоваться снова
// Owns subscriptions, effects and computeds: close() cancels everything at once, after which the scope can be reused
public final class UiScope implements Subscription {
    private final ArrayList<Subscription> owned = new ArrayList<>();

    public <S extends Subscription> S own(S subscription) {
        owned.add(Objects.requireNonNull(subscription));
        return subscription;
    }

    // Выполнить действие при закрытии / Run an action on close
    public void onClose(Runnable action) {
        owned.add(Subscription.of(action));
    }

    public Effect effect(Runnable body) {
        return own(Effect.create(body));
    }

    public <T> Computed<T> computed(Supplier<? extends T> compute) {
        return own(new Computed<T>(compute));
    }

    // sink вызывается сразу и при каждом изменении; чтения внутри sink ничего не отслеживают
    // sink runs immediately and on every change; reads inside sink track nothing
    public <T> Effect observe(ReadableSignal<T> source, Consumer<? super T> sink) {
        return effect(() -> {
            T value = source.get();
            Signals.untracked(() -> sink.accept(value));
        });
    }

    public <T> Subscription subscribe(ReadableSignal<T> source, Consumer<? super T> sink) {
        return own(source.subscribe(sink));
    }

    // Отменяет всё в обратном порядке создания; первая ошибка пробрасывается после отмены остального
    // Cancels everything in reverse creation order; the first error is rethrown after the rest is cancelled
    public void close() {
        RuntimeException first = null;
        for (int i = owned.size() - 1; i >= 0; i--) {
            try {
                owned.get(i).cancel();
            } catch (RuntimeException ex) {
                if (first == null) first = ex;
            }
        }
        owned.clear();
        if (first != null) throw first;
    }

    @Override
    public void cancel() {
        close();
    }
}