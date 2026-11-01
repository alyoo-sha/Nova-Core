package org.mod.nova_core.ui.dnd;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.style.FieldStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.Objects;
import java.util.function.Predicate;

// Зона-цель переноса, которая может содержать детей. Пока идёт подходящий перенос, обведена рамкой,
// а под курсором ещё и залита
// A drop target zone that can hold children. While a suitable drag is on it is outlined,
// and under the cursor it is filled as well
public class DropZone extends Widget {
    @FunctionalInterface
    public interface DropHandler {
        void drop(DragSession session, double x, double y);
    }

    private final StyleRef<FieldStyle> style = new StyleRef<>(Styles.FIELD);

    private Predicate<DragSession> accepts = s -> true;
    private DropHandler onDrop = (s, x, y) -> { };
    private boolean frame = false;

    public DropZone(int x, int y, int width, int height) {
        super(x, y, width, height);
        Dnd.target(this, new DropTarget() {
            @Override
            public boolean canAccept(DragSession session) {
                return accepts.test(session);
            }

            @Override
            public void onDrop(DragSession session, double px, double py) {
                onDrop.drop(session, px, py);
            }
        });
    }

    // Какие данные принимает зона / Which data the zone accepts
    public DropZone setAccepts(Predicate<DragSession> accepts) {
        this.accepts = Objects.requireNonNull(accepts);
        return this;
    }

    public DropZone setOnDrop(DropHandler onDrop) {
        this.onDrop = Objects.requireNonNull(onDrop);
        return this;
    }

    // Рисовать фон и рамку зоны / Draw the zone's background and border
    public DropZone setFrame(boolean frame) {
        this.frame = frame;
        return this;
    }

    public FieldStyle getStyle() { return style.get(); }

    public DropZone setStyle(FieldStyle value) {
        style.set(value);
        return this;
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!frame) return;
        FieldStyle s = style.get();
        int ax = absX();
        int ay = absY();
        g.fill(ax, ay, ax + getWidth(), ay + getHeight(), s.border());
        g.fill(ax + 1, ay + 1, ax + getWidth() - 1, ay + getHeight() - 1, s.background());
    }

    // Подсветка поверх детей / The highlight above the children
    @Override
    protected void renderOverlay(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        DragSession session = DragLayer.of(this).session();
        if (session == null || !accepts.test(session)) return;

        FieldStyle s = style.get();
        boolean over = session.hoveredTarget() == this;
        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();

        if (over) g.fill(ax, ay, ax + w, ay + h, s.selection());

        int c = over ? s.focusedBorder() : s.selection();
        int t = 2;
        g.fill(ax, ay, ax + w, ay + t, c);
        g.fill(ax, ay + h - t, ax + w, ay + h, c);
        g.fill(ax, ay + t, ax + t, ay + h - t, c);
        g.fill(ax + w - t, ay + t, ax + w, ay + h - t, c);
    }
}