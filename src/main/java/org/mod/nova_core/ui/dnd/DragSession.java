package org.mod.nova_core.ui.dnd;

import org.mod.nova_core.ui.component.Widget;

// Текущий перенос / The current drag
public final class DragSession {
    private final Object data;
    private final Widget source;
    private final double startX;
    private final double startY;

    private double x;
    private double y;
    private Widget hoveredTarget = null;

    DragSession(Object data, Widget source, double startX, double startY) {
        this.data = data;
        this.source = source;
        this.startX = startX;
        this.startY = startY;
        this.x = startX;
        this.y = startY;
    }

    public Object data() { return data; }

    // Данные нужного типа или null / The data of the given type, or null
    public <T> T dataAs(Class<T> type) {
        return type.isInstance(data) ? type.cast(data) : null;
    }

    // Виджет, на котором зарегистрирован источник / The widget the source is registered on
    public Widget source() { return source; }

    public double startX() { return startX; }
    public double startY() { return startY; }
    public double x() { return x; }
    public double y() { return y; }

    // Виджет-цель под курсором, готовая принять данные, или null / The target widget under the cursor that is ready to accept, or null
    public Widget hoveredTarget() { return hoveredTarget; }

    void setPointer(double x, double y) {
        this.x = x;
        this.y = y;
    }

    void setHoveredTarget(Widget widget) {
        this.hoveredTarget = widget;
    }
}