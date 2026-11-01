package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.layout.Alignment;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.Justification;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.layout.SizeMode;

import java.util.ArrayList;
import java.util.List;

public abstract class LinearStack extends Widget {
    // true = столбец (вертикально), false = строка / true = column (vertical), false = row
    private final boolean vertical;
    private Insets padding = Insets.ZERO;
    private int gap = 0;
    private Alignment alignment = Alignment.START;
    private Justification justification = Justification.START;

    protected LinearStack(int x, int y, int width, int height, boolean vertical) {
        super(x, y, width, height);
        this.vertical = vertical;
    }

    public LinearStack setPadding(Insets padding) {
        this.padding = padding;
        requestLayout();
        return this;
    }

    public LinearStack setGap(int gap) {
        this.gap = gap;
        requestLayout();
        return this;
    }

    public LinearStack setAlignment(Alignment alignment) {
        this.alignment = alignment;
        requestLayout();
        return this;
    }

    public LinearStack setJustification(Justification justification) {
        this.justification = justification;
        requestLayout();
        return this;
    }

    // ---------- Вспомогательные / Helpers ----------
    // main = главная ось (вдоль стека), cross = поперечная / main = axis along the stack, cross = perpendicular

    private int main(Widget w) { return vertical ? w.getHeight() : w.getWidth(); }
    private int cross(Widget w) { return vertical ? w.getWidth() : w.getHeight(); }

    private int mainMargin(Widget w) {
        Insets m = w.getMargin();
        return vertical ? m.top() + m.bottom() : m.left() + m.right();
    }

    private int crossMargin(Widget w) {
        Insets m = w.getMargin();
        return vertical ? m.left() + m.right() : m.top() + m.bottom();
    }

    private int padMain() { return vertical ? padding.top() + padding.bottom() : padding.left() + padding.right(); }
    private int padCross() { return vertical ? padding.left() + padding.right() : padding.top() + padding.bottom(); }

    private SizeMode mainMode() { return vertical ? getHeightMode() : getWidthMode(); }

    private Constraints make(int mainMin, int mainMax, int crossMin, int crossMax) {
        return vertical
                ? new Constraints(crossMin, crossMax, mainMin, mainMax)
                : new Constraints(mainMin, mainMax, crossMin, crossMax);
    }

    private List<Widget> visibleChildren() {
        List<Widget> items = new ArrayList<>();
        for (Widget c : getChildren()) if (c.isVisible()) items.add(c);
        return items;
    }

    // ---------- Проход 1: измерение / Pass 1: measure ----------

    @Override
    protected Size measureContent(Constraints c) {
        List<Widget> items = visibleChildren();
        int padMain = padMain();
        int padCross = padCross();
        int availMain = Constraints.shrink(vertical ? c.maxHeight() : c.maxWidth(), padMain);
        int availCross = Constraints.shrink(vertical ? c.maxWidth() : c.maxHeight(), padCross);
        boolean crossBounded = availCross != Constraints.UNBOUNDED;
        boolean stretch = alignment == Alignment.STRETCH && crossBounded;
        // Вес работает, только если размер по главной оси известен / Weights work only when the main-axis size is known
        boolean flexible = availMain != Constraints.UNBOUNDED && mainMode() != SizeMode.WRAP;

        int used = gap * Math.max(0, items.size() - 1);
        float totalWeight = 0f;

        // Сначала дети без веса / Children without weight first
        for (Widget w : items) {
            if (flexible && w.getWeight() > 0f) {
                totalWeight += w.getWeight();
                used += mainMargin(w);
                continue;
            }
            int crossAvail = Constraints.shrink(availCross, crossMargin(w));
            int crossMin = stretch ? crossAvail : 0;
            w.measure(make(0, Constraints.shrink(availMain, mainMargin(w)), crossMin, crossAvail));
            used += main(w) + mainMargin(w);
        }

        // Затем делим остаток между детьми с весом / Then split the remainder among weighted children
        if (totalWeight > 0f) {
            int remaining = Math.max(0, availMain - used);
            int distributed = 0;
            float acc = 0f;
            for (Widget w : items) {
                if (w.getWeight() <= 0f) continue;
                acc += w.getWeight();
                // Накопительное округление: пиксели не теряются / Cumulative rounding: no pixels get lost
                int target = Math.round(remaining * acc / totalWeight);
                int share = target - distributed;
                distributed = target;
                int crossAvail = Constraints.shrink(availCross, crossMargin(w));
                int crossMin = stretch ? crossAvail : 0;
                w.measure(make(share, share, crossMin, crossAvail));
            }
            used += distributed;
        }

        int maxCross = 0;
        for (Widget w : items) maxCross = Math.max(maxCross, cross(w) + crossMargin(w));

        int mainContent = padMain + used;
        int crossContent = padCross + maxCross;
        return vertical ? new Size(crossContent, mainContent) : new Size(mainContent, crossContent);
    }

    // ---------- Проход 2: расстановка / Pass 2: position ----------

    @Override
    protected void layoutChildren() {
        List<Widget> items = visibleChildren();
        int n = items.size();
        if (n > 0) {
            int innerMain = (vertical ? getHeight() : getWidth()) - padMain();
            int innerCross = (vertical ? getWidth() : getHeight()) - padCross();
            int startMain = vertical ? padding.top() : padding.left();
            int startCross = vertical ? padding.left() : padding.top();

            int total = gap * (n - 1);
            for (Widget w : items) total += main(w) + mainMargin(w);
            int free = Math.max(0, innerMain - total);

            int pos = 0;
            int between = gap;
            switch (justification) {
                case CENTER -> pos = free / 2;
                case END -> pos = free;
                case SPACE_BETWEEN -> { if (n > 1) between = gap + free / (n - 1); }
                default -> { }
            }

            for (Widget w : items) {
                Insets m = w.getMargin();
                int marginBeforeMain = vertical ? m.top() : m.left();
                int marginBeforeCross = vertical ? m.left() : m.top();
                int crossSpace = innerCross - crossMargin(w);
                int crossPos = switch (alignment) {
                    case CENTER -> (crossSpace - cross(w)) / 2;
                    case END -> crossSpace - cross(w);
                    default -> 0;
                };

                int mainCoord = startMain + pos + marginBeforeMain;
                int crossCoord = startCross + marginBeforeCross + crossPos;
                if (vertical) w.setPosition(crossCoord, mainCoord);
                else w.setPosition(mainCoord, crossCoord);

                pos += main(w) + mainMargin(w) + between;
            }
        }
        super.layoutChildren(); // раскладка вложенных контейнеров / lay out nested containers
    }
}