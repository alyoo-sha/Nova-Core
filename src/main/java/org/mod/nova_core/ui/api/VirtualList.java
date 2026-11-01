package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.collection.ItemAdapter;
import org.mod.nova_core.ui.collection.ItemSource;
import org.mod.nova_core.ui.state.Signal;

// Вертикальный список строк одной высоты на всю ширину; это сетка из одной колонки
// A vertical list of equal-height rows spanning the full width; it is a grid with one column
public class VirtualList<T> extends VirtualGrid<T> {
    public VirtualList(int x, int y, int width, int height, int rowHeight,
                       ItemSource<T> source, ItemAdapter<T> adapter) {
        this(x, y, width, height, rowHeight, source, adapter, Signal.of(-1));
    }

    public VirtualList(int x, int y, int width, int height, int rowHeight,
                       ItemSource<T> source, ItemAdapter<T> adapter, Signal<Integer> selected) {
        super(x, y, width, height, source, adapter, selected);
        setFillWidth(true);
        setCellSize(1, rowHeight);
    }

    public VirtualList<T> setRowHeight(int rowHeight) {
        setCellSize(1, rowHeight);
        return this;
    }
}