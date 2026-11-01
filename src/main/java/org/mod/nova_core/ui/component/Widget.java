package org.mod.nova_core.ui.component;

import org.mod.nova_core.ui.animation.Easing;
import org.mod.nova_core.ui.animation.Tween;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.input.*;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.state.ReadableSignal;
import org.mod.nova_core.ui.state.UiScope;

import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Widget implements EventTarget {
    // Дети, слушатели и cleanup-действия создаются сразу / Children, listeners and cleanups are created eagerly
    private final List<Widget> children = new ArrayList<>();
    private final List<Widget> childrenView = Collections.unmodifiableList(children);
    private final List<ListenerEntry<?>> listeners = new ArrayList<>();
    private final List<Runnable> cleanups = new ArrayList<>();
    // Привязки сигналов, запускаются при каждом монтировании / Signal bindings, started on every mount
    private final List<Runnable> mountBindings = new ArrayList<>();
    // Реактивная область, закрывается при размонтировании / Reactive scope, closed on unmount
    private final UiScope scope = new UiScope();
    //
    private Widget parent = null;
    // Имя для инспектора и дампа дерева; на поведение не влияет / A name for the inspector and the tree dump; does not affect behavior
    private String debugName = null;
    // Позиция относительно родителя / Position relative to the parent
    private int x;
    private int y;
    // Фактический размер (после measure) / Actual size (after measure)
    private int width;
    private int height;
    // Запрошенный размер, используется в режиме FIXED / Requested size, used by FIXED mode
    private int prefWidth;
    private int prefHeight;

    private SizeMode widthMode = SizeMode.FIXED;
    private SizeMode heightMode = SizeMode.FIXED;
    // Доля свободного места в стеке, 0 = не растягивается / Share of free space in a stack, 0 = no stretching
    private float weight = 0f;
    // Внешние отступы, учитываются в стеках / Outer margin, honored by stacks
    private Insets margin = Insets.ZERO;

    // true = раскладка этого поддерева устарела; если виджет грязный, все его предки тоже грязные
    // true = the layout of this subtree is stale; if a widget is dirty, all its ancestors are dirty too
    private boolean layoutDirty = true;
    // Ограничения последнего измерения, нужны для кэша / Constraints of the last measure, used by the cache
    private Constraints lastConstraints = null;

    private boolean mounted = false;
    private boolean visible = true;
    private boolean active = true;
    // Обрезать детей по границам виджета / Clip children to this widget's bounds
    private boolean clipChildren = false;
    // true = сам виджет не становится целью указателя, но его дети становятся
    // true = the widget itself is never a pointer target, but its children can be
    private boolean pointerTransparent = false;
    // Прозрачность виджета и всего его поддерева: 0 = невидим и не принимает указатель, 1 = обычный вид
    // Opacity of the widget and its whole subtree: 0 = invisible and ignores the pointer, 1 = normal look
    private float opacity = 1f;
    // Текущая анимация прозрачности / The current opacity animation
    private Tween<Float> opacityAnim = null;

    private boolean focused = false;
    // Находится ли указатель над виджетом или его потомком; выставляется событиями enter/leave
    // Is the pointer over the widget or its descendant; set by enter/leave events
    private boolean hovered = false;
    // Используется только у корня: кто сейчас в фокусе / Used on the root only: who currently has focus
    private Widget focusOwner = null;

    public Widget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.prefWidth = width;
        this.prefHeight = height;
    }

    // ---------- Дерево / Tree ----------

    public void add(Widget w) {
        if (w == this) throw new IllegalArgumentException("Widget cannot be its own child");
        if (w.parent != null) w.parent.remove(w);
        w.parent = this;
        children.add(w);
        w.requestLayout();
        if (mounted) w.mountSubtree();
    }

    public void remove(Widget w) {
        if (!children.contains(w)) return;
        dropFocusIfInside(w);
        if (w.mounted) w.unmountSubtree();
        children.remove(w);
        w.parent = null;
        requestLayout();
    }

    public void clearChildren() {
        if (children.isEmpty()) return;
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget c = children.get(i);
            dropFocusIfInside(c);
            if (c.mounted) c.unmountSubtree();
            c.parent = null;
        }
        children.clear();
        requestLayout();
    }

    public List<Widget> getChildren() { return childrenView; }
    public Widget getParent() { return parent; }

    // Корень дерева / Root of the tree
    public Widget root() {
        Widget w = this;
        while (w.parent != null) w = w.parent;
        return w;
    }

    // ---------- Жизненный цикл / Lifecycle ----------

    public final boolean isMounted() { return mounted; }

    // Вызывается хостом на корне, когда дерево становится живым / Called by the host on the root when the tree goes live
    public final void mount() {
        if (parent != null) throw new IllegalStateException("Only a root widget can be mounted by the host");
        mountSubtree();
    }

    // Вызывается хостом на корне, когда дерево перестаёт быть живым / Called by the host on the root when the tree stops being live
    public final void unmount() {
        if (parent != null) throw new IllegalStateException("Only a root widget can be unmounted by the host");
        unmountSubtree();
    }

    // Родитель раньше детей / Parent before children
    private void mountSubtree() {
        if (mounted) return;
        mounted = true;
        for (int i = 0; i < mountBindings.size(); i++) mountBindings.get(i).run();
        onMount();
        for (int i = 0; i < children.size(); i++) children.get(i).mountSubtree();
    }

    // Дети раньше родителя / Children before the parent
    private void unmountSubtree() {
        if (!mounted) return;
        for (int i = children.size() - 1; i >= 0; i--) children.get(i).unmountSubtree();
        if (parent == null) clearFocus();
        mounted = false;
        hovered = false;
        try {
            runCleanups();
        } finally {
            try {
                scope.close();
            } finally {
                onUnmount();
            }
        }
    }

    private void runCleanups() {
        RuntimeException first = null;
        for (int i = cleanups.size() - 1; i >= 0; i--) {
            try {
                cleanups.get(i).run();
            } catch (RuntimeException ex) {
                if (first == null) first = ex;
            }
        }
        cleanups.clear();
        if (first != null) throw first;
    }

    // Выполнить действие при размонтировании, в обратном порядке регистрации. Вызывать из onMount()
    // Run an action on unmount, in reverse order of registration. Call it from onMount()
    public final void addCleanup(Runnable cleanup) {
        if (!mounted) throw new IllegalStateException("addCleanup must be called while the widget is mounted");
        cleanups.add(cleanup);
    }

    // Реактивная область виджета: всё созданное в ней отменяется при размонтировании. Только пока смонтирован
    // The widget's reactive scope: everything created in it is cancelled on unmount. Only while mounted
    public final UiScope scope() {
        if (!mounted) throw new IllegalStateException("scope() is available only while the widget is mounted");
        return scope;
    }

    public final <T> void bind(ReadableSignal<T> source, Consumer<? super T> sink) {
        whileMounted(() -> scope.observe(source, sink));
    }

    // Запускает starter при каждом монтировании (и сразу, если уже смонтирован); внутри можно вызывать addCleanup и scope()
    // Runs starter on every mount (and right away if already mounted); addCleanup and scope() may be called inside
    public final void whileMounted(Runnable starter) {
        mountBindings.add(starter);
        if (mounted) starter.run();
    }

    public final void bindVisible(ReadableSignal<Boolean> source) {
        bind(source, v -> setVisible(v));
    }

    public final void bindActive(ReadableSignal<Boolean> source) {
        bind(source, v -> setActive(v));
    }

    // Запустить анимацию, привязанную к жизни виджета: при размонтировании она отменяется. Только пока смонтирован
    // Start an animation tied to the widget's life: it is cancelled on unmount. Only while mounted
    public final <T> Tween<T> animate(Tween.Builder<T> builder) {
        return scope().own(builder.start());
    }

    // Плавно изменить прозрачность; новый вызов заменяет предыдущую анимацию. Только пока смонтирован
    // Smoothly change opacity; a new call replaces the previous animation. Only while mounted
    public final Tween<Float> animateOpacity(float to, long durationMs) {
        return animateOpacity(to, durationMs, null);
    }

    // onComplete вызывается, только если анимация дошла до конца (не при отмене). Только пока смонтирован
    // onComplete runs only if the animation reaches its end (not on cancel). Only while mounted
    public final Tween<Float> animateOpacity(float to, long durationMs, Runnable onComplete) {
        if (opacityAnim != null) opacityAnim.cancel();
        Tween.Builder<Float> builder = Tween.floats(opacity, to)
                .duration(durationMs)
                .easing(Easing.EASE_OUT)
                .onUpdate(v -> setOpacity(v));
        if (onComplete != null) builder.onComplete(onComplete);
        opacityAnim = animate(builder);
        return opacityAnim;
    }

    // Виджет вошёл в живое дерево; размеры уже посчитаны / The widget entered the live tree; sizes are already computed
    protected void onMount() {}

    // Виджет покинул живое дерево; cleanup-действия уже выполнены / The widget left the live tree; cleanups have already run
    protected void onUnmount() {}

    // ---------- Геометрия / Geometry ----------

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // Не вызывает раскладку: так стеки двигают детей. Если вы вручную двигаете ребёнка внутри WRAP-контейнера
    // после первой раскладки, вызовите requestLayout()
    // Does not trigger layout: stacks move children this way. If you manually move a child inside a WRAP container
    // after the first layout, call requestLayout()
    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    // Задаёт запрошенный (для FIXED) и текущий размер / Sets the requested (for FIXED) and current size
    public void setSize(int width, int height) {
        this.prefWidth = width;
        this.prefHeight = height;
        this.width = width;
        this.height = height;
        requestLayout();
    }

    public void setBounds(int x, int y, int width, int height) {
        setPosition(x, y);
        setSize(width, height);
    }

    // Абсолютные координаты на экране / Absolute on-screen coordinates
    public int absX() { return parent == null ? x : parent.absX() + x; }
    public int absY() { return parent == null ? y : parent.absY() + y; }

    public boolean contains(double mx, double my) {
        int ax = absX();
        int ay = absY();
        return mx >= ax && mx < ax + width && my >= ay && my < ay + height;
    }

    // ---------- Параметры раскладки / Layout properties ----------

    public SizeMode getWidthMode() { return widthMode; }
    public SizeMode getHeightMode() { return heightMode; }

    public void setWidthMode(SizeMode mode) {
        this.widthMode = mode;
        requestLayout();
    }

    public void setHeightMode(SizeMode mode) {
        this.heightMode = mode;
        requestLayout();
    }

    public void setSizeMode(SizeMode widthMode, SizeMode heightMode) {
        this.widthMode = widthMode;
        this.heightMode = heightMode;
        requestLayout();
    }

    public float getWeight() { return weight; }

    public void setWeight(float weight) {
        this.weight = Math.max(0f, weight);
        requestLayout();
    }

    public Insets getMargin() { return margin; }

    public void setMargin(Insets margin) {
        this.margin = margin;
        requestLayout();
    }

    // ---------- Раскладка / Layout ----------

    // Пометить раскладку устаревшей у себя и у всех предков; сам пересчёт сделает хост через performLayout
    // Mark the layout stale on this widget and all its ancestors; the host does the actual work via performLayout
    public final void requestLayout() {
        for (Widget w = this; w != null; w = w.parent) w.layoutDirty = true;
    }

    public final boolean needsLayout() { return layoutDirty; }
    // Пометить устаревшим всё поддерево и сбросить кэш измерений; нужно при смене темы
    // Mark the whole subtree stale and drop the measure cache; needed on a theme change
    public final void invalidateLayoutDeep() {
        requestLayout();
        invalidateDown();
    }

    private void invalidateDown() {
        lastConstraints = null;
        layoutDirty = true;
        for (int i = 0; i < children.size(); i++) children.get(i).invalidateDown();
    }

    // Хост вызывает каждый кадр на корне: если ничего не менялось, почти бесплатно
    // The host calls it on the root every frame: nearly free when nothing changed
    public final void performLayout(Constraints c) {
        measure(c);
        layout();
    }

    // Проход 1: вычисляет и записывает размер виджета и всех детей; чистое поддерево с теми же ограничениями пропускается
    // Pass 1: computes and stores sizes of this widget and its children; a clean subtree with the same constraints is skipped
    public final Size measure(Constraints c) {
        if (!layoutDirty && c.equals(lastConstraints)) return new Size(width, height);

        int knownW = knownExtent(widthMode, prefWidth, c.maxWidth());
        int knownH = knownExtent(heightMode, prefHeight, c.maxHeight());
        // Если размер уже известен, детям доступно ровно столько / If the size is already known, children get exactly that much
        int innerMaxW = knownW >= 0 ? c.constrainWidth(knownW) : c.maxWidth();
        int innerMaxH = knownH >= 0 ? c.constrainHeight(knownH) : c.maxHeight();
        Size content = measureContent(Constraints.loose(innerMaxW, innerMaxH));
        this.width = c.constrainWidth(knownW >= 0 ? knownW : content.width());
        this.height = c.constrainHeight(knownH >= 0 ? knownH : content.height());

        lastConstraints = c;
        layoutDirty = true; // размер мог измениться: детей нужно расставить заново / the size may have changed: children need positioning again
        return new Size(width, height);
    }

    // -1 = размер определяется содержимым / -1 = size comes from the content
    private static int knownExtent(SizeMode mode, int fixed, int max) {
        return switch (mode) {
            case FIXED -> fixed;
            case FILL -> max == Constraints.UNBOUNDED ? -1 : max;
            case WRAP -> -1;
        };
    }

    // Измеряет детей и возвращает размер содержимого. По умолчанию дети стоят где заданы (x, y)
    // Measures children and returns the content size. By default children stay where set (x, y)
    protected Size measureContent(Constraints c) {
        int w = 0;
        int h = 0;
        for (Widget child : children) {
            if (!child.visible) continue;
            child.measure(c);
            w = Math.max(w, child.x + child.width);
            h = Math.max(h, child.y + child.height);
        }
        return new Size(w, h);
    }

    // Проход 2: расставляет детей; чистое поддерево пропускается / Pass 2: positions children; a clean subtree is skipped
    public final void layout() {
        if (!layoutDirty) return;
        layoutChildren();
        layoutDirty = false;
    }

    // Контейнеры переопределяют это, затем вызывают super / Containers override this, then call super
    protected void layoutChildren() {
        for (Widget c : children) c.layout();
    }

    // ---------- Состояние / State ----------

    public boolean isVisible() { return visible; }

    public void setVisible(boolean visible) {
        if (this.visible == visible) return;
        this.visible = visible;
        requestLayout();
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) {
        this.active = active;
        if (!active) hovered = false;
    }
    public boolean isClipChildren() { return clipChildren; }
    public void setClipChildren(boolean clipChildren) { this.clipChildren = clipChildren; }
    public boolean isPointerTransparent() { return pointerTransparent; }
    public void setPointerTransparent(boolean pointerTransparent) { this.pointerTransparent = pointerTransparent; }
    public float getOpacity() { return opacity; }
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0f,
                Math.min(1f, opacity)
        );
    }

    // ---------- Фокус / Focus ----------

    public boolean isHovered() { return hovered; }

    public String getDebugName() { return debugName; }
    public void setDebugName(String name) { this.debugName = name; }

    // Дополнительные строки для инспектора; наследники добавляют свои / Extra lines for the inspector; subclasses add their own
    public void describe(List<String> lines) {}

    public boolean isFocused() { return focused; }

    // Может ли виджет получать фокус клавиатуры / Can this widget take keyboard focus
    public boolean isFocusable() { return false; }

    // Виджет в фокусе всего дерева / Focused widget of the whole tree
    public Widget getFocusOwner() { return root().focusOwner; }

    // Запросить фокус для себя / Request focus for this widget
    public final void requestFocus() {
        if (visible && active && isFocusable()) root().changeFocus(this);
    }

    // Отпустить фокус, если он у этого виджета / Release focus if this widget holds it
    public final void releaseFocus() {
        if (focused) root().changeFocus(null);
    }

    // Сбросить фокус во всём дереве / Clear focus in the whole tree
    public final void clearFocus() {
        root().changeFocus(null);
    }

    // Вызывается только на корне / Called on the root only
    private void changeFocus(Widget next) {
        Widget old = focusOwner;
        if (old == next) return;
        focusOwner = next;
        if (old != null) {
            old.focused = false;
            old.onFocusChanged(false);
        }
        if (next != null) {
            next.focused = true;
            next.onFocusChanged(true);
        }
    }

    // Если фокус внутри удаляемого поддерева, сбросить его / If focus is inside the removed subtree, drop it
    private void dropFocusIfInside(Widget subtree) {
        Widget owner = root().focusOwner;
        for (Widget p = owner; p != null; p = p.parent) {
            if (p == subtree) {
                root().changeFocus(null);
                return;
            }
        }
    }

    // ---------- Отрисовка / Rendering ----------

    public final void render(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!visible || opacity <= 0f) return;
        boolean faded = opacity < 1f;
        if (faded) g.pushOpacity(opacity);
        renderSelf(g, mouseX, mouseY, partialTick);
        if (clipChildren) g.pushClip(absX(), absY(), absX() + width, absY() + height);
        for (Widget c : children) c.render(g, mouseX, mouseY, partialTick);
        if (clipChildren) g.popClip();
        renderOverlay(g, mouseX, mouseY, partialTick);
        if (faded) g.popOpacity();
    }

    // Рисует сам виджет, без детей / Draws the widget itself, without children
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {}

    // Рисуется поверх детей и вне обрезки / Drawn above children and outside the clip
    protected void renderOverlay(UiGraphics g, int mouseX, int mouseY, float partialTick) {}

    // ---------- Поиск цели под указателем / Pointer hit testing ----------

    // Самый верхний виджет под точкой или null; верхний слой = последний ребёнок
    // The topmost widget under the point, or null; top layer = last child
    public final Widget hitTest(double mx, double my) {
        if (!visible || !active || opacity <= 0f) return null;
        if (clipChildren && !contains(mx, my)) return null;
        for (int i = children.size() - 1; i >= 0; i--) {
            Widget hit = children.get(i).hitTest(mx, my);
            if (hit != null) return hit;
        }
        return !pointerTransparent && contains(mx, my) ? this : null;
    }

    // ---------- События / Events ----------

    @Override
    public EventTarget getEventParent() { return parent; }

    // Отправить событие, адресованное этому виджету / Dispatch an event addressed to this widget
    public final void dispatchEvent(UiEvent event) {
        EventDispatcher.dispatch(this, event);
    }

    // Подписка на события; capture = true слушает фазу перехвата у предков
    // Subscribe to events; capture = true listens during the capture phase on ancestors
    public <E extends UiEvent> void addListener(EventType<E> type, EventListener<E> listener) {
        addListener(type, listener, false);
    }

    public <E extends UiEvent> void addListener(EventType<E> type, EventListener<E> listener, boolean capture) {
        listeners.add(new ListenerEntry<>(type, listener, capture));
    }

    // Убирает все подписки этого слушателя / Removes all subscriptions of this listener
    public void removeListener(EventListener<?> listener) {
        listeners.removeIf(entry -> entry.listener() == listener);
    }

    // Вызывается диспетчером на каждом узле пути / Called by the dispatcher on every node of the path
    @Override
    public final void handleEvent(UiEvent e) {
        if (!active) return;

        if (e.getPhase() == EventPhase.CAPTURE) onCapture(e);
        else runHook(e);

        for (int i = 0; i < listeners.size(); i++) {
            ListenerEntry<?> entry = listeners.get(i);
            if (entry.accepts(e)) entry.invoke(e);
        }
    }

    private void runHook(UiEvent e) {
        EventType<?> t = e.getType();
        if (t == EventType.POINTER_DOWN) onPointerDown((PointerEvent) e);
        else if (t == EventType.POINTER_UP) onPointerUp((PointerEvent) e);
        else if (t == EventType.POINTER_MOVE) onPointerMove((PointerEvent) e);
        else if (t == EventType.POINTER_DRAG) onPointerDrag((PointerEvent) e);
        else if (t == EventType.POINTER_SCROLL) onPointerScroll((PointerEvent) e);
        else if (t == EventType.POINTER_ENTER) {
            hovered = true;
            onPointerEnter((PointerEvent) e);
        } else if (t == EventType.POINTER_LEAVE) {
            hovered = false;
            onPointerLeave((PointerEvent) e);
        }
        else if (t == EventType.POINTER_CANCEL) onPointerCancel((PointerEvent) e);
        else if (t == EventType.KEY_DOWN) onKeyDown((KeyboardEvent) e);
        else if (t == EventType.KEY_UP) onKeyUp((KeyboardEvent) e);
        else if (t == EventType.CHAR) onChar((KeyboardEvent) e);
    }

    // ---------- Обработчики для наследников / Hooks for subclasses ----------
    // Вызываются, когда виджет цель события или его предок (фазы target и bubble).
    // Чтобы забрать событие себе, вызовите e.stopPropagation().
    // Called when the widget is the event target or its ancestor (target and bubble phases).
    // To claim the event, call e.stopPropagation().

    // Фаза capture, до того как событие дойдёт до цели / Capture phase, before the event reaches the target
    protected void onCapture(UiEvent e) {}

    protected void onPointerDown(PointerEvent e) {}
    protected void onPointerUp(PointerEvent e) {}
    protected void onPointerMove(PointerEvent e) {}
    protected void onPointerDrag(PointerEvent e) {}
    protected void onPointerScroll(PointerEvent e) {}
    // Enter/leave не всплывают; capture-слушатели предков их видят / Enter/leave do not bubble; capture listeners on ancestors do see them
    protected void onPointerEnter(PointerEvent e) {}
    protected void onPointerLeave(PointerEvent e) {}
    // Нажатие отменено, не дойдя до отпускания / The press was cancelled before the release
    protected void onPointerCancel(PointerEvent e) {}
    protected void onKeyDown(KeyboardEvent e) {}
    protected void onKeyUp(KeyboardEvent e) {}
    protected void onChar(KeyboardEvent e) {}

    public void onFocusChanged(boolean focused) {}

    // ---------- Внутреннее / Internal ----------

    private record ListenerEntry<E extends UiEvent>(EventType<E> type, EventListener<E> listener, boolean capture) {
        // Capture-слушатели работают в фазе capture, остальные в target и bubble; на цели срабатывают все
        // Capture listeners run in the capture phase, the rest in target and bubble; all run on the target
        boolean accepts(UiEvent e) {
            if (type != e.getType()) return false;
            return switch (e.getPhase()) {
                case CAPTURE -> capture;
                case TARGET -> true;
                case BUBBLE -> !capture;
                default -> false;
            };
        }

        void invoke(UiEvent e) {
            listener.handle(type.cast(e));
        }
    }
}