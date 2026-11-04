package org.mod.nova_core.ui.collection;

import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.state.Signal;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

// Набор выбранных индексов с логикой кликов: обычный, Ctrl (переключить), Shift (диапазон от опорной точки)
// A set of selected indices with click logic: plain, Ctrl (toggle), Shift (a range from the anchor)
public final class MultiSelection {
    private final TreeSet<Integer> selected = new TreeSet<>();
    // Растёт при каждом изменении / Grows on every change
    private final Signal<Integer> revision = Signal.of(0);
    private int anchor = -1;

    public boolean contains(int index) { return selected.contains(index); }

    public int size() { return selected.size(); }

    public boolean isEmpty() { return selected.isEmpty(); }

    // Индексы по возрастанию / Indices in ascending order
    public List<Integer> indices() { return new ArrayList<>(selected); }

    public ReadableSignal<Integer> changes() { return revision; }

    public void click(int index, boolean ctrl, boolean shift) {
        if (shift && anchor >= 0) {
            int from = Math.min(anchor, index);
            int to = Math.max(anchor, index);
            if (!ctrl) selected.clear();
            for (int i = from; i <= to; i++) selected.add(i);
        } else if (ctrl) {
            if (!selected.remove(index)) selected.add(index);
            anchor = index;
        } else {
            selected.clear();
            selected.add(index);
            anchor = index;
        }
        changed();
    }

    public void selectAll(int count) {
        selected.clear();
        for (int i = 0; i < count; i++) selected.add(i);
        changed();
    }

    public void clear() {
        if (selected.isEmpty()) return;
        selected.clear();
        anchor = -1;
        changed();
    }

    // После сокращения источника убрать индексы за его пределами / After the source shrinks, drop indices beyond its end
    public void retainBelow(int size) {
        if (selected.isEmpty() || selected.last() < size) return;
        selected.removeIf(i -> i >= size);
        if (anchor >= size) anchor = -1;
        changed();
    }

    private void changed() {
        revision.update(r -> r + 1);
    }
}