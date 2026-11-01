package org.mod.nova_core.ui.input;

// Узел, которому можно доставлять события; Widget реализует его
// A node that events can be delivered to; Widget implements it
public interface EventTarget {
    // Родитель в цепочке событий, null у корня / Parent in the event path, null for the root
    EventTarget getEventParent();

    // Вызывается диспетчером на каждом узле пути, фаза берётся из события
    // Called by the dispatcher on every node of the path, the phase is read from the event
    void handleEvent(UiEvent event);
}