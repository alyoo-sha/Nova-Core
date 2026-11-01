package org.mod.nova_core.ui.graphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

// Перенос текста по словам. Не зависит от платформы: ширину считает переданная функция
// Word wrapping. Platform independent: the width is measured by the given function
public final class TextWrap {
    private TextWrap() {}

    // maxWidth <= 0 или Integer.MAX_VALUE = без переноса, только по '\n'. Слишком длинные слова режутся по символам.
    // Повторные пробелы схлопываются / maxWidth <= 0 or Integer.MAX_VALUE = no wrapping, only at '\n'. Words that are too long are cut by characters.
    // Repeated spaces collapse
    public static List<String> wrap(String text, int maxWidth, ToIntFunction<String> width) {
        List<String> out = new ArrayList<>();
        boolean unbounded = maxWidth <= 0 || maxWidth == Integer.MAX_VALUE;

        for (String paragraph : text.split("\n", -1)) {
            if (unbounded) {
                out.add(paragraph);
                continue;
            }
            String line = "";
            for (String raw : paragraph.split(" ", -1)) {
                String word = raw;
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (width.applyAsInt(candidate) <= maxWidth) {
                    line = candidate;
                    continue;
                }
                if (!line.isEmpty()) {
                    out.add(line);
                    line = "";
                }
                // Слово не влезает даже одно: режем / The word does not fit even alone: cut it
                while (word.length() > 1 && width.applyAsInt(word) > maxWidth) {
                    int cut = 1;
                    while (cut < word.length() - 1 && width.applyAsInt(word.substring(0, cut + 1)) <= maxWidth) cut++;
                    out.add(word.substring(0, cut));
                    word = word.substring(cut);
                }
                line = word;
            }
            out.add(line);
        }
        return out;
    }
}