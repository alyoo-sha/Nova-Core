package org.mod.nova_core.ui.input;

import java.util.ArrayList;
import java.util.List;

// Проводит событие через фазы capture → target → bubble / Runs an event through capture → target → bubble
public final class EventDispatcher {
    private EventDispatcher() {}

    public static void dispatch(EventTarget target, UiEvent event) {
        // Предки от ближайшего к корню / Ancestors from the nearest to the root
        List<EventTarget> ancestors = new ArrayList<>();
        for (EventTarget t = target.getEventParent(); t != null; t = t.getEventParent()) ancestors.add(t);

        event.begin(target);
        try {
            for (int i = ancestors.size() - 1; i >= 0 && !event.isPropagationStopped(); i--) {
                EventTarget node = ancestors.get(i);
                event.enter(node, EventPhase.CAPTURE);
                node.handleEvent(event);
            }
            if (!event.isPropagationStopped()) {
                event.enter(target, EventPhase.TARGET);
                target.handleEvent(event);
            }
            if (event.bubbles()) {
                for (int i = 0; i < ancestors.size() && !event.isPropagationStopped(); i++) {
                    EventTarget node = ancestors.get(i);
                    event.enter(node, EventPhase.BUBBLE);
                    node.handleEvent(event);
                }
            }
        } finally {
            event.finish();
        }
    }
}