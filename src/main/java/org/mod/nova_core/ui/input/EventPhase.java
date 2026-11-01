package org.mod.nova_core.ui.input;

// Фаза прохождения события / Phase of event propagation
public enum EventPhase {
    NONE,    // вне диспетчеризации / outside of dispatch
    CAPTURE, // от корня вниз к родителю цели / from the root down to the target's parent
    TARGET,  // на самой цели / on the target itself
    BUBBLE   // от родителя цели вверх к корню / from the target's parent up to the root
}