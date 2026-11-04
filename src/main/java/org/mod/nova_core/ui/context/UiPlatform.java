package org.mod.nova_core.ui.context;

import org.mod.nova_core.ui.graphics.TextWrap;
import org.mod.nova_core.ui.graphics.UiText;

import java.util.ArrayList;
import java.util.List;

// Что ядру нужно от платформы / What the core needs from the platform
public interface UiPlatform {
    int textWidth(UiText text);

    int lineHeight();

    void playClickSound();

    // Буфер обмена; пустая строка, если он пуст / The clipboard; an empty string if it is empty
    String getClipboard();

    void setClipboard(String text);

    // Перенос по ширине. По умолчанию по простому тексту; платформа может сохранять стили
    // Wrap to a width. By default on plain text; a platform may keep the styles
    default List<UiText> wrap(UiText text, int maxWidth) {
        List<UiText> out = new ArrayList<>();
        for (String line : TextWrap.wrap(text.plain(), maxWidth, s -> textWidth(UiText.literal(s)))) {
            out.add(UiText.literal(line));
        }
        return out;
    }

    // Выполнить в основном (клиентском) потоке; по умолчанию сразу / Run on the main (client) thread; right away by default
    default void post(Runnable task) {
        task.run();
    }
}