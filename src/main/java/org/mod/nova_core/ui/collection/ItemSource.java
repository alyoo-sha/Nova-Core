package org.mod.nova_core.ui.collection;

import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.state.Signal;

import java.util.List;
import java.util.Objects;

// Источник элементов для списков и сеток / A source of items for lists and grids
public interface ItemSource<T> {
    int size();

    T get(int index);

    // Меняется при любом изменении содержимого; значение не важно, важен сам факт изменения
    // Changes on any content change; the value does not matter, only the fact of the change does
    ReadableSignal<?> changes();

    // Источник из сигнала со списком, например из Computed с фильтром. Для Computed с большими списками задавайте
    // равенство (a, b) -> false: сравнивать списки целиком дорого, а пересчёт и так происходит только при изменении зависимостей
    // A source from a signal holding a list, e.g. a filtering Computed. For a Computed with big lists use the equality
    // (a, b) -> false: comparing whole lists is expensive, and a recompute only happens when a dependency changed
    static <T> ItemSource<T> ofSignal(ReadableSignal<? extends List<? extends T>> list) {
        Objects.requireNonNull(list);
        return new ItemSource<>() {
            @Override
            public int size() { return list.peek().size(); }

            @Override
            public T get(int index) { return list.peek().get(index); }

            @Override
            public ReadableSignal<?> changes() { return list; }
        };
    }

    // Неизменяемый источник / An immutable source
    static <T> ItemSource<T> of(List<? extends T> items) {
        List<T> copy = List.copyOf(items);
        Signal<Integer> never = Signal.of(0);
        return new ItemSource<>() {
            @Override
            public int size() { return copy.size(); }

            @Override
            public T get(int index) { return copy.get(index); }

            @Override
            public ReadableSignal<?> changes() { return never; }
        };
    }
}