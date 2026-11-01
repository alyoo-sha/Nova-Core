package org.mod.nova_core.ui.dnd;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.state.Subscription;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

// Точка входа для регистрации источников и целей. Регистрация живёт, пока виджет смонтирован,
// и возобновляется при повторном монтировании. Один виджет: один источник и одна цель
// The entry point for registering sources and targets. A registration lives while the widget is mounted
// and resumes on a remount. One widget: one source and one target
public final class Dnd {
    private Dnd() {}

    public static Subscription source(Widget widget, DragSource source) {
        return attach(widget, Objects.requireNonNull(source), layer -> layer.sources);
    }

    public static Subscription target(Widget widget, DropTarget target) {
        return attach(widget, Objects.requireNonNull(target), layer -> layer.targets);
    }

    // Идёт ли перенос на экране, где лежит виджет / Is a drag in progress on the screen that holds the widget
    public static boolean isDragging(Widget any) {
        return DragLayer.of(any).isDragging();
    }

    // Текущий перенос или null / The current drag or null
    public static DragSession session(Widget any) {
        return DragLayer.of(any).session();
    }

    private static <T> Subscription attach(Widget widget, T value,
                                           Function<DragLayer, Map<Widget, Binding<T>>> registryOf) {
        Binding<T> binding = new Binding<>(widget, value);
        widget.whileMounted(() -> {
            if (!binding.active) return;
            Map<Widget, Binding<T>> registry = registryOf.apply(DragLayer.of(widget));
            registry.put(widget, binding);
            binding.registry = registry;
            widget.addCleanup(() -> {
                registry.remove(widget, binding);
                if (binding.registry == registry) binding.registry = null;
            });
        });
        return binding;
    }

    // Запись в реестре; она же подписка для отмены / A registry entry; also the subscription for cancelling
    static final class Binding<T> implements Subscription {
        final Widget widget;
        final T value;
        boolean active = true;
        Map<Widget, Binding<T>> registry = null;

        Binding(Widget widget, T value) {
            this.widget = widget;
            this.value = value;
        }

        @Override
        public void cancel() {
            active = false;
            Map<Widget, Binding<T>> r = registry;
            registry = null;
            if (r != null) r.remove(widget, this);
        }
    }
}