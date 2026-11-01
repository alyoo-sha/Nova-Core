package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.animation.AnimatedValue;
import org.mod.nova_core.ui.animation.Easing;
import org.mod.nova_core.ui.animation.Interpolators;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.style.ScrollbarStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.List;

// Вертикальный скролл-контейнер: окно просмотра + один контент-виджет, который сдвигается
// Vertical scroll container: a viewport + one content widget that gets shifted
public class ScrollView extends Widget {
    private final StyleRef<ScrollbarStyle> style = new StyleRef<>(Styles.SCROLLBAR);
    // Прокрутка как плавное значение: peek() = то, что видно, target() = куда идём
    // Scroll as a smooth value: peek() = what is shown, target() = where we are heading
    private final AnimatedValue<Float> scrollAnim =
            new AnimatedValue<>(Interpolators.FLOAT, 0f, 140L, Easing.EASE_OUT);

    // Шаг колеса мыши в пикселях / Mouse wheel step in pixels
    private int wheelStep = 14;
    private boolean smooth = true;

    private Widget content;

    // Перетаскивание ползунка / Thumb dragging
    private boolean dragging = false;
    private int grabOffset = 0;

    public ScrollView(int x, int y, int width, int height) {
        super(x, y, width, height);
        setClipChildren(true);
        this.content = new Widget(0, 0, 0, 0);
        add(content);
    }

    // ---------- Контент и стиль / Content and style ----------

    public Widget getContent() { return content; }

    public ScrollView setContent(Widget newContent) {
        remove(content);
        content = newContent;
        add(content);
        requestLayout();
        return this;
    }

    public ScrollbarStyle getStyle() { return style.get(); }

    // Свой стиль полосы; null = брать из темы / Own scrollbar style; null = use the theme
    public ScrollView setStyle(ScrollbarStyle value) {
        style.set(value);
        requestLayout(); // ширина полосы влияет на размер контента / the bar width affects the content size
        return this;
    }

    // Удобный сеттер: фиксирует текущий стиль темы с новой шириной / Convenience setter: freezes the current theme style with a new width
    public ScrollView setScrollbarWidth(int w) { return setStyle(style.get().withWidth(w)); }

    public ScrollView setWheelStep(int step) { this.wheelStep = step; return this; }

    // Плавная прокрутка колесом и кликом по дорожке; false = мгновенная / Smooth scrolling for the wheel and track clicks; false = instant
    public ScrollView setSmoothScroll(boolean smooth) { this.smooth = smooth; return this; }

    // ---------- Раскладка / Layout ----------

    @Override
    protected Size measureContent(Constraints c) {
        int bar = style.get().width();
        // Ширина контента фиксирована окном, высота свободная / Content width is set by the viewport, height is free
        int cw = Constraints.shrink(c.maxWidth(), bar);
        Constraints cc = cw == Constraints.UNBOUNDED
                ? Constraints.UNCONSTRAINED
                : new Constraints(cw, cw, 0, Constraints.UNBOUNDED);
        Size s = content.measure(cc);
        return new Size(s.width() + bar, Math.min(s.height(), c.maxHeight()));
    }

    @Override
    protected void layoutChildren() {
        // Если контент стал короче, подрезать прокрутку / If the content got shorter, clamp the scroll
        if (getTargetScroll() > getMaxScroll()) setScroll(getMaxScroll());
        else applyScroll();
        super.layoutChildren();
    }

    @Override
    protected void onUnmount() {
        scrollAnim.finish();
        applyScroll();
    }

    // ---------- Прокрутка / Scrolling ----------

    private static int toInt(Float v) { return Math.round(v.floatValue()); }

    // Положение, которое сейчас видно / The position currently shown
    public int getScroll() { return toInt(scrollAnim.peek()); }

    // Положение, к которому идёт прокрутка / The position the scroll is heading to
    public int getTargetScroll() { return toInt(scrollAnim.target()); }

    public int getMaxScroll() { return Math.max(0, content.getHeight() - getHeight()); }

    private int clamp(int value) { return Math.max(0, Math.min(value, getMaxScroll())); }

    private void applyScroll() { content.setPosition(0, -getScroll()); }

    // Мгновенно / Instantly
    public void setScroll(int value) {
        scrollAnim.jumpTo((float) clamp(value));
        applyScroll();
    }

    // Плавно (если включено) / Smoothly (if enabled)
    public void scrollTo(int value) {
        int v = clamp(value);
        if (smooth) scrollAnim.setTarget((float) v);
        else setScroll(v);
    }

    public void scrollBy(int delta) { scrollTo(getTargetScroll() + delta); }

    // Показать область [top, bottom) контента / Make content range [top, bottom) visible
    public void scrollToShow(int top, int bottom) {
        int s = getTargetScroll();
        if (top < s) scrollTo(top);
        else if (bottom > s + getHeight()) scrollTo(bottom - getHeight());
    }

    // ---------- Геометрия полосы / Scrollbar geometry ----------

    private int thumbHeight() {
        int h = getHeight();
        int contentH = Math.max(1, content.getHeight());
        return Math.min(h, Math.max(style.get().minThumbHeight(), h * h / contentH));
    }

    private int thumbTop() {
        int max = getMaxScroll();
        if (max <= 0) return 0;
        return (getHeight() - thumbHeight()) * getScroll() / max;
    }

    private boolean inScrollbar(double mx) {
        int left = absX() + getWidth() - style.get().width();
        return mx >= left && mx < absX() + getWidth();
    }

    // ---------- Отрисовка / Rendering ----------

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Анимация двигает значение между кадрами; здесь переносим его на контент до отрисовки детей
        // The animation moves the value between frames; here we apply it to the content before the children are drawn
        applyScroll();
    }

    @Override
    protected void renderOverlay(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (getMaxScroll() <= 0) return;
        ScrollbarStyle s = style.get();

        int left = absX() + getWidth() - s.width();
        int right = absX() + getWidth();
        int top = absY();

        g.fill(left, top, right, top + getHeight(), s.track());

        int ty = top + thumbTop();
        int th = thumbHeight();
        boolean hovered = dragging
                || (inScrollbar(mouseX) && mouseY >= ty && mouseY < ty + th);
        g.fill(left, ty, right, ty + th, hovered ? s.thumbHover() : s.thumb());
    }

    // ---------- События / Events ----------

    @Override
    protected void onPointerDown(PointerEvent e) {
        if (e.getButton() != MouseButton.LEFT || getMaxScroll() <= 0 || !inScrollbar(e.getX())) return;

        int ty = absY() + thumbTop();
        int th = thumbHeight();
        if (e.getY() >= ty && e.getY() < ty + th) {
            dragging = true;
            grabOffset = (int) e.getY() - ty;
        } else {
            // Клик по дорожке: прокрутка на страницу / Track click: page scroll
            scrollBy(e.getY() < ty ? -getHeight() : getHeight());
        }
        e.stopPropagation();
    }

    @Override
    protected void onPointerDrag(PointerEvent e) {
        if (!dragging) return;
        int range = getHeight() - thumbHeight();
        if (range > 0) {
            int newTop = (int) e.getY() - grabOffset - absY();
            setScroll(Math.round((float) newTop * getMaxScroll() / range)); // мгновенно: ползунок не должен отставать от курсора
        }
        e.stopPropagation();
    }

    @Override
    protected void onPointerCancel(PointerEvent e) {
        dragging = false;
    }

    @Override
    protected void onPointerUp(PointerEvent e) {
        if (!dragging) return;
        dragging = false;
        e.stopPropagation();
    }

    @Override
    protected void onPointerScroll(PointerEvent e) {
        int before = getTargetScroll();
        scrollBy((int) Math.round(-e.getScrollDelta() * wheelStep));
        // Если упёрлись в край, событие идёт дальше к родителю / At the edge, the event goes on to the parent
        if (getTargetScroll() != before) e.stopPropagation();
    }

    @Override
    public void describe(List<String> lines) {
        lines.add("scroll " + getScroll() + "/" + getMaxScroll() + " (target " + getTargetScroll()
                + ")  content " + content.getWidth() + "x" + content.getHeight());
    }
}