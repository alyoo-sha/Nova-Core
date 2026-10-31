package org.mod.nova_core.ui.component;

import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.layout.SizeMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Widget {
    // Дети создаются сразу / Children are created eagerly
    private final List<Widget> children = new ArrayList<>();
    private final List<Widget> childrenView = Collections.unmodifiableList(children);

    private Widget parent = null;
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

    private boolean visible = true;
    private boolean active = true;
    // Обрезать детей по границам виджета / Clip children to this widget's bounds
    private boolean clipChildren = false;
    private boolean focused = false;
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
    }

    public void remove(Widget w) {
        if (!children.contains(w)) return;
        dropFocusIfInside(w);
        children.remove(w);
        w.parent = null;
    }

    public void clearChildren() {
        for (Widget c : children) {
            dropFocusIfInside(c);
            c.parent = null;
        }
        children.clear();
    }

    public List<Widget> getChildren() { return childrenView; }
    public Widget getParent() { return parent; }

    // Корень дерева / Root of the tree
    public Widget root() {
        Widget w = this;
        while (w.parent != null) w = w.parent;
        return w;
    }

    // ---------- Геометрия / Geometry ----------

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    // Задаёт запрошенный (для FIXED) и текущий размер / Sets the requested (for FIXED) and current size
    public void setSize(int width, int height) {
        this.prefWidth = width;
        this.prefHeight = height;
        this.width = width;
        this.height = height;
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
    public void setWidthMode(SizeMode mode) { this.widthMode = mode; }
    public void setHeightMode(SizeMode mode) { this.heightMode = mode; }
    public void setSizeMode(SizeMode widthMode, SizeMode heightMode) {
        this.widthMode = widthMode;
        this.heightMode = heightMode;
    }

    public float getWeight() { return weight; }
    public void setWeight(float weight) { this.weight = Math.max(0f, weight); }

    public Insets getMargin() { return margin; }
    public void setMargin(Insets margin) { this.margin = margin; }

    // ---------- Раскладка / Layout ----------

    // Проход 1: вычисляет и записывает размер виджета и всех детей / Pass 1: computes and stores sizes of this widget and its children
    public final Size measure(Constraints c) {
        int knownW = knownExtent(widthMode, prefWidth, c.maxWidth());
        int knownH = knownExtent(heightMode, prefHeight, c.maxHeight());
        // Если размер уже известен, детям доступно ровно столько / If the size is already known, children get exactly that much
        int innerMaxW = knownW >= 0 ? c.constrainWidth(knownW) : c.maxWidth();
        int innerMaxH = knownH >= 0 ? c.constrainHeight(knownH) : c.maxHeight();
        Size content = measureContent(Constraints.loose(innerMaxW, innerMaxH));
        this.width = c.constrainWidth(knownW >= 0 ? knownW : content.width());
        this.height = c.constrainHeight(knownH >= 0 ? knownH : content.height());
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

    // Проход 2: расставляет детей; контейнеры переопределяют, затем вызывают super
    // Pass 2: positions children; containers override, then call super
    public void layout() {
        for (Widget c : children) c.layout();
    }

    // Пересчитать всё дерево; позже заменим флагами dirty / Re-layout the whole tree; to be replaced by dirty flags later
    public final void requestLayout() {
        Widget r = root();
        r.measure(Constraints.UNCONSTRAINED);
        r.layout();
    }

    // ---------- Состояние / State ----------

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isClipChildren() { return clipChildren; }
    public void setClipChildren(boolean clipChildren) { this.clipChildren = clipChildren; }

    // ---------- Фокус / Focus ----------

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

    // ---------- Отрисовка / Rendering ----------

    public final void render(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        renderSelf(g, mouseX, mouseY, partialTick);
        if (clipChildren) g.pushClip(absX(), absY(), absX() + width, absY() + height);
        for (Widget c : children) c.render(g, mouseX, mouseY, partialTick);
        if (clipChildren) g.popClip();
        renderOverlay(g, mouseX, mouseY, partialTick);
    }

    // Рисует сам виджет, без детей / Draws the widget itself, without children
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {}

    // Рисуется поверх детей и вне обрезки / Drawn above children and outside the clip
    protected void renderOverlay(UiGraphics g, int mouseX, int mouseY, float partialTick) {}

    // ---------- Диспетчеризация мыши через дерево / Mouse dispatch through the tree ----------
    // Дети проверяются с конца: верхний слой первым / Children checked in reverse: top layer first

    // Возвращает виджет, обработавший клик, или null / Returns the widget that handled the click, or null
    public final Widget dispatchMouseClicked(double mx, double my, int button) {
        if (!visible || !active) return null;
        if (clipChildren && !contains(mx, my)) return null;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (i >= children.size()) continue;
            Widget h = children.get(i).dispatchMouseClicked(mx, my, button);
            if (h != null) return h;
        }
        return contains(mx, my) && onMouseClicked(mx, my, button) ? this : null;
    }

    public final boolean dispatchMouseScrolled(double mx, double my, double delta) {
        if (!visible || !active) return false;
        if (clipChildren && !contains(mx, my)) return false;
        for (int i = children.size() - 1; i >= 0; i--) {
            if (i >= children.size()) continue;
            if (children.get(i).dispatchMouseScrolled(mx, my, delta)) return true;
        }
        return contains(mx, my) && onMouseScrolled(mx, my, delta);
    }

    // ---------- Обработчики событий / Event handlers ----------
    // true = событие обработано / true = event consumed

    // Идут через дерево (dispatch…) / Routed through the tree (dispatch…)
    protected boolean onMouseClicked(double mx, double my, int button) { return false; }
    protected boolean onMouseScrolled(double mx, double my, double delta) { return false; }

    // Адресуются конкретному виджету экраном / Sent to a specific widget by the screen
    public boolean onMouseReleased(double mx, double my, int button) { return false; }
    public boolean onMouseDragged(double mx, double my, int button, double dx, double dy) { return false; }
    public boolean onKeyPressed(int key, int scanCode, int modifiers) { return false; }
    public boolean onKeyReleased(int key, int scanCode, int modifiers) { return false; }
    public boolean onCharTyped(char c, int modifiers) { return false; }
    public void onFocusChanged(boolean focused) {}
}