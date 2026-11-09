package org.mod.nova_core.ui.debug;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.TextWrap;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.Keys;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.SizeMode;

import java.util.ArrayList;
import java.util.List;

// Слой инспектора поверх всего экрана. Ввод не принимает: хост (UIScreen) передаёт ему клавиши F12 и Alt+... и среднюю кнопку мыши
// The inspector layer above the whole screen. Takes no input: the host (UIScreen) passes it F12, Alt+... keys and the middle mouse button
public final class DebugLayer extends Widget {
    private static final int[] DEPTH_COLORS = {
            0x90FF5050, 0x90FFB040, 0x9050E050, 0x9040C8E0, 0x906080FF, 0x90D060FF};
    private static final int SELECTION_FILL = 0x403A78C8;
    private static final int SELECTION_BORDER = 0xFF4090FF;
    private static final int MARGIN_FILL = 0x60FF9030;
    private static final int PARENT_BORDER = 0xA040C060;
    private static final int BOX_BACKGROUND = 0xE0101018;
    private static final int BOX_BORDER = 0xFF5060A0;
    private static final int COLOR_HEADER = 0xFF70A0FF;
    private static final int COLOR_NAME = 0xFFFFD060;
    private static final int COLOR_TEXT = 0xFFE8E8E8;
    private static final int COLOR_DIM = 0xFF8890B0;
    private static final int BOX_PADDING = 4;
    private static final int BOX_MARGIN = 4;
    private static final int MAX_BOX_WIDTH = 340;

    private record Line(String text, int color) {}

    private boolean enabled = false;
    private boolean showAllBounds = false;
    // Зафиксированный виджет; null = выбирается тот, что под курсором / The locked widget; null = the one under the cursor is selected
    private Widget locked = null;
    private int pointerX = 0;
    private int pointerY = 0;

    private String stats = "";

    // Строка замеров, которую выставляет хост каждый кадр / The measurements line the host sets every frame
    public void setStats(String stats) { this.stats = stats; }

    public DebugLayer() {
        super(0, 0, 0, 0);
        setSizeMode(SizeMode.FILL, SizeMode.FILL);
        setPointerTransparent(true);
        // Неактивный слой пропускается при поиске цели под курсором и не получает события
        // An inactive layer is skipped when looking for the target under the cursor and receives no events
        setActive(false);
    }

    // ---------- Состояние / State ----------

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) locked = null;
    }

    public boolean isLocked() { return locked != null; }

    public boolean isShowAllBounds() { return showAllBounds; }

    public void setShowAllBounds(boolean showAllBounds) { this.showAllBounds = showAllBounds; }

    // Сбросить зафиксированный виджет; нужно при пересборке дерева / Drop the locked widget; needed when the tree is rebuilt
    public void clearSelection() { locked = null; }

    // Зафиксировать виджет под курсором или снять фиксацию / Lock the widget under the cursor or release the lock
    public void toggleLock() {
        if (locked != null) {
            locked = null;
            return;
        }
        locked = pick(root(), pointerX, pointerY);
    }

    // ---------- Ввод от хоста / Input from the host ----------

    // true = клавиша обработана / true = the key was handled
    public boolean handleKey(int key, int modifiers) {
        boolean ctrl = (modifiers & Keys.MOD_CONTROL) != 0;
        boolean shift = (modifiers & Keys.MOD_SHIFT) != 0;
        boolean alt = (modifiers & Keys.MOD_ALT) != 0;
        boolean sup = (modifiers & Keys.MOD_SUPER) != 0;

        boolean toggle = (key == Keys.F12 && !ctrl && !alt && !sup) || (key == Keys.I && ctrl && shift && !alt);
        if (toggle) {
            setEnabled(!enabled);
            return true;
        }
        if (!enabled || !alt) return false;

        if (key == Keys.B) {
            showAllBounds = !showAllBounds;
            return true;
        }
        if (key == Keys.L) {
            toggleLock();
            return true;
        }
        if (key == Keys.P) {
            DebugLog.log(WidgetInfo.dump(root()));
            return true;
        }

        Widget sel = selection();
        if (key == Keys.C) {
            if (sel != null) UiContext.platform().setClipboard(String.join("\n", WidgetInfo.lines(sel)));
            return true;
        }
        // Правка выбранного: Alt+Shift+стрелки двигают, Alt+Ctrl+стрелки меняют размер, Alt+[ и Alt+] прозрачность
        // Editing the selected one: Alt+Shift+arrows move, Alt+Ctrl+arrows resize, Alt+[ and Alt+] opacity
        if (sel != null && edit(sel, key, shift, ctrl)) return true;
        if (key == Keys.UP || key == Keys.DOWN || key == Keys.LEFT || key == Keys.RIGHT) {
            Widget next = sel == null ? null : relative(sel, key);
            if (next != null) locked = next;
            return true;
        }
        return false;
    }

    private boolean edit(Widget w, int key, boolean shift, boolean ctrl) {
        int dx = key == Keys.LEFT ? -1 : key == Keys.RIGHT ? 1 : 0;
        int dy = key == Keys.UP ? -1 : key == Keys.DOWN ? 1 : 0;
        if ((dx != 0 || dy != 0) && shift) {
            w.moveTo(w.getX() + dx, w.getY() + dy);
            return true;
        }
        if ((dx != 0 || dy != 0) && ctrl) {
            w.setSize(Math.max(0, w.getWidth() + dx), Math.max(0, w.getHeight() + dy));
            return true;
        }
        if (key == Keys.LEFT_BRACKET || key == Keys.RIGHT_BRACKET) {
            float step = key == Keys.RIGHT_BRACKET ? 0.1f : -0.1f;
            w.setOpacity(Math.round((w.getOpacity() + step) * 10f) / 10f);
            return true;
        }
        return false;
    }

    // Средняя кнопка фиксирует выбор. true = событие обработано / The middle button locks the selection. true = the event was handled
    public boolean handleMouseDown(double x, double y, int button) {
        if (!enabled || button != MouseButton.MIDDLE) return false;
        pointerX = (int) x;
        pointerY = (int) y;
        toggleLock();
        return true;
    }

    // ---------- Выбор виджета / Picking a widget ----------

    private Widget selection() {
        if (locked != null) {
            if (locked.root() != root()) locked = null; // убран из дерева / removed from the tree
            else return locked;
        }
        return pick(root(), pointerX, pointerY);
    }

    // Самый глубокий видимый виджет под точкой. Неактивные виджеты тоже выбираются, а прозрачные для указателя нет
    // (но их потомки выбираются). Слой инспектора пропускается
    // The deepest visible widget under the point. Inactive widgets are picked too, pointer-transparent ones are not
    // (but their descendants are). The inspector layer is skipped
    private Widget pick(Widget w, double mx, double my) {
        if (w == this || !w.isVisible() || w.getOpacity() <= 0f) return null;
        boolean inside = w.contains(mx, my);
        if (w.isClipChildren() && !inside) return null;
        List<Widget> kids = w.getChildren();
        for (int i = kids.size() - 1; i >= 0; i--) {
            Widget hit = pick(kids.get(i), mx, my);
            if (hit != null) return hit;
        }
        return inside && !w.isPointerTransparent() ? w : null;
    }

    // Родитель, первый ребёнок или сосед / The parent, the first child or a sibling
    private Widget relative(Widget sel, int key) {
        if (key == Keys.UP) return sel.getParent();
        if (key == Keys.DOWN) {
            for (Widget c : sel.getChildren()) {
                if (c != this && c.isVisible()) return c;
            }
            return null;
        }
        Widget parent = sel.getParent();
        if (parent == null) return null;
        List<Widget> siblings = parent.getChildren();
        int idx = siblings.indexOf(sel);
        int step = key == Keys.RIGHT ? 1 : -1;
        for (int i = idx + step; i >= 0 && i < siblings.size(); i += step) {
            Widget s = siblings.get(i);
            if (s != this && s.isVisible()) return s;
        }
        return null;
    }

    // ---------- Отрисовка / Rendering ----------

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        pointerX = mouseX;
        pointerY = mouseY;
        if (!enabled) return;

        if (showAllBounds) drawAllBounds(g, root(), 0);
        Widget sel = selection();
        if (sel != null) drawSelection(g, sel);
        drawInfoBox(g, sel);
    }

    private void drawAllBounds(UiGraphics g, Widget w, int depth) {
        if (w == this || !w.isVisible() || w.getOpacity() <= 0f) return;
        int ax = w.absX();
        int ay = w.absY();
        int ww = w.getWidth();
        int wh = w.getHeight();
        if (ww > 0 && wh > 0) outline(g, ax, ay, ww, wh, DEPTH_COLORS[depth % DEPTH_COLORS.length]);

        boolean clip = w.isClipChildren();
        if (clip) g.pushClip(ax, ay, ax + ww, ay + wh);
        for (Widget c : w.getChildren()) drawAllBounds(g, c, depth + 1);
        if (clip) g.popClip();
    }

    private void drawSelection(UiGraphics g, Widget sel) {
        int x = sel.absX();
        int y = sel.absY();
        int w = sel.getWidth();
        int h = sel.getHeight();

        // Рамка родителя, кроме корня экрана / The parent's frame, except for the screen root
        Widget parent = sel.getParent();
        if (parent != null && parent.getParent() != null && parent.getWidth() > 0 && parent.getHeight() > 0) {
            outline(g, parent.absX(), parent.absY(), parent.getWidth(), parent.getHeight(), PARENT_BORDER);
        }

        // Отступы вокруг границ / The margins around the bounds
        Insets m = sel.getMargin();
        if (m.top() > 0) g.fill(x - m.left(), y - m.top(), x + w + m.right(), y, MARGIN_FILL);
        if (m.bottom() > 0) g.fill(x - m.left(), y + h, x + w + m.right(), y + h + m.bottom(), MARGIN_FILL);
        if (m.left() > 0) g.fill(x - m.left(), y, x, y + h, MARGIN_FILL);
        if (m.right() > 0) g.fill(x + w, y, x + w + m.right(), y + h, MARGIN_FILL);

        if (w > 0 && h > 0) {
            g.fill(x, y, x + w, y + h, SELECTION_FILL);
            outline(g, x, y, w, h, SELECTION_BORDER);
        }
    }

    private void drawInfoBox(UiGraphics g, Widget sel) {
        UiPlatform p = UiContext.platform();
        int screenW = getWidth();
        int screenH = getHeight();
        int maxInner = Math.max(80, Math.min(screenW - 2 * BOX_MARGIN, MAX_BOX_WIDTH) - 2 * BOX_PADDING);

        List<Line> raw = new ArrayList<>();
        raw.add(new Line("Inspector " + (locked != null ? "[LOCKED]" : "[hover]")
                + (showAllBounds ? " [all bounds]" : ""), COLOR_HEADER));
        if (sel == null) {
            raw.add(new Line("(nothing under the cursor)", COLOR_DIM));
        } else {
            List<String> info = WidgetInfo.lines(sel);
            for (int i = 0; i < info.size(); i++) {
                raw.add(new Line(info.get(i), i == 0 ? COLOR_NAME : COLOR_TEXT));
            }
        }
        if (!stats.isEmpty()) raw.add(new Line(stats, COLOR_DIM));
        raw.add(new Line("Alt+arrows: parent/child/sibling", COLOR_DIM));
        raw.add(new Line("Alt+L / MMB: lock   Alt+B: bounds", COLOR_DIM));
        raw.add(new Line("Alt+C: copy   Alt+P: dump   F12: off", COLOR_DIM));

        // Перенос длинных строк и ширина блока / Wrapping long lines and the box width
        List<Line> lines = new ArrayList<>();
        int textWidth = 0;
        for (Line line : raw) {
            for (String part : TextWrap.wrap(line.text(), maxInner, s -> p.textWidth(UiText.literal(s)))) {
                lines.add(new Line(part, line.color()));
                textWidth = Math.max(textWidth, p.textWidth(UiText.literal(part)));
            }
        }

        int lineHeight = p.lineHeight() + 1;
        int boxW = textWidth + 2 * BOX_PADDING;
        int boxH = lines.size() * lineHeight + 2 * BOX_PADDING - 1;

        // Первый угол, где блок не закрывает ни курсор, ни выбранный виджет / The first corner where the box covers neither the cursor nor the selected widget
        int[][] corners = {
                {BOX_MARGIN, BOX_MARGIN},
                {screenW - boxW - BOX_MARGIN, BOX_MARGIN},
                {BOX_MARGIN, screenH - boxH - BOX_MARGIN},
                {screenW - boxW - BOX_MARGIN, screenH - boxH - BOX_MARGIN}};
        int bx = corners[0][0];
        int by = corners[0][1];
        for (int[] corner : corners) {
            if (!overlapsFocus(corner[0], corner[1], boxW, boxH, sel)) {
                bx = corner[0];
                by = corner[1];
                break;
            }
        }

        g.fill(bx, by, bx + boxW, by + boxH, BOX_BORDER);
        g.fill(bx + 1, by + 1, bx + boxW - 1, by + boxH - 1, BOX_BACKGROUND);
        int y = by + BOX_PADDING;
        for (Line line : lines) {
            g.drawText(UiText.literal(line.text()), bx + BOX_PADDING, y, line.color(), true);
            y += lineHeight;
        }
    }

    private boolean overlapsFocus(int x, int y, int w, int h, Widget sel) {
        if (pointerX >= x - 8 && pointerX < x + w + 8 && pointerY >= y - 8 && pointerY < y + h + 8) return true;
        if (sel == null) return false;
        return x < sel.absX() + sel.getWidth() && x + w > sel.absX()
                && y < sel.absY() + sel.getHeight() && y + h > sel.absY();
    }

    private static void outline(UiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }
}