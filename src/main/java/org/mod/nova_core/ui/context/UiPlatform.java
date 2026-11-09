package org.mod.nova_core.ui.context;

import org.mod.nova_core.ui.graphics.TextWrap;
import org.mod.nova_core.ui.graphics.UiItem;
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

    // Полный текст подсказки предмета, строки через «\n» / The full item tooltip text, lines joined by "\n"
    default UiText itemTooltip(UiItem item) {
        return UiText.literal("");
    }

    // Перенос по ширине. По умолчанию по простому тексту; платформа может сохранять стили
    // Wrap to a width. By default on plain text; a platform may keep the styles
    default List<UiText> wrap(UiText text, int maxWidth) {
        List<UiText> out = new ArrayList<>();
        for (String line : TextWrap.wrap(text.plain(), maxWidth, s -> textWidth(UiText.literal(s)))) {
            out.add(UiText.literal(line));
        }
        return out;
    }

    // Обрезает текст по ширине, добавляя «…» / Cuts the text to a width, adding "…"
    default UiText ellipsize(UiText text, int maxWidth) {
        if (textWidth(text) <= maxWidth) return text;
        String s = text.plain();
        for (int n = s.length() - 1; n > 0; n--) {
            UiText cut = UiText.literal(s.substring(0, n) + "…");
            if (textWidth(cut) <= maxWidth) return cut;
        }
        return UiText.literal("…");
    }

    // Выполнить в основном (клиентском) потоке; по умолчанию сразу / Run on the main (client) thread; right away by default
    default void post(Runnable task) {
        task.run();
    }
}