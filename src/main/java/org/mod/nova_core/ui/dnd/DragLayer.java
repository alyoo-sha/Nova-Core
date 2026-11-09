package org.mod.nova_core.ui.dnd;

import org.mod.nova_core.ui.api.ScrollView;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.input.Keys;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.SizeMode;

import java.util.IdentityHashMap;
import java.util.Map;

// Слой переноса поверх всего: хранит реестры источников и целей, ведёт сеанс и рисует «призрак».
// Создаётся хостом (UIScreen); из любого виджета: DragLayer.of(widget)
// The drag layer above everything: holds the source and target registries, runs the session and draws the ghost.
// Created by the host (UIScreen); from any widget: DragLayer.of(widget)
public final class DragLayer extends Widget {
    // Расстояние от точки нажатия, после которого начинается перенос / The distance from the press point after which a drag begins
    private static final double START_DISTANCE = 4.0;
    private static final long FADE_OUT_MS = 120L;
    private static final float GHOST_OPACITY = 0.85f;

    // Реестры создаются сразу / Registries are created eagerly
    final Map<Widget, Dnd.Binding<DragSource>> sources = new IdentityHashMap<>();
    final Map<Widget, Dnd.Binding<DropTarget>> targets = new IdentityHashMap<>();

    // Нажатие, которое может стать переносом / A press that may turn into a drag
    private Dnd.Binding<DragSource> candidate = null;
    private double pressX = 0;
    private double pressY = 0;

    private DragSession session = null;
    private DragSource activeSource = null;
    private Widget ghost = null;
    private static final int EDGE = 18;
    private static final int MAX_SCROLL_SPEED = 8;
    private int grabX = 0;
    private int grabY = 0;
    private boolean centered = true;
    private boolean keyboardDrag = false;
    private DropTarget hoveredTarget = null;
    private double pointerX = 0;
    private double pointerY = 0;

    public DragLayer() {
        super(0, 0, 0, 0);
        setSizeMode(SizeMode.FILL, SizeMode.FILL);
        setPointerTransparent(true);
        // Неактивный слой целиком пропускается при поиске цели под курсором, в том числе «призрак» со всеми детьми
        // An inactive layer is skipped as a whole when looking for the target under the cursor, the ghost with all its children included
        setActive(false);
    }

    // Слой переноса экрана, в котором лежит виджет / The drag layer of the screen that contains the widget
    public static DragLayer of(Widget any) {
        for (Widget c : any.root().getChildren()) {
            if (c instanceof DragLayer layer) return layer;
        }
        throw new IllegalStateException("No drag layer: the widget is not inside a UIScreen");
    }

    public boolean isDragging() { return session != null; }

    public DragSession session() { return session; }

    // ---------- Вызывается хостом / Called by the host ----------

    // Нажатие мыши: ищем источник от цели вверх по предкам / A mouse press: look for a source from the target up through its ancestors
    public void press(Widget target, double x, double y, int button) {
        candidate = null;
        if (session != null || button != MouseButton.LEFT) return;
        for (Widget w = target; w != null; w = w.getParent()) {
            Dnd.Binding<DragSource> binding = sources.get(w);
            if (binding != null && binding.active) {
                candidate = binding;
                pressX = x;
                pressY = y;
                return;
            }
        }
    }

    // Движение с зажатой кнопкой. true = перенос идёт, событие забрано / Movement with the button held. true = a drag is going on, the event is taken
    public boolean pointerDragged(double x, double y) {
        pointerX = x;
        pointerY = y;
        if (session == null) {
            Dnd.Binding<DragSource> c = candidate;
            if (c == null || !c.active) return false;
            if (Math.hypot(x - pressX, y - pressY) < START_DISTANCE) return false;
            candidate = null; // решение принимается один раз за нажатие / the decision is made once per press
            begin(c);
            if (session == null) return false;
        }
        update();
        return true;
    }

    // Отпускание кнопки. true = было завершение переноса, событие забрано / A button release. true = a drag was finished, the event is taken
    public boolean pointerReleased(double x, double y, int button) {
        candidate = null;
        if (session == null) return false;
        if (button != MouseButton.LEFT) return true; // другие кнопки во время переноса игнорируем / other buttons are ignored during a drag

        pointerX = x;
        pointerY = y;
        update();
        DragSession s = session;
        DropTarget target = hoveredTarget;
        boolean dropped = target != null;
        try {
            if (dropped) target.onDrop(s, x, y);
        } finally {
            finish(dropped, false);
        }
        return true;
    }

    // Каждый кадр: обновляет цель под курсором, даже если мышь стоит / Every frame: refreshes the target under the cursor even if the mouse is still
    public void tick(double x, double y) {
        pointerX = x;
        pointerY = y;
        if (session != null) update();
    }

    // Отменить с плавным исчезновением «призрака» / Cancel with a smooth ghost fade-out
    public void cancel() {
        candidate = null;
        finish(false, false);
    }

    // Отменить мгновенно; для пересборки и закрытия экрана / Cancel instantly; for rebuilds and screen close
    public void cancelImmediately() {
        candidate = null;
        finish(false, true);
    }

    // ---------- Сеанс / Session ----------

    private void begin(Dnd.Binding<DragSource> c) {
        DragSource src = c.value;
        DragInfo info = src.begin(pressX, pressY);
        if (info == null) return;

        session = new DragSession(info.data(), c.widget, pressX, pressY);
        session.setPointer(pointerX, pointerY);
        activeSource = src;
        grabX = info.grabX();
        grabY = info.grabY();
        centered = info.centered();
        hoveredTarget = null;

        Widget g = info.ghost();
        if (g != null) {
            ghost = g;
            g.setPointerTransparent(true);
            g.setOpacity(GHOST_OPACITY);
            add(g);
            // Сразу считаем размер, чтобы в первом кадре «призрак» уже стоял по центру курсора
            // Compute the size right away so the ghost is already centered on the cursor in the first frame
            g.measure(Constraints.UNCONSTRAINED);
            g.layout();
        }
    }

    private void update() {
        session.setPointer(pointerX, pointerY);

        // Ближайший принимающий предок того, что под курсором / The nearest accepting ancestor of whatever is under the cursor
        Widget hit = root().hitTest(pointerX, pointerY);
        autoScroll(hit);
        Widget newWidget = null;
        DropTarget newTarget = null;
        for (Widget w = hit; w != null; w = w.getParent()) {
            Dnd.Binding<DropTarget> binding = targets.get(w);
            if (binding != null && binding.active && binding.value.canAccept(session)) {
                newWidget = w;
                newTarget = binding.value;
                break;
            }
        }

        if (newWidget != session.hoveredTarget()) {
            DropTarget old = hoveredTarget;
            hoveredTarget = newTarget;
            session.setHoveredTarget(newWidget);
            if (old != null) old.onDragLeave(session);
            if (newTarget != null) newTarget.onDragEnter(session);
        }
        if (hoveredTarget != null) hoveredTarget.onDragOver(session, pointerX, pointerY);
    }

    private void finish(boolean dropped, boolean immediate) {
        keyboardDrag = false;
        DragSession s = session;
        if (s == null) return;
        DragSource src = activeSource;
        DropTarget leaving = hoveredTarget;
        Widget g = ghost;

        session = null;
        activeSource = null;
        hoveredTarget = null;
        ghost = null;
        s.setHoveredTarget(null);
        releaseGhost(g, immediate);

        try {
            if (leaving != null) leaving.onDragLeave(s);
        } finally {
            src.onDragEnd(s, dropped);
        }
    }

    private void releaseGhost(Widget g, boolean immediate) {
        if (g == null) return;
        if (!immediate && g.isMounted()) g.animateOpacity(0f, FADE_OUT_MS, () -> remove(g));
        else remove(g);
    }

    // Если курсор у верхнего или нижнего края прокручиваемого списка, список едет сам / If the cursor is at a scrollable list's top or bottom edge, the list scrolls by itself
    private void autoScroll(Widget hit) {
        for (Widget w = hit; w != null; w = w.getParent()) {
            if (!(w instanceof ScrollView view) || view.getMaxScroll() <= 0) continue;
            int top = view.absY();
            int bottom = top + view.getHeight();
            int delta = 0;
            if (pointerY < top + EDGE) {
                delta = -(int) Math.ceil(MAX_SCROLL_SPEED * Math.min(1.0, (top + EDGE - pointerY) / EDGE));
            } else if (pointerY > bottom - EDGE) {
                delta = (int) Math.ceil(MAX_SCROLL_SPEED * Math.min(1.0, (pointerY - (bottom - EDGE)) / EDGE));
            }
            if (delta != 0) view.scrollImmediately(delta);
            return;
        }
    }

    // «Призрак» центрируется на курсоре; позиция слоя совпадает с экраном, поэтому координаты общие
    // The ghost is centered on the cursor; the layer matches the screen, so the coordinates are shared
    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (ghost == null || session == null) return;
        int px = (int) Math.round(pointerX);
        int py = (int) Math.round(pointerY);
        if (centered) ghost.setPosition(px - ghost.getWidth() / 2, py - ghost.getHeight() / 2);
        else ghost.setPosition(px - grabX, py - grabY);
    }

    // ---------- Перенос с клавиатуры / Keyboard drag ----------

    public boolean isKeyboardDrag() { return keyboardDrag && session != null; }

    // Начать перенос с виджета, у которого (или у предка) есть источник; курсор ставится в его центр
    // Start a drag from a widget that (or whose ancestor) has a source; the cursor goes to its center
    public boolean startKeyboardDrag(Widget from) {
        if (session != null) return false;
        for (Widget w = from; w != null; w = w.getParent()) {
            Dnd.Binding<DragSource> binding = sources.get(w);
            if (binding == null || !binding.active) continue;
            pressX = from.absX() + from.getWidth() / 2.0;
            pressY = from.absY() + from.getHeight() / 2.0;
            pointerX = pressX;
            pointerY = pressY;
            begin(binding);
            if (session == null) return false;
            keyboardDrag = true;
            update();
            return true;
        }
        return false;
    }

    // Стрелки переводят «курсор» на ближайшую подходящую цель, Enter или Space бросают, Escape отменяет
    // The arrows move the "cursor" to the nearest suitable target, Enter or Space drops, Escape cancels
    public boolean handleKey(int key) {
        if (!isKeyboardDrag()) return false;
        if (key == Keys.ESCAPE) {
            cancel();
            return true;
        }
        if (key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) {
            DragSession s = session;
            DropTarget target = hoveredTarget;
            boolean dropped = target != null;
            try {
                if (dropped) target.onDrop(s, pointerX, pointerY);
            } finally {
                finish(dropped, false);
            }
            return true;
        }
        boolean arrow = key == Keys.UP || key == Keys.DOWN || key == Keys.LEFT || key == Keys.RIGHT;
        if (!arrow) return false;
        Widget next = pickTarget(key);
        if (next != null) {
            pointerX = next.absX() + next.getWidth() / 2.0;
            pointerY = next.absY() + next.getHeight() / 2.0;
            update();
        }
        return true;
    }

    private Widget pickTarget(int key) {
        boolean horizontal = key == Keys.LEFT || key == Keys.RIGHT;
        double sign = (key == Keys.RIGHT || key == Keys.DOWN) ? 1.0 : -1.0;
        Widget best = null;
        double bestScore = Double.MAX_VALUE;
        for (Map.Entry<Widget, Dnd.Binding<DropTarget>> entry : targets.entrySet()) {
            Widget w = entry.getKey();
            Dnd.Binding<DropTarget> b = entry.getValue();
            if (!b.active || !w.isMounted() || !w.isVisible() || !b.value.canAccept(session)) continue;
            double dx = w.absX() + w.getWidth() / 2.0 - pointerX;
            double dy = w.absY() + w.getHeight() / 2.0 - pointerY;
            double along = sign * (horizontal ? dx : dy);
            double across = Math.abs(horizontal ? dy : dx);
            if (along <= 1.0) continue;
            double score = along + 2.0 * across;
            if (score < bestScore) {
                bestScore = score;
                best = w;
            }
        }
        return best;
    }
}