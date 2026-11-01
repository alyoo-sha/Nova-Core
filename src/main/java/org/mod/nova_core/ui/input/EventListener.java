package org.mod.nova_core.ui.input;

@FunctionalInterface
public interface EventListener<E extends UiEvent> {
    void handle(E event);
}