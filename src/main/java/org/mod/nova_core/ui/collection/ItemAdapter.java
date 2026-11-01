package org.mod.nova_core.ui.collection;

import org.mod.nova_core.ui.component.Widget;

import java.util.function.Supplier;

// Как создавать и заполнять виджеты ячеек. Виджеты переиспользуются, поэтому bind должен полностью
// обновлять внешний вид под новый элемент. bind может вызываться, когда виджет ещё не смонтирован,
// поэтому не используйте в нём scope()
// How to create and fill cell widgets. Widgets are reused, so bind must fully update the look for the new item.
// bind may run while the widget is not mounted yet, so do not use scope() inside it
public interface ItemAdapter<T> {
    Widget create();

    void bind(Widget view, T item, int index, boolean selected);

    // Виджет уходит в пул; освободите ссылки на элемент / The widget goes to the pool; release references to the item
    default void recycle(Widget view) {}

    @FunctionalInterface
    interface Binder<T, W extends Widget> {
        void bind(W view, T item, int index, boolean selected);
    }

    // Адаптер из фабрики виджета и функции привязки; типы лучше задавать явно: ItemAdapter.<Item, MyCell>of(...)
    // An adapter from a widget factory and a bind function; better to give the types explicitly: ItemAdapter.<Item, MyCell>of(...)
    static <T, W extends Widget> ItemAdapter<T> of(Supplier<? extends W> factory, Binder<T, W> binder) {
        return new ItemAdapter<>() {
            @Override
            public Widget create() {
                return factory.get();
            }

            @Override
            @SuppressWarnings("unchecked")
            public void bind(Widget view, T item, int index, boolean selected) {
                binder.bind((W) view, item, index, selected);
            }
        };
    }
}