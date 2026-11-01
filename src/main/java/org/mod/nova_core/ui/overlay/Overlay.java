package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.component.Widget;

import java.util.ArrayList;
import java.util.List;

// Ручка открытого оверлея / A handle to an open overlay
public final class Overlay {
    private final OverlayEntry entry;
    private final List<Runnable> closeListeners = new ArrayList<>();
    private boolean closed = false;

    Overlay(OverlayEntry entry) {
        this.entry = entry;
    }

    public Widget content() { return entry.content(); }

    // false, как только закрытие началось (даже если ещё идёт затухание) / false as soon as closing has begun (even during the fade)
    public boolean isOpen() { return !entry.isClosing(); }

    public void close() { entry.requestClose(); }

    // Вызывается, когда оверлей полностью убран; если уже убран, то сразу
    // Called when the overlay is fully removed; right away if it already is
    public Overlay onClose(Runnable listener) {
        if (closed) listener.run();
        else closeListeners.add(listener);
        return this;
    }

    void fireClosed() {
        closed = true;
        RuntimeException first = null;
        for (int i = 0; i < closeListeners.size(); i++) {
            try {
                closeListeners.get(i).run();
            } catch (RuntimeException ex) {
                if (first == null) first = ex;
            }
        }
        closeListeners.clear();
        if (first != null) throw first;
    }
}