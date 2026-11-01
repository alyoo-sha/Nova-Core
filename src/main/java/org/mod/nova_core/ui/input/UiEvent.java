package org.mod.nova_core.ui.input;

// Базовое событие. Одноразовое: после диспетчеризации его повторно отправить нельзя
// Base event. Single-use: it cannot be dispatched again
public abstract class UiEvent {
    private final EventType<?> type;
    private final boolean bubbles;

    private EventTarget target = null;
    private EventTarget currentTarget = null;
    private EventPhase phase = EventPhase.NONE;
    private boolean stopped = false;
    private EventTarget stoppedBy = null;

    protected UiEvent(EventType<?> type, boolean bubbles) {
        this.type = type;
        this.bubbles = bubbles;
    }

    public EventType<?> getType() { return type; }
    public boolean bubbles() { return bubbles; }

    // Узел, которому событие адресовано / The node the event is addressed to
    public EventTarget getTarget() { return target; }

    // Узел, который обрабатывает событие прямо сейчас / The node handling the event right now
    public EventTarget getCurrentTarget() { return currentTarget; }

    public EventPhase getPhase() { return phase; }

    // Остановить дальнейшее прохождение; остальные слушатели того же узла всё равно сработают
    // Stop further propagation; other listeners of the same node still run
    public final void stopPropagation() {
        if (stopped) return;
        stopped = true;
        stoppedBy = currentTarget;
    }

    public boolean isPropagationStopped() { return stopped; }

    // Узел, остановивший событие, или null / The node that stopped the event, or null
    public EventTarget getStoppedBy() { return stoppedBy; }

    // ---------- Для EventDispatcher / For EventDispatcher ----------

    final void begin(EventTarget t) {
        if (target != null) throw new IllegalStateException("Event was already dispatched");
        target = t;
    }

    final void enter(EventTarget node, EventPhase p) {
        currentTarget = node;
        phase = p;
    }

    final void finish() {
        currentTarget = null;
        phase = EventPhase.NONE;
    }
}