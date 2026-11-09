package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.collection.ItemAdapter;
import org.mod.nova_core.ui.collection.ItemSource;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.input.KeyboardEvent;
import org.mod.nova_core.ui.input.Keys;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.style.FieldStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;

// Вертикальный список со строками разной высоты: высота каждой строки считается функцией от элемента и ширины.
// Как VirtualGrid, создаёт виджеты только для видимых строк
// A vertical list with rows of different heights: each row's height comes from a function of the item and the width.
// Like VirtualGrid, it creates widgets only for the visible rows
public class VirtualFlowList<T> extends Widget {
    @FunctionalInterface
    public interface HeightFn<T> {
        int height(T item, int width);
    }

    private static final int OVERSCAN = 48;
    private static final int POOL_LIMIT = 64;
    private static final long DOUBLE_CLICK_NANOS = 350_000_000L;

    private final StyleRef<FieldStyle> fieldStyle = new StyleRef<>(Styles.FIELD);
    private final ItemSource<T> source;
    private final ItemAdapter<T> adapter;
    private final Signal<Integer> selected;
    private final ScrollView scroll;
    private final Widget content;
    private final Map<Integer, Widget> realized = new HashMap<>();
    private final ArrayDeque<Widget> pool = new ArrayDeque<>();
    private final List<Integer> scratch = new ArrayList<>();

    private HeightFn<T> heightFn = (item, width) -> 20;
    private int gap = 0;
    private IntConsumer onActivate = i -> { };
    // tops[i] = y строки i, tops[n] = полная высота / tops[i] = y of row i, tops[n] = the total height
    private int[] tops = new int[1];
    private int[] heights = new int[0];
    private int lastWidth = -1;
    private boolean dirty = true;
    private int lastClickIndex = -1;
    private long lastClickNanos = 0L;

    public VirtualFlowList(int x, int y, int width, int height, ItemSource<T> source, ItemAdapter<T> adapter,
                           Signal<Integer> selected) {
        super(x, y, width, height);
        this.source = Objects.requireNonNull(source);
        this.adapter = Objects.requireNonNull(adapter);
        this.selected = Objects.requireNonNull(selected);

        this.scroll = new ScrollView(0, 0, 0, 0);
        scroll.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        this.content = new Widget(0, 0, 0, 0);
        scroll.setContent(content);
        add(scroll);

        bind(source.changes(), v -> refreshAll());
        bind(this.selected, v -> rebindAll());
    }

    public VirtualFlowList(int x, int y, int width, int height, ItemSource<T> source, ItemAdapter<T> adapter) {
        this(x, y, width, height, source, adapter, Signal.of(-1));
    }

    // ---------- Настройки / Settings ----------

    public VirtualFlowList<T> setHeightFunction(HeightFn<T> fn) {
        this.heightFn = Objects.requireNonNull(fn);
        dirty = true;
        return this;
    }

    public VirtualFlowList<T> setGap(int gap) {
        this.gap = Math.max(0, gap);
        dirty = true;
        return this;
    }

    public VirtualFlowList<T> setOnActivate(IntConsumer onActivate) {
        this.onActivate = Objects.requireNonNull(onActivate);
        return this;
    }

    public ScrollView getScrollView() { return scroll; }

    public Signal<Integer> selectedSignal() { return selected; }

    public int getSelectedIndex() { return selected.peek(); }

    public VirtualFlowList<T> setSelectedIndex(int index) {
        int n = source.size();
        selected.set(n == 0 ? -1 : Math.max(-1, Math.min(index, n - 1)));
        return this;
    }

    public void scrollToIndex(int index) {
        if (index < 0 || index >= heights.length) return;
        scroll.scrollToShow(tops[index], tops[index] + heights[index]);
    }

    @Override
    public boolean isFocusable() { return true; }

    // ---------- Раскладка строк / Row layout ----------

    private void recompute(int width) {
        int n = source.size();
        heights = new int[n];
        tops = new int[n + 1];
        int y = 0;
        for (int i = 0; i < n; i++) {
            int h = Math.max(1, heightFn.height(source.get(i), width));
            heights[i] = h;
            tops[i] = y;
            y += h + gap;
        }
        tops[n] = n == 0 ? 0 : y - gap;
        lastWidth = width;
        dirty = false;
    }

    // Наибольший индекс, у которого tops[i] <= y / The largest index with tops[i] <= y
    private int indexAtY(int y, int n) {
        int lo = 0;
        int hi = n - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (tops[mid] <= y) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    private int indexAt(double mx, double my) {
        int n = source.size();
        double ly = my - content.absY();
        if (n == 0 || ly < 0 || ly >= tops[n]) return -1;
        int i = indexAtY((int) ly, n);
        return ly < tops[i] + heights[i] ? i : -1;
    }

    private void update() {
        int viewW = content.getWidth();
        if (viewW <= 0) return;
        int n = source.size();
        boolean relayout = false;
        if (dirty || viewW != lastWidth || heights.length != n) {
            recompute(viewW);
            relayout = true;
        }
        if (content.getHeight() != tops[n]) content.setSize(viewW, tops[n]);
        if (n == 0) {
            recycleOutside(0, -1);
            return;
        }

        int first = indexAtY(Math.max(0, scroll.getScroll() - OVERSCAN), n);
        int last = indexAtY(scroll.getScroll() + scroll.getHeight() + OVERSCAN, n);
        recycleOutside(first, last);
        for (int i = first; i <= last; i++) {
            Widget view = realized.get(i);
            if (view == null) realize(i, viewW);
            else if (relayout) place(i, view, viewW);
        }
    }

    private void realize(int index, int viewW) {
        Widget view = pool.poll();
        if (view == null) view = adapter.create();
        content.add(view);
        realized.put(index, view);
        place(index, view, viewW);
        adapter.bind(view, source.get(index), index, index == selected.peek());
        view.performLayout(Constraints.tight(view.getWidth(), view.getHeight()));
    }

    private void place(int index, Widget view, int viewW) {
        if (view.getX() != 0 || view.getY() != tops[index]) view.setPosition(0, tops[index]);
        if (view.getWidth() != viewW || view.getHeight() != heights[index]) view.setSize(viewW, heights[index]);
    }

    private void recycleOutside(int first, int last) {
        scratch.clear();
        for (int index : realized.keySet()) {
            if (index < first || index > last) scratch.add(index);
        }
        for (int i = 0; i < scratch.size(); i++) recycle(scratch.get(i));
    }

    private void recycle(int index) {
        Widget view = realized.remove(index);
        if (view == null) return;
        content.remove(view);
        adapter.recycle(view);
        if (pool.size() < POOL_LIMIT) pool.add(view);
    }

    // Содержимое источника изменилось: высоты считаются заново / The source changed: heights are computed again
    private void refreshAll() {
        int n = source.size();
        if (selected.peek() >= n) selected.set(n - 1);
        dirty = true;
        recycleOutside(0, -1);
    }

    private void rebindAll() {
        int sel = selected.peek();
        for (Map.Entry<Integer, Widget> entry : realized.entrySet()) {
            int index = entry.getKey();
            if (index >= source.size()) continue;
            adapter.bind(entry.getValue(), source.get(index), index, index == sel);
            entry.getValue().performLayout(Constraints.tight(entry.getValue().getWidth(), entry.getValue().getHeight()));
        }
    }

    // ---------- Отрисовка и ввод / Rendering and input ----------

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        update();
    }

    @Override
    protected void renderOverlay(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!isFocused()) return;
        int c = fieldStyle.get().focusedBorder();
        int ax = absX();
        int ay = absY();
        g.fill(ax, ay, ax + getWidth(), ay + 1, c);
        g.fill(ax, ay + getHeight() - 1, ax + getWidth(), ay + getHeight(), c);
        g.fill(ax, ay + 1, ax + 1, ay + getHeight() - 1, c);
        g.fill(ax + getWidth() - 1, ay + 1, ax + getWidth(), ay + getHeight() - 1, c);
    }

    @Override
    protected void onPointerDown(PointerEvent e) {
        if (e.getButton() != MouseButton.LEFT) return;
        int index = indexAt(e.getX(), e.getY());
        if (index < 0) return;

        long now = System.nanoTime();
        boolean doubleClick = index == lastClickIndex && now - lastClickNanos < DOUBLE_CLICK_NANOS;
        lastClickIndex = index;
        lastClickNanos = now;

        setSelectedIndex(index);
        if (doubleClick) {
            lastClickIndex = -1;
            onActivate.accept(index);
        }
        e.stopPropagation();
    }

    @Override
    protected void onKeyDown(KeyboardEvent e) {
        int n = source.size();
        if (n == 0) return;
        int key = e.getKey();
        int sel = selected.peek();
        int target;
        if (key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) {
            if (sel < 0 || sel >= n) return;
            onActivate.accept(sel);
            e.stopPropagation();
            return;
        } else if (key == Keys.UP) {
            target = sel < 0 ? n - 1 : sel - 1;
        } else if (key == Keys.DOWN) {
            target = sel < 0 ? 0 : sel + 1;
        } else if (key == Keys.PAGE_UP || key == Keys.PAGE_DOWN) {
            // Страница: до первой строки, которая не помещается / A page: up to the first row that does not fit
            int dir = key == Keys.PAGE_UP ? -1 : 1;
            int base = sel < 0 ? 0 : sel;
            int y = tops[Math.min(base, n - 1)] + dir * scroll.getHeight();
            target = indexAtY(Math.max(0, y), n);
        } else if (key == Keys.HOME) {
            target = 0;
        } else if (key == Keys.END) {
            target = n - 1;
        } else {
            return;
        }
        target = Math.max(0, Math.min(target, n - 1));
        setSelectedIndex(target);
        scrollToIndex(target);
        e.stopPropagation();
    }
}