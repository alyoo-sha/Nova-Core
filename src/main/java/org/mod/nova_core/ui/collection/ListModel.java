package org.mod.nova_core.ui.collection;

import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.state.Signal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

// Изменяемый список, который уведомляет об изменениях / A mutable list that notifies about changes
public final class ListModel<T> implements ItemSource<T> {
    private final List<T> items = new ArrayList<>();
    // Растёт при каждом изменении; на него можно подписаться как на обычный сигнал
    // Grows on every change; it can be observed like any signal
    private final Signal<Integer> revision = Signal.of(0);

    public ListModel() {}

    public ListModel(Collection<? extends T> initial) {
        items.addAll(initial);
    }

    @Override
    public int size() { return items.size(); }

    @Override
    public T get(int index) { return items.get(index); }

    @Override
    public ReadableSignal<Integer> changes() { return revision; }

    // Копия текущего содержимого, которую нельзя менять / A copy of the current content that cannot be modified
    public List<T> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public void add(T item) {
        items.add(item);
        changed();
    }

    public void add(int index, T item) {
        items.add(index, item);
        changed();
    }

    public void addAll(Collection<? extends T> more) {
        if (more.isEmpty()) return;
        items.addAll(more);
        changed();
    }

    public T remove(int index) {
        T old = items.remove(index);
        changed();
        return old;
    }

    public boolean removeItem(T item) {
        boolean removed = items.remove(item);
        if (removed) changed();
        return removed;
    }

    public T replace(int index, T item) {
        T old = items.set(index, item);
        changed();
        return old;
    }

    // Заменить всё содержимое / Replace the whole content
    public void setAll(Collection<? extends T> next) {
        items.clear();
        items.addAll(next);
        changed();
    }

    public void clear() {
        if (items.isEmpty()) return;
        items.clear();
        changed();
    }

    // Несколько изменений с одним уведомлением. Не сохраняйте переданный список после возврата
    // Several changes with a single notification. Do not keep the passed list after returning
    public void modify(Consumer<? super List<T>> action) {
        try {
            action.accept(items);
        } finally {
            changed();
        }
    }

    private void changed() {
        revision.update(r -> r + 1);
    }
}