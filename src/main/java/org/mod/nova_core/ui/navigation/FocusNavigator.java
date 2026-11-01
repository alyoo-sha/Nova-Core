package org.mod.nova_core.ui.navigation;

import org.mod.nova_core.ui.api.ScrollView;
import org.mod.nova_core.ui.component.Widget;

import java.util.ArrayList;
import java.util.List;

// Поиск следующего виджета для фокуса; работает внутри заданной области (scope) / Finds the next widget to focus; works inside a given scope
public final class FocusNavigator {
    private FocusNavigator() {}

    // Виджеты, которые сейчас могут получить фокус, в порядке дерева / Widgets that can take focus right now, in tree order
    public static List<Widget> focusables(Widget scope) {
        List<Widget> out = new ArrayList<>();
        collect(scope, out);
        return out;
    }

    private static void collect(Widget w, List<Widget> out) {
        if (!w.isVisible() || !w.isActive() || w.getOpacity() <= 0f) return;
        if (w.isFocusable()) out.add(w);
        for (Widget c : w.getChildren()) collect(c, out);
    }

    public static Widget first(Widget scope) {
        List<Widget> list = focusables(scope);
        return list.isEmpty() ? null : list.get(0);
    }

    public static Widget last(Widget scope) {
        List<Widget> list = focusables(scope);
        return list.isEmpty() ? null : list.get(list.size() - 1);
    }

    // Tab (forward) или Shift+Tab; по кругу. Если current вне области или null: первый или последний
    // Tab (forward) or Shift+Tab; wraps around. If current is outside the scope or null: the first or the last
    public static Widget next(Widget scope, Widget current, boolean forward) {
        List<Widget> list = focusables(scope);
        int n = list.size();
        if (n == 0) return null;
        int idx = current == null ? -1 : list.indexOf(current);
        if (idx < 0) return forward ? list.get(0) : list.get(n - 1);
        return list.get(((idx + (forward ? 1 : -1)) % n + n) % n);
    }

    // Ближайший виджет в направлении; null, если дальше идти некуда / The nearest widget in a direction; null if there is nowhere to go
    public static Widget find(Widget scope, Widget current, Direction dir) {
        List<Widget> list = focusables(scope);
        if (list.isEmpty()) return null;
        int idx = current == null ? -1 : list.indexOf(current);
        if (idx < 0) {
            boolean forward = dir == Direction.DOWN || dir == Direction.RIGHT;
            return forward ? list.get(0) : list.get(list.size() - 1);
        }
        // Сначала строго «в конусе» 90° от центра, потом любой в нужной полуплоскости
        // First strictly inside a 90° cone from the center, then anything in the right half-plane
        Widget best = pick(list, current, dir, true);
        return best != null ? best : pick(list, current, dir, false);
    }

    private static Widget pick(List<Widget> list, Widget current, Direction dir, boolean strictCone) {
        double cx = current.absX() + current.getWidth() / 2.0;
        double cy = current.absY() + current.getHeight() / 2.0;
        boolean horizontal = dir == Direction.LEFT || dir == Direction.RIGHT;
        double sign = (dir == Direction.RIGHT || dir == Direction.DOWN) ? 1.0 : -1.0;

        Widget best = null;
        double bestScore = Double.MAX_VALUE;
        for (Widget w : list) {
            if (w == current) continue;
            double dx = w.absX() + w.getWidth() / 2.0 - cx;
            double dy = w.absY() + w.getHeight() / 2.0 - cy;
            double along = sign * (horizontal ? dx : dy);
            double across = Math.abs(horizontal ? dy : dx);
            if (along <= 0.0) continue;
            if (strictCone && across > along) continue;
            // Отклонение в сторону штрафуется вдвое сильнее / Sideways deviation is penalized twice as hard
            double score = along + 2.0 * across;
            if (score < bestScore) {
                bestScore = score;
                best = w;
            }
        }
        return best;
    }

    // Ставит фокус и прокручивает ScrollView-предков, чтобы виджет был виден / Sets focus and scrolls ScrollView ancestors so the widget is visible
    public static boolean focus(Widget target) {
        if (target == null) return false;
        target.requestFocus();
        reveal(target);
        return target.isFocused();
    }

    public static void reveal(Widget w) {
        for (Widget p = w.getParent(); p != null; p = p.getParent()) {
            if (p instanceof ScrollView view) {
                int top = w.absY() - view.getContent().absY();
                view.scrollToShow(top, top + w.getHeight());
            }
        }
    }
}