package org.mod.nova_core.ui.context;

import org.mod.nova_core.ui.graphics.UiText;

// Что ядру нужно от платформы / What the core needs from the platform
public interface UiPlatform {
    int textWidth(UiText text);

    int lineHeight();

    void playClickSound();

    // Буфер обмена; пустая строка, если он пуст / The clipboard; an empty string if it is empty
    String getClipboard();

    void setClipboard(String text);
}