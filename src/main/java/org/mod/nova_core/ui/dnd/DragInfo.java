package org.mod.nova_core.ui.dnd;

import org.mod.nova_core.ui.component.Widget;

import java.util.Objects;

// Что переносим и как это выглядит под курсором. ghost может быть null: тогда ничего не рисуется.
// По умолчанию «призрак» стоит по центру курсора; grabbed(...) сохраняет точку, за которую схватили
// What is carried and how it looks under the cursor. ghost may be null: then nothing is drawn.
// By default the ghost is centered on the cursor; grabbed(...) keeps the point that was grabbed
public record DragInfo(Object data, Widget ghost, int grabX, int grabY, boolean centered) {
    public DragInfo {
        Objects.requireNonNull(data, "data");
    }

    public DragInfo(Object data, Widget ghost) {
        this(data, ghost, 0, 0, true);
    }

    // grabX и grabY: где курсор находится внутри «призрака» / grabX and grabY: where the cursor sits inside the ghost
    public static DragInfo grabbed(Object data, Widget ghost, int grabX, int grabY) {
        return new DragInfo(data, ghost, grabX, grabY, false);
    }
}