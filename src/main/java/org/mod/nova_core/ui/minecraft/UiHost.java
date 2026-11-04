package org.mod.nova_core.ui.minecraft;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.mod.nova_core.ui.animation.Animator;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.debug.DebugLayer;
import org.mod.nova_core.ui.dnd.DragLayer;
import org.mod.nova_core.ui.input.KeyboardEvent;
import org.mod.nova_core.ui.input.Keys;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.navigation.Direction;
import org.mod.nova_core.ui.navigation.FocusNavigator;
import org.mod.nova_core.ui.overlay.OverlayHost;
import org.mod.nova_core.ui.style.BackdropStyle;
import org.mod.nova_core.ui.style.Gradient;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Дерево виджетов со слоями и вся обработка ввода. Экран (Screen) только передаёт ему вызовы.
// Возвращаемое boolean: true = хост обработал событие, экран не должен делать ничего своего
// The widget tree with its layers and all input handling. A Screen only forwards calls to it.
// The returned boolean: true = the host handled the event, the screen must not do anything of its own
public final class UiHost {
    // Настоящий корень: содержимое, оверлеи, перенос, инспектор / The real root: content, overlays, drag, inspector
    private final Widget treeRoot = new Widget(0, 0, 0, 0);
    private final Widget root = new Widget(0, 0, 0, 0);
    private final OverlayHost overlays = new OverlayHost();
    private final DragLayer dragLayer = new DragLayer();
    private final DebugLayer debug = new DebugLayer();
    private final McGraphics graphics = new McGraphics();

    private final Consumer<Widget> builder;
    private final Supplier<Widget> initialFocus;

    private int width = 0;
    private int height = 0;
    // Виджет, забравший нажатие: ему идут drag и up / The widget that claimed the press: gets drag and up
    private Widget pressed = null;
    private final List<Widget> hoverPath = new ArrayList<>();
    private final List<Widget> newHoverPath = new ArrayList<>();

    // Замеры кадра для инспектора / Frame measurements for the inspector
    private long prepNanos = 0L;
    private long drawNanos = 0L;

    public UiHost(Consumer<Widget> builder, Supplier<Widget> initialFocus) {
        this.builder = builder;
        this.initialFocus = initialFocus;
        root.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        treeRoot.setPointerTransparent(true);
        treeRoot.add(root);
        treeRoot.add(overlays);
        treeRoot.add(dragLayer);
        treeRoot.add(debug);
        treeRoot.setDebugName("screen");
        root.setDebugName("content");
        overlays.setDebugName("overlays");
        dragLayer.setDebugName("drag");
        debug.setDebugName("inspector");
        // Смена темы пересчитывает раскладку: размеры могут зависеть от стиля / A theme change re-lays out the tree: sizes may depend on the style
        treeRoot.bind(ThemeManager.signal(), t -> treeRoot.invalidateLayoutDeep());
    }

    public Widget root() { return root; }

    public OverlayHost overlays() { return overlays; }

    public boolean hasModal() { return overlays.hasModal(); }

    // ---------- Жизненный цикл / Lifecycle ----------

    // Вызывать из Screen.init(): дерево пересобирается / Call from Screen.init(): the tree is rebuilt
    public void init(int width, int height) {
        this.width = width;
        this.height = height;
        dragLayer.cancelImmediately();
        debug.clearSelection();
        treeRoot.unmount();
        overlays.clearImmediately();
        root.clearChildren();
        treeRoot.setBounds(0, 0, width, height);
        root.setBounds(0, 0, width, height);
        pressed = null;
        hoverPath.clear();
        builder.accept(root);
        treeRoot.performLayout(Constraints.tight(width, height));
        treeRoot.mount();

        Widget initial = initialFocus.get();
        if (initial != null) FocusNavigator.focus(initial);
    }

    public void removed() {
        dragLayer.cancelImmediately();
        pressed = null;
        treeRoot.unmount();
        overlays.clearImmediately();
        hoverPath.clear();
    }

    // ---------- Кадр / Frame ----------

    // Фон за экраном из темы / The backdrop behind the screen from the theme
    public void renderBackdrop(GuiGraphics g) {
        BackdropStyle style = ThemeManager.current().get(Styles.BACKDROP);
        Gradient f = style.fill();
        graphics.begin(g);
        graphics.fillRoundedGradient(0, 0, width, height, 0f,
                f.topLeft(), f.topRight(), f.bottomLeft(), f.bottomRight());
        graphics.end();
    }

    // Один раз за кадр до отрисовки: анимации, раскладка, hover / Once per frame before drawing: animations, layout, hover
    public void prepare(int mx, int my) {
        long start = System.nanoTime();
        Animator.shared().tick(System.nanoTime());
        overlays.flush();
        overlays.setPointer(mx, my);
        dragLayer.tick(mx, my);
        Constraints c = Constraints.tight(width, height);
        treeRoot.performLayout(c);
        updateHover(mx, my);
        // Обработчики enter/leave могли открыть оверлей или изменить размеры / enter/leave handlers may have opened an overlay or changed sizes
        treeRoot.performLayout(c);
        prepNanos = System.nanoTime() - start;
        drawNanos = 0L;
    }

    // Содержимое экрана / The screen content
    public void renderContent(GuiGraphics g, int mx, int my, float partialTick) {
        long start = System.nanoTime();
        graphics.begin(g);
        root.render(graphics, mx, my, partialTick);
        graphics.end();
        drawNanos += System.nanoTime() - start;
    }

    // Слои поверх всего: оверлеи, «призрак» переноса, инспектор. Сдвинуты по глубине выше предметов в слотах (z 150-200)
    // и ниже ванильных подсказок (z 400)
    // Layers above everything: overlays, the drag ghost, the inspector. Shifted in depth above the items in slots (z 150-200)
    // and below vanilla tooltips (z 400)
    public void renderTop(GuiGraphics g, int mx, int my, float partialTick) {
        long start = System.nanoTime();
        g.pose().pushPose();
        g.pose().translate(0f, 0f, 300f);
        graphics.begin(g);
        overlays.render(graphics, mx, my, partialTick);
        dragLayer.render(graphics, mx, my, partialTick);
        debug.render(graphics, mx, my, partialTick);
        graphics.end();
        g.pose().popPose();
        drawNanos += System.nanoTime() - start;

        if (debug.isEnabled()) {
            debug.setStats(String.format(Locale.ROOT, "prep %.2f ms  draw %.2f ms  widgets %d",
                    prepNanos / 1_000_000.0, drawNanos / 1_000_000.0, countWidgets(treeRoot)));
        }
    }

    private static int countWidgets(Widget w) {
        int n = 1;
        for (Widget c : w.getChildren()) n += countWidgets(c);
        return n;
    }

    private void updateHover(int mx, int my) {
        // Во время переноса ничего не считается наведённым / During a drag nothing counts as hovered
        Widget target = dragLayer.isDragging() ? null : treeRoot.hitTest(mx, my);
        newHoverPath.clear();
        for (Widget w = target; w != null; w = w.getParent()) newHoverPath.add(w);

        for (int i = 0; i < hoverPath.size(); i++) {
            Widget w = hoverPath.get(i);
            if (!newHoverPath.contains(w) && w.isMounted()) w.dispatchEvent(PointerEvent.leave(mx, my));
        }
        for (int i = newHoverPath.size() - 1; i >= 0; i--) {
            Widget w = newHoverPath.get(i);
            if (!hoverPath.contains(w)) w.dispatchEvent(PointerEvent.enter(mx, my));
        }

        hoverPath.clear();
        hoverPath.addAll(newHoverPath);
    }

    // ---------- Мышь / Mouse ----------

    // Зажатые модификаторы; Ctrl на macOS соответствует Cmd / Held modifiers; Ctrl on macOS corresponds to Cmd
    private static int modifiers() {
        return (Screen.hasShiftDown() ? Keys.MOD_SHIFT : 0)
                | (Screen.hasControlDown() ? Keys.MOD_CONTROL : 0)
                | (Screen.hasAltDown() ? Keys.MOD_ALT : 0);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (debug.handleMouseDown(mx, my, button)) return true;
        if (dragLayer.isDragging()) return true; // во время переноса остальные нажатия игнорируются / other presses are ignored during a drag
        pressed = null;
        Widget target = treeRoot.hitTest(mx, my);
        if (target == null) {
            treeRoot.clearFocus();
            return false;
        }

        Widget focusable = target;
        while (focusable != null && !focusable.isFocusable()) focusable = focusable.getParent();
        if (focusable != null) focusable.requestFocus();
        else treeRoot.clearFocus();

        dragLayer.press(target, mx, my, button);

        PointerEvent event = PointerEvent.down(mx, my, button, modifiers());
        target.dispatchEvent(event);
        if (event.getStoppedBy() instanceof Widget claimer) pressed = claimer;
        // Всё, что лежит в оверлеях, считается занятым: клик не должен «проваливаться» к слотам под ним
        // Everything inside overlays counts as taken: a click must not fall through to the slots below it
        return event.isPropagationStopped() || isInside(target, overlays);
    }

    public boolean mouseReleased(double mx, double my, int button) {
        if (dragLayer.pointerReleased(mx, my, button)) {
            pressed = null;
            return true;
        }
        Widget claimer = pressed;
        pressed = null;
        if (claimer == null) return false;

        PointerEvent event = PointerEvent.up(mx, my, button, modifiers());
        claimer.dispatchEvent(event);
        return event.isPropagationStopped();
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        boolean wasDragging = dragLayer.isDragging();
        if (dragLayer.pointerDragged(mx, my)) {
            // Перенос только что начался: тот, кто забрал нажатие, больше не получит ни drag, ни up
            // The drag has just begun: whoever claimed the press gets neither drag nor up anymore
            if (!wasDragging) {
                Widget claimer = pressed;
                pressed = null;
                if (claimer != null) claimer.dispatchEvent(PointerEvent.cancel(mx, my));
            }
            return true;
        }
        if (pressed == null) return false;

        PointerEvent event = PointerEvent.drag(mx, my, button, dx, dy, modifiers());
        pressed.dispatchEvent(event);
        return event.isPropagationStopped();
    }

    public void mouseMoved(double mx, double my) {
        Widget target = treeRoot.hitTest(mx, my);
        if (target != null) target.dispatchEvent(PointerEvent.move(mx, my));
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        Widget target = treeRoot.hitTest(mx, my);
        if (target == null) return false;

        PointerEvent event = PointerEvent.scroll(mx, my, delta, modifiers());
        target.dispatchEvent(event);
        return event.isPropagationStopped();
    }

    // ---------- Клавиатура / Keyboard ----------

    // Цель: виджет в фокусе; без фокуса при открытом модальном окне само окно, иначе корень содержимого
    // Target: the focused widget; with no focus and a modal open the modal itself, otherwise the content root
    private Widget keyTarget() {
        Widget focus = treeRoot.getFocusOwner();
        if (focus != null) return focus;
        Widget modal = overlays.modalScope();
        return modal != null ? modal : root;
    }

    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (debug.handleKey(key, modifiers)) return true;
        // Escape во время переноса отменяет перенос / Escape during a drag cancels the drag
        if (key == Keys.ESCAPE && dragLayer.isDragging()) {
            dragLayer.cancel();
            return true;
        }
        // Alt+стрелки всегда двигают фокус, даже когда стрелки нужны полю ввода или ползунку
        // Alt+arrows always move the focus, even when the arrows are needed by a text field or a slider
        if ((modifiers & Keys.MOD_ALT) != 0 && directionOf(key) != null
                && handleNavigationKey(key, modifiers & ~Keys.MOD_ALT)) {
            return true;
        }

        KeyboardEvent event = KeyboardEvent.down(key, scanCode, modifiers);
        keyTarget().dispatchEvent(event);
        if (event.isPropagationStopped()) return true;
        if (handleNavigationKey(key, modifiers)) return true;

        // Пока набирается текст, остальные клавиши (например, E для закрытия инвентаря) экрану не отдаём
        // While text is typed, other keys (e.g. E closing an inventory) are not handed to the screen
        Widget focus = treeRoot.getFocusOwner();
        return focus != null && focus.isTextInput() && key != Keys.ESCAPE;
    }

    public boolean keyReleased(int key, int scanCode, int modifiers) {
        KeyboardEvent event = KeyboardEvent.up(key, scanCode, modifiers);
        keyTarget().dispatchEvent(event);
        return event.isPropagationStopped();
    }

    public boolean charTyped(char c, int modifiers) {
        KeyboardEvent event = KeyboardEvent.typed(c, modifiers);
        keyTarget().dispatchEvent(event);
        return event.isPropagationStopped();
    }

    // ---------- Навигация фокуса / Focus navigation ----------

    private boolean handleNavigationKey(int key, int modifiers) {
        if ((modifiers & (Keys.MOD_CONTROL | Keys.MOD_ALT | Keys.MOD_SUPER)) != 0) return false;

        Direction direction = directionOf(key);
        if (key != Keys.TAB && direction == null) return false;

        Widget scope = overlays.navigationScope();
        if (scope == null) scope = root;

        Widget current = treeRoot.getFocusOwner();
        if (current != null && !isInside(current, scope)) current = null;

        Widget target = key == Keys.TAB
                ? FocusNavigator.next(scope, current, (modifiers & Keys.MOD_SHIFT) == 0)
                : FocusNavigator.find(scope, current, direction);
        if (target == null) return false;

        FocusNavigator.focus(target);
        return true;
    }

    private static Direction directionOf(int key) {
        if (key == Keys.UP) return Direction.UP;
        if (key == Keys.DOWN) return Direction.DOWN;
        if (key == Keys.LEFT) return Direction.LEFT;
        if (key == Keys.RIGHT) return Direction.RIGHT;
        return null;
    }

    private static boolean isInside(Widget w, Widget ancestor) {
        for (Widget p = w; p != null; p = p.getParent()) {
            if (p == ancestor) return true;
        }
        return false;
    }
}