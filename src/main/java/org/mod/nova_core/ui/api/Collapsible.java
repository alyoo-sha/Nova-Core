package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.layout.Constraints;

// Сворачиваемый блок: высота плавно меняется между нулём и высотой содержимого / A collapsible block: the height glides between zero and the content height
public class Collapsible extends AnimatedBox {
    private final Widget content;
    private boolean expanded;

    public Collapsible(int width, Widget content, boolean expanded) {
        super(0, 0, width, 0);
        this.content = content;
        this.expanded = expanded;
        setClipChildren(true);
        add(content);
        jumpTo(width, expanded ? contentHeight() : 0);
    }

    private int contentHeight() {
        return content.measure(new Constraints(getWidth(), getWidth(), 0, Constraints.UNBOUNDED)).height();
    }

    public boolean isExpanded() { return expanded; }

    public void setExpanded(boolean value) {
        this.expanded = value;
        animateTo(getWidth(), value ? contentHeight() : 0);
    }

    public void toggle() { setExpanded(!expanded); }
}