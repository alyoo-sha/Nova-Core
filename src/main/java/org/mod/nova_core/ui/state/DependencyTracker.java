package org.mod.nova_core.ui.state;

import java.util.ArrayList;

// Запоминает, какие сигналы прочитал владелец (Computed или Effect), и версии, которые он видел
// Remembers which signals the owner (Computed or Effect) read, and the versions it saw
final class DependencyTracker {
    private static final class Entry {
        final AbstractSignal<?> source;
        final int version;

        Entry(AbstractSignal<?> source, int version) {
            this.source = source;
            this.version = version;
        }
    }

    private final Runnable onStale;
    private final ArrayList<Entry> entries = new ArrayList<>();

    DependencyTracker(Runnable onStale) {
        this.onStale = onStale;
    }

    // Источник возможно изменился / A source may have changed
    void markStale() {
        onStale.run();
    }

    // Начало прогона: сбрасывает старые зависимости и делает этот узел текущим
    // Start of a run: drops old dependencies and makes this node the current one
    DependencyTracker begin() {
        clear();
        return Reactive.enter(this);
    }

    void end(DependencyTracker previous) {
        Reactive.exit(previous);
    }

    void recordRead(AbstractSignal<?> source) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).source == source) return;
        }
        entries.add(new Entry(source, source.version));
        source.addDependent(this);
    }

    // Изменилась ли хоть одна зависимость; порядок чтения сохраняется, поэтому условные ветки безопасны
    // Whether any dependency changed; read order is kept, so conditional branches are safe
    boolean dependenciesChanged() {
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            e.source.refresh();
            if (e.source.version != e.version) return true;
        }
        return false;
    }

    void clear() {
        for (int i = 0; i < entries.size(); i++) entries.get(i).source.removeDependent(this);
        entries.clear();
    }
}