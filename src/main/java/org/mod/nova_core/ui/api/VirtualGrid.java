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

// Сетка фиксированных ячеек с прокруткой, которая создаёт виджеты только для видимых рядов и переиспользует их.
// Один и тот же размер ячейки для всех. Сама получает фокус: стрелки двигают выбор
// A scrolling grid of fixed-size cells that creates widgets only for the visible rows and reuses them.
// All cells have the same size. Takes focus itself: arrows move the selection
public class VirtualGrid<T> extends Widget {
    private static final int OVERSCAN_ROWS = 1;
    private static final int POOL_LIMIT = 64;
    private static final long DOUBLE_CLICK_NANOS = 350_000_000L;

    private final StyleRef<FieldStyle> fieldStyle = new StyleRef<>(Styles.FIELD);
    private final ItemSource<T> source;
    private final ItemAdapter<T> adapter;
    private final Signal<Integer> selected;
    private final ScrollView scroll;
    private final Widget content;

    // Созданные сейчас виджеты по индексу элемента и пул свободных / Currently created widgets by item index and the pool of free ones
    private final Map<Integer, Widget> realized = new HashMap<>();
    private final ArrayDeque<Widget> pool = new ArrayDeque<>();
    private final List<Integer> scratch = new ArrayList<>();

    private int cellWidth = 18;
    private int cellHeight = 18;
    private int gap = 0;
    // 0 = число колонок считается по ширине / 0 = the column count is computed from the width
    private int fixedColumns = 0;
    // true = ячейки делят всю ширину поровну / true = the cells share the whole width equally
    private boolean fillWidth = false;
    private IntConsumer onActivate = i -> { };

    private boolean geometryDirty = true;
    private int lastColumns = -1;
    private int lastCellWidth = -1;
    private int lastClickIndex = -1;
    private long lastClickNanos = 0L;

    public VirtualGrid(int x, int y, int width, int height, ItemSource<T> source, ItemAdapter<T> adapter) {
        this(x, y, width, height, source, adapter, Signal.of(-1));
    }

    // selected: индекс выбранного элемента, -1 = ничего; можно делить с другими виджетами
    // selected: the index of the selected item, -1 = nothing; can be shared with other widgets
    public VirtualGrid(int x, int y, int width, int height, ItemSource<T> source, ItemAdapter<T> adapter,
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
        geometryChanged();
    }

    // ---------- Настройки / Settings ----------

    public ItemSource<T> getSource() { return source; }

    // Окно прокрутки, например для стиля полосы или шага колеса / The scroll view, e.g. for the bar style or the wheel step
    public ScrollView getScrollView() { return scroll; }

    public VirtualGrid<T> setCellSize(int width, int height) {
        this.cellWidth = Math.max(1, width);
        this.cellHeight = Math.max(1, height);
        geometryChanged();
        return this;
    }

    public VirtualGrid<T> setGap(int gap) {
        this.gap = Math.max(0, gap);
        geometryChanged();
        return this;
    }

    // Фиксированное число колонок; 0 = по ширине / A fixed column count; 0 = from the width
    public VirtualGrid<T> setColumns(int columns) {
        this.fixedColumns = Math.max(0, columns);
        geometryChanged();
        return this;
    }

    public VirtualGrid<T> setFillWidth(boolean fillWidth) {
        this.fillWidth = fillWidth;
        geometryChanged();
        return this;
    }

    // Двойной клик или Enter по выбранному элементу / Double click or Enter on the selected item
    public VirtualGrid<T> setOnActivate(IntConsumer onActivate) {
        this.onActivate = Objects.requireNonNull(onActivate);
        return this;
    }

    private void geometryChanged() {
        geometryDirty = true;
        scroll.setWheelStep(cellHeight + gap);
    }

    @Override
    public boolean isFocusable() { return true; }

    // ---------- Выбор / Selection ----------

    public int getSelectedIndex() { return selected.peek(); }

    public Signal<Integer> selectedSignal() { return selected; }

    // null, если ничего не выбрано / null if nothing is selected
    public T getSelectedItem() {
        int i = selected.peek();
        return i >= 0 && i < source.size() ? source.get(i) : null;
    }

    public VirtualGrid<T> setSelectedIndex(int index) {
        selected.set(clampIndex(index));
        return this;
    }

    private int clampIndex(int index) {
        int n = source.size();
        return n == 0 ? -1 : Math.max(-1, Math.min(index, n - 1));
    }

    // Прокрутить так, чтобы элемент был виден / Scroll so that the item is visible
    public void scrollToIndex(int index) {
        if (index < 0 || index >= source.size()) return;
        int top = (index / columns()) * (cellHeight + gap);
        scroll.scrollToShow(top, top + cellHeight);
    }

    // ---------- Геометрия / Geometry ----------

    public int columns() {
        if (fixedColumns > 0) return fixedColumns;
        if (fillWidth) return 1;
        return Math.max(1, (content.getWidth() + gap) / (cellWidth + gap));
    }

    private int cellWidthFor(int cols) {
        if (!fillWidth) return cellWidth;
        return Math.max(1, (content.getWidth() - gap * (cols - 1)) / cols);
    }

    // Индекс элемента под точкой или -1, если там промежуток или пусто / The item index under a point, or -1 for a gap or nothing
    public int indexAt(double mx, double my) {
        double lx = mx - content.absX();
        double ly = my - content.absY();
        if (lx < 0 || ly < 0) return -1;

        int cols = columns();
        int cw = cellWidthFor(cols);
        int strideX = cw + gap;
        int strideY = cellHeight + gap;
        int col = (int) (lx / strideX);
        int row = (int) (ly / strideY);
        if (col >= cols) return -1;
        if (lx - col * strideX >= cw || ly - row * strideY >= cellHeight) return -1;

        long index = (long) row * cols + col;
        return index < source.size() ? (int) index : -1;
    }

    // ---------- Виртуализация / Virtualization ----------

    // Каждый кадр до отрисовки детей: подгоняет высоту контента и набор созданных виджетов под положение прокрутки
    // Every frame before the children are drawn: fits the content height and the set of created widgets to the scroll position
    private void update() {
        int viewW = content.getWidth();
        if (viewW <= 0) return; // ещё не измерено / not measured yet

        int n = source.size();
        int cols = columns();
        int cw = cellWidthFor(cols);
        int rowStride = cellHeight + gap;
        int rows = (n + cols - 1) / cols;
        int total = rows == 0 ? 0 : rows * rowStride - gap;
        if (content.getHeight() != total) content.setSize(viewW, total);

        boolean moved = geometryDirty || cols != lastColumns || cw != lastCellWidth;
        geometryDirty = false;
        lastColumns = cols;
        lastCellWidth = cw;

        int first = 0;
        int endExclusive = 0;
        if (rows > 0) {
            int viewH = scroll.getHeight();
            int top = Math.min(scroll.getScroll(), Math.max(0, total - viewH));
            int firstRow = Math.max(0, top / rowStride - OVERSCAN_ROWS);
            int lastRow = Math.min(rows - 1, (top + viewH) / rowStride + OVERSCAN_ROWS);
            first = firstRow * cols;
            endExclusive = Math.min(n, (lastRow + 1) * cols);
        }

        if (!realized.isEmpty()) {
            scratch.clear();
            for (int index : realized.keySet()) {
                if (index < first || index >= endExclusive) scratch.add(index);
            }
            for (int i = 0; i < scratch.size(); i++) recycle(scratch.get(i));
        }

        for (int i = first; i < endExclusive; i++) {
            Widget view = realized.get(i);
            if (view == null) realize(i, cols, cw);
            else if (moved) place(i, view, cols, cw);
        }
    }

    private void realize(int index, int cols, int cw) {
        Widget view = pool.poll();
        if (view == null) view = adapter.create();
        content.add(view);
        realized.put(index, view);
        place(index, view, cols, cw);
        adapter.bind(view, source.get(index), index, index == selected.peek());
        // Сразу раскладываем, чтобы ячейка не мелькнула нераскладенной / Lay out right away so the cell never flashes unlaid
        view.performLayout(Constraints.tight(view.getWidth(), view.getHeight()));
    }

    private void place(int index, Widget view, int cols, int cw) {
        int x = (index % cols) * (cw + gap);
        int y = (index / cols) * (cellHeight + gap);
        if (view.getX() != x || view.getY() != y) view.setPosition(x, y);
        if (view.getWidth() != cw || view.getHeight() != cellHeight) view.setSize(cw, cellHeight);
    }

    private void recycle(int index) {
        Widget view = realized.remove(index);
        if (view == null) return;
        content.remove(view);
        adapter.recycle(view);
        if (pool.size() < POOL_LIMIT) pool.add(view);
    }

    // Содержимое источника изменилось / The source content changed
    private void refreshAll() {
        int n = source.size();
        if (selected.peek() >= n) selected.set(n - 1); // при пустом списке получится -1 / becomes -1 for an empty list

        scratch.clear();
        for (int index : realized.keySet()) {
            if (index >= n) scratch.add(index);
        }
        for (int i = 0; i < scratch.size(); i++) recycle(scratch.get(i));
        rebindAll();
    }

    private void rebindAll() {
        int sel = selected.peek();
        for (Map.Entry<Integer, Widget> entry : realized.entrySet()) {
            int index = entry.getKey();
            Widget view = entry.getValue();
            adapter.bind(view, source.get(index), index, index == sel);
            view.performLayout(Constraints.tight(view.getWidth(), view.getHeight()));
        }
    }

    // ---------- Отрисовка / Rendering ----------

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        update();
    }

    // Рамка фокуса поверх всего / The focus frame above everything
    @Override
    protected void renderOverlay(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!isFocused()) return;
        int c = fieldStyle.get().focusedBorder();
        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();
        g.fill(ax, ay, ax + w, ay + 1, c);
        g.fill(ax, ay + h - 1, ax + w, ay + h, c);
        g.fill(ax, ay + 1, ax + 1, ay + h - 1, c);
        g.fill(ax + w - 1, ay + 1, ax + w, ay + h - 1, c);
    }

    // ---------- События / Events ----------

    // Всплывает от ячейки; если ячейка (например, кнопка) забрала событие, выбор не меняется
    // Bubbles up from the cell; if the cell (e.g. a button) claimed the event, the selection does not change
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
        int cols = columns();
        int sel = selected.peek();
        int page = Math.max(1, scroll.getHeight() / (cellHeight + gap)) * cols;
        int target;

        if (key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) {
            if (sel < 0 || sel >= n) return;
            onActivate.accept(sel);
            e.stopPropagation();
            return;
        } else if (key == Keys.UP) {
            target = sel < 0 ? n - 1 : (sel - cols >= 0 ? sel - cols : sel);
        } else if (key == Keys.DOWN) {
            target = sel < 0 ? 0 : (sel + cols < n ? sel + cols : (sel / cols < (n - 1) / cols ? n - 1 : sel));
        } else if (key == Keys.LEFT && cols > 1) {
            target = sel < 0 ? n - 1 : Math.max(0, sel - 1);
        } else if (key == Keys.RIGHT && cols > 1) {
            target = sel < 0 ? 0 : Math.min(n - 1, sel + 1);
        } else if (key == Keys.PAGE_UP) {
            target = sel < 0 ? 0 : sel - page;
        } else if (key == Keys.PAGE_DOWN) {
            target = sel < 0 ? 0 : sel + page;
        } else if (key == Keys.HOME) {
            target = 0;
        } else if (key == Keys.END) {
            target = n - 1;
        } else {
            return; // остальное (Tab, влево-вправо в списке) не наше / the rest (Tab, left-right in a list) is not ours
        }

        target = Math.max(0, Math.min(target, n - 1));
        setSelectedIndex(target);
        scrollToIndex(target);
        e.stopPropagation();
    }

    @Override
    public void describe(List<String> lines) {
        lines.add("items " + source.size() + "  columns " + columns() + "  realized " + realized.size()
                + "  pooled " + pool.size() + "  selected " + selected.peek());
    }
}