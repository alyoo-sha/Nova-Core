package org.mod.nova_core.ui.dnd;

import org.mod.nova_core.ui.component.Widget;

import java.util.Objects;

// Что переносим и как это выглядит под курсором. ghost может быть null: тогда ничего не рисуется
// What is carried and how it looks under the cursor. ghost may be null: then nothing is drawn
public record DragInfo(Object data, Widget ghost) {
    public DragInfo {
        Objects.requireNonNull(data, "data");
    }
}