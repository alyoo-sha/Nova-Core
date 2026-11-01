package org.mod.nova_core.ui.minecraft;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
import org.mod.nova_core.ui.theme.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public abstract class UIScreen extends Screen {
    // Настоящий корень дерева: содержимое, слой оверлеев и слой переноса / The real tree root: the content, the overlay layer and the drag layer
    private final Widget treeRoot = new Widget(0, 0, 0, 0);
    // Корень содержимого экрана; build() наполняет его / Root of the screen content; build() fills it
    protected final Widget root = new Widget(0, 0, 0, 0);
    // Слой оверлеев: попапы, тултипы, диалоги / The overlay layer: popups, tooltips, dialogs
    private final OverlayHost overlays = new OverlayHost();
    // Слой переноса: «призрак» и сеанс drag-and-drop / The drag layer: the ghost and the drag-and-drop session
    private final DragLayer dragLayer = new DragLayer();
    // Инспектор дерева виджетов; включается по F12 / The widget tree inspector; toggled with F12
    private final DebugLayer debug = new DebugLayer();
    // Один объект рисования на экран, на кадр подставляется GuiGraphics / One graphics object per screen, GuiGraphics is attached per frame
    private final McGraphics graphics = new McGraphics();

    // Виджет, забравший нажатие: ему идут drag и up / Widget that claimed the press: gets drag and up
    private Widget pressed = null;
    // Цепочка под курсором от цели к корню, для событий enter/leave / The chain under the cursor from the target to the root, for enter/leave events
    private final List<Widget> hoverPath = new ArrayList<>();
    private final List<Widget> newHoverPath = new ArrayList<>();

    protected UIScreen(Component title) {
        super(title);
        root.setSizeMode(SizeMode.FILL, SizeMode.FILL);
        treeRoot.setPointerTransparent(true);
        treeRoot.add(root);
        treeRoot.add(overlays);  // выше содержимого / above the content
        treeRoot.add(dragLayer); // выше оверлеев / above the overlays
        treeRoot.add(debug);     // инспектор поверх всего / the inspector above everything
        treeRoot.setDebugName("screen");
        root.setDebugName("content");
        overlays.setDebugName("overlays");
        dragLayer.setDebugName("drag");
        debug.setDebugName("inspector");
        // Смена темы пересчитывает раскладку: размеры могут зависеть от стиля
        // A theme change re-lays out the tree: sizes may depend on the style
        treeRoot.bind(ThemeManager.signal(), t -> treeRoot.invalidateLayoutDeep());
    }

    // Слой оверлеев этого экрана; из любого виджета: OverlayHost.of(widget) / This screen's overlay layer; from any widget: OverlayHost.of(widget)
    protected final OverlayHost overlays() { return overlays; }

    // Строит дерево виджетов; вызывается при открытии и ресайзе / Builds the widget tree; called on open and resize
    protected abstract void build(Widget root);

    // Виджет, который получит фокус сразу после построения; null = без фокуса. Вызывается и при ресайзе
    // The widget that gets focus right after the build; null = no focus. Also called on resize
    protected Widget initialFocus() { return null; }

    @Override
    protected void init() {
        dragLayer.cancelImmediately(); // перенос не переживает пересборку / a drag does not survive a rebuild
        debug.clearSelection(); // зафиксированный виджет исчезает вместе со старым деревом / a locked widget vanishes with the old tree
        treeRoot.unmount(); // старое дерево: onUnmount и cleanup / old tree: onUnmount and cleanups
        overlays.clearImmediately(); // якоря оверлеев исчезают вместе со старым деревом / overlay anchors vanish with the old tree
        root.clearChildren();
        treeRoot.setBounds(0, 0, width, height);
        root.setBounds(0, 0, width, height);
        pressed = null;
        hoverPath.clear();
        build(root);
        treeRoot.performLayout(Constraints.tight(width, height));
        treeRoot.mount(); // после раскладки размеры уже верны / sizes are valid after layout

        Widget initial = initialFocus();
        if (initial != null) FocusNavigator.focus(initial);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        // Один раз за кадр: двигает все активные анимации / Once per frame: advances all running animations
        Animator.shared().tick(System.nanoTime());
        overlays.flush();
        overlays.setPointer(mouseX, mouseY);
        dragLayer.tick(mouseX, mouseY);
        // Дёшево, если ничего не менялось / Cheap if nothing changed
        treeRoot.performLayout(Constraints.tight(width, height));
        updateHover(mouseX, mouseY);
        // Обработчики enter/leave могли открыть оверлей или изменить размеры / enter/leave handlers may have opened an overlay or changed sizes
        treeRoot.performLayout(Constraints.tight(width, height));
        graphics.begin(g);
        treeRoot.render(graphics, mouseX, mouseY, partialTick);
        graphics.end();
    }

    @Override
    public void removed() {
        dragLayer.cancelImmediately();
        pressed = null;
        treeRoot.unmount();
        overlays.clearImmediately();
        hoverPath.clear();
        super.removed();
    }

    // ---------- Hover ----------

    private void updateHover(int mx, int my) {
        // Во время переноса ничего не считается наведённым / During a drag nothing counts as hovered
        Widget target = dragLayer.isDragging() ? null : treeRoot.hitTest(mx, my);
        newHoverPath.clear();
        for (Widget w = target; w != null; w = w.getParent()) newHoverPath.add(w);

        // Уход: от самого глубокого к корню / Leave: from the deepest to the root
        for (int i = 0; i < hoverPath.size(); i++) {
            Widget w = hoverPath.get(i);
            if (!newHoverPath.contains(w) && w.isMounted()) w.dispatchEvent(PointerEvent.leave(mx, my));
        }
        // Вход: от корня к самому глубокому / Enter: from the root to the deepest
        for (int i = newHoverPath.size() - 1; i >= 0; i--) {
            Widget w = newHoverPath.get(i);
            if (!hoverPath.contains(w)) w.dispatchEvent(PointerEvent.enter(mx, my));
        }

        hoverPath.clear();
        hoverPath.addAll(newHoverPath);
    }

    // ---------- Мышь / Mouse ----------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (debug.handleMouseDown(mx, my, button)) return true;
        if (dragLayer.isDragging()) return true; // во время переноса остальные нажатия игнорируются / other presses are ignored during a drag
        pressed = null;
        Widget target = treeRoot.hitTest(mx, my);
        if (target == null) {
            treeRoot.clearFocus();
            return false;
        }

        // Фокус получает ближайший focusable-предок цели / Focus goes to the nearest focusable ancestor of the target
        Widget focusable = target;
        while (focusable != null && !focusable.isFocusable()) focusable = focusable.getParent();
        if (focusable != null) focusable.requestFocus();
        else treeRoot.clearFocus();

        // Запоминаем возможный источник переноса, даже если нажатие заберёт другой виджет
        // Remember a possible drag source even if another widget claims the press
        dragLayer.press(target, mx, my, button);

        PointerEvent event = PointerEvent.down(mx, my, button);
        target.dispatchEvent(event);
        if (event.getStoppedBy() instanceof Widget claimer) pressed = claimer;
        return event.isPropagationStopped();
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragLayer.pointerReleased(mx, my, button)) {
            pressed = null;
            return true;
        }

        Widget claimer = pressed;
        pressed = null;
        if (claimer == null) return false;

        PointerEvent event = PointerEvent.up(mx, my, button);
        claimer.dispatchEvent(event);
        return event.isPropagationStopped();
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        boolean wasDragging = dragLayer.isDragging();
        if (dragLayer.pointerDragged(mx, my)) {
            // Перенос только что начался: тот, кто забрал нажатие, больше не получит ни drag, ни up
            // The drag has just begun: whoever claimed the press gets neither drag nor up anymore
            if (!wasDragging) cancelPressed(mx, my);
            return true;
        }

        if (pressed == null) return false;

        PointerEvent event = PointerEvent.drag(mx, my, button, dx, dy);
        pressed.dispatchEvent(event);
        return event.isPropagationStopped();
    }

    private void cancelPressed(double mx, double my) {
        Widget claimer = pressed;
        pressed = null;
        if (claimer != null) claimer.dispatchEvent(PointerEvent.cancel(mx, my));
    }

    @Override
    public void mouseMoved(double mx, double my) {
        Widget target = treeRoot.hitTest(mx, my);
        if (target != null) target.dispatchEvent(PointerEvent.move(mx, my));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        Widget target = treeRoot.hitTest(mx, my);
        if (target == null) return false;

        PointerEvent event = PointerEvent.scroll(mx, my, delta);
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

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (debug.handleKey(key, modifiers)) return true;
        // Escape во время переноса отменяет перенос, а не закрывает экран / Escape during a drag cancels the drag instead of closing the screen
        if (key == Keys.ESCAPE && dragLayer.isDragging()) {
            dragLayer.cancel();
            return true;
        }

        KeyboardEvent event = KeyboardEvent.down(key, scanCode, modifiers);
        keyTarget().dispatchEvent(event);
        if (event.isPropagationStopped()) return true;
        // Никто не забрал клавишу: Tab и стрелки двигают фокус / Nobody claimed the key: Tab and arrows move the focus
        if (handleNavigationKey(key, modifiers)) return true;
        return super.keyPressed(key, scanCode, modifiers); // ESC закрывает экран / ESC closes the screen
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        KeyboardEvent event = KeyboardEvent.up(key, scanCode, modifiers);
        keyTarget().dispatchEvent(event);
        if (event.isPropagationStopped()) return true;
        return super.keyReleased(key, scanCode, modifiers);
    }

    @Override
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

        // Область: верхний попап или модальное окно, иначе содержимое экрана / Scope: the top popup or modal, otherwise the screen content
        Widget scope = overlays.navigationScope();
        if (scope == null) scope = root;

        // Фокус вне области (например, на кнопке под попапом) считаем отсутствующим
        // Focus outside the scope (e.g. on a button below a popup) counts as missing
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