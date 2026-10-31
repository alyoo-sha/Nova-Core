package org.mod.nova_core.ui.context;

import org.mod.nova_core.ui.graphics.UiText;

// Что ядру нужно от платформы / What the core needs from the platform
public interface UiPlatform {
    int textWidth(UiText text);

    int lineHeight();

    void playClickSound();
}