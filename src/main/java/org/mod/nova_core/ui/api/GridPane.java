package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Insets;
import org.mod.nova_core.ui.layout.Size;

import java.util.ArrayList;
import java.util.List;

// Сетка с заданным числом колонок: дети идут слева направо и вниз. Ширина колонки по самому широкому ребёнку,
// высота ряда по самому высокому. При equalColumns все колонки одинаковой ширины и дети растягиваются на неё
// A grid with a given column count: children go left to right and downward. A column's width follows its widest child,
// a row's height its tallest. With equalColumns all columns are equally wide and children stretch to them
public class GridPane extends Widget {
    private final int columns;
    private int hGap = 4;
    private int vGap = 4;
    private Insets padding = Insets.ZERO;
    private boolean equalColumns = false;

    // Размеры дорожек после последнего измерения / Track sizes after the last measure
    private int[] colWidths = new int[0];
    private int[] rowHeights = new int[0];

    public GridPane(int columns) {
        super(0, 0, 0, 0);
        if (columns < 1) throw new IllegalArgumentException("A grid needs at least one column");
        this.columns = columns;
    }

    public GridPane setGaps(int horizontal, int vertical) {
        this.hGap = Math.max(0, horizontal);
        this.vGap = Math.max(0, vertical);
        requestLayout();
        return this;
    }

    public GridPane setPadding(Insets padding) {
        this.padding = padding;
        requestLayout();
        return this;
    }

    public GridPane setEqualColumns(boolean equalColumns) {
        this.equalColumns = equalColumns;
        requestLayout();
        return this;
    }

    private List<Widget> visibleChildren() {
        List<Widget> items = new ArrayList<>();
        for (Widget c : getChildren()) if (c.isVisible()) items.add(c);
        return items;
    }

    @Override
    protected Size measureContent(Constraints c) {
        List<Widget> items = visibleChildren();
        int n = items.size();
        int rows = (n + columns - 1) / columns;
        int padH = padding.left() + padding.right();
        int padV = padding.top() + padding.bottom();
        int availW = Constraints.shrink(c.maxWidth(), padH);
        boolean equalFit = equalColumns && availW != Constraints.UNBOUNDED;
        int equalWidth = equalFit ? Math.max(0, (availW - hGap * (columns - 1)) / columns) : 0;

        int[] widths = new int[columns];
        int[] heights = new int[rows];
        for (int i = 0; i < n; i++) {
            Widget w = items.get(i);
            Size s = equalFit
                    ? w.measure(new Constraints(equalWidth, equalWidth, 0, Constraints.UNBOUNDED))
                    : w.measure(Constraints.loose(availW, Constraints.UNBOUNDED));
            widths[i % columns] = Math.max(widths[i % columns], s.width());
            heights[i / columns] = Math.max(heights[i / columns], s.height());
        }
        colWidths = widths;
        rowHeights = heights;

        int totalW = padH + hGap * (columns - 1);
        for (int cw : widths) totalW += cw;
        int totalH = padV + vGap * Math.max(0, rows - 1);
        for (int rh : heights) totalH += rh;
        return new Size(totalW, totalH);
    }

    @Override
    protected void layoutChildren() {
        List<Widget> items = visibleChildren();
        int[] colX = new int[columns];
        int x = padding.left();
        for (int col = 0; col < columns; col++) {
            colX[col] = x;
            x += colWidths[col] + hGap;
        }
        int y = padding.top();
        for (int row = 0; row < rowHeights.length; row++) {
            for (int col = 0; col < columns; col++) {
                int i = row * columns + col;
                if (i >= items.size()) break;
                items.get(i).setPosition(colX[col], y);
            }
            y += rowHeights[row] + vGap;
        }
        super.layoutChildren();
    }
}