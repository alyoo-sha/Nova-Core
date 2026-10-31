package org.mod.nova_core.ui.api;

import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;

// Вертикальный скролл-контейнер: окно просмотра + один контент-виджет, который сдвигается
// Vertical scroll container: a viewport + one content widget that gets shifted
public class ScrollView extends Widget {
    private static final int MIN_THUMB_HEIGHT = 16;

    private int scrollbarWidth = 6;
    // Шаг колеса мыши в пикселях / Mouse wheel step in pixels
    private int wheelStep = 14;
    private int trackColor = 0x40000000;
    private int thumbColor = 0xFF707070;
    private int thumbHoverColor = 0xFFA0A0A0;

    private Widget content;
    private int scroll = 0;

    // Перетаскивание ползунка / Thumb dragging
    private boolean dragging = false;
    private int grabOffset = 0;

    public ScrollView(int x, int y, int width, int height) {
        super(x, y, width, height);
        setClipChildren(true);
        this.content = new Widget(0, 0, 0, 0);
        add(content);
    }

    // ---------- Контент / Content ----------

    public Widget getContent() { return content; }

    public ScrollView setContent(Widget newContent) {
        remove(content);
        content = newContent;
        add(content);
        requestLayout();
        return this;
    }

    public ScrollView setScrollbarWidth(int w) { this.scrollbarWidth = w; return this; }
    public ScrollView setWheelStep(int step) { this.wheelStep = step; return this; }

    // ---------- Раскладка / Layout ----------

    @Override
    protected Size measureContent(Constraints c) {
        // Ширина контента фиксирована окном, высота свободная / Content width is set by the viewport, height is free
        int cw = Constraints.shrink(c.maxWidth(), scrollbarWidth);
        Constraints cc = cw == Constraints.UNBOUNDED
                ? Constraints.UNCONSTRAINED
                : new Constraints(cw, cw, 0, Constraints.UNBOUNDED);
        Size s = content.measure(cc);
        return new Size(s.width() + scrollbarWidth, Math.min(s.height(), c.maxHeight()));
    }

    @Override
    public void layout() {
        setScroll(scroll); // подрезать прокрутку после измерения / clamp scroll after measuring
        super.layout();
    }

    // ---------- Прокрутка / Scrolling ----------

    public int getScroll() { return scroll; }

    public int getMaxScroll() { return Math.max(0, content.getHeight() - getHeight()); }

    public void setScroll(int value) {
        scroll = Math.max(0, Math.min(value, getMaxScroll()));
        content.setPosition(0, -scroll);
    }

    public void scrollBy(int delta) { setScroll(scroll + delta); }

    // Показать область [top, bottom) контента / Make content range [top, bottom) visible
    public void scrollToShow(int top, int bottom) {
        if (top < scroll) setScroll(top);
        else if (bottom > scroll + getHeight()) setScroll(bottom - getHeight());
    }

    // ---------- Геометрия полосы / Scrollbar geometry ----------

    private int thumbHeight() {
        int h = getHeight();
        int contentH = Math.max(1, content.getHeight());
        return Math.min(h, Math.max(MIN_THUMB_HEIGHT, h * h / contentH));
    }

    private int thumbTop() {
        int max = getMaxScroll();
        if (max <= 0) return 0;
        return (getHeight() - thumbHeight()) * scroll / max;
    }

    private boolean inScrollbar(double mx) {
        int left = absX() + getWidth() - scrollbarWidth;
        return mx >= left && mx < absX() + getWidth();
    }

    // ---------- Отрисовка / Rendering ----------

    @Override
    protected void renderOverlay(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (getMaxScroll() <= 0) return;

        int left = absX() + getWidth() - scrollbarWidth;
        int right = absX() + getWidth();
        int top = absY();

        g.fill(left, top, right, top + getHeight(), trackColor);

        int ty = top + thumbTop();
        int th = thumbHeight();
        boolean hovered = dragging
                || (inScrollbar(mouseX) && mouseY >= ty && mouseY < ty + th);
        g.fill(left, ty, right, ty + th, hovered ? thumbHoverColor : thumbColor);
    }

    // ---------- События / Events ----------

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || getMaxScroll() <= 0 || !inScrollbar(mx)) return false;

        int ty = absY() + thumbTop();
        int th = thumbHeight();
        if (my >= ty && my < ty + th) {
            dragging = true;
            grabOffset = (int) my - ty;
        } else {
            // Клик по дорожке: прокрутка на страницу / Track click: page scroll
            scrollBy(my < ty ? -getHeight() : getHeight());
        }
        return true;
    }

    @Override
    public boolean onMouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!dragging) return false;
        int range = getHeight() - thumbHeight();
        if (range > 0) {
            int newTop = (int) my - grabOffset - absY();
            setScroll(Math.round((float) newTop * getMaxScroll() / range));
        }
        return true;
    }

    @Override
    public boolean onMouseReleased(double mx, double my, int button) {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    @Override
    protected boolean onMouseScrolled(double mx, double my, double delta) {
        int before = scroll;
        scrollBy((int) Math.round(-delta * wheelStep));
        // Если упёрлись в край, отдаём событие родителю / At the edge, let a parent handle it
        return scroll != before;
    }
}