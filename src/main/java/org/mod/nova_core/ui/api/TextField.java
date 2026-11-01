package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.KeyboardEvent;
import org.mod.nova_core.ui.input.Keys;
import org.mod.nova_core.ui.input.MouseButton;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.style.FieldStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

// Однострочное текстовое поле. Текст хранится в Signal: можно передать свой, чтобы делить его с другими виджетами
// A single-line text field. The text lives in a Signal: pass your own to share it with other widgets
public class TextField extends Widget {
    private final StyleRef<FieldStyle> style = new StyleRef<>(Styles.FIELD);
    private final Signal<String> text;

    private UiText placeholder = UiText.literal("");
    private int maxLength = 256;
    private IntPredicate filter = c -> true;
    private Consumer<String> onSubmit = s -> { };

    // Позиция каретки и якорь выделения: выделено [min, max) / Caret position and selection anchor: [min, max) is selected
    private int caret = 0;
    private int anchor = 0;
    // Сдвиг текста влево в пикселях / Leftward text shift in pixels
    private int scrollX = 0;
    private boolean dragging = false;
    private long blinkStart = System.currentTimeMillis();

    public TextField(int x, int y, int width, int height) {
        this(x, y, width, height, Signal.of(""));
    }

    public TextField(int x, int y, int width, int height, Signal<String> text) {
        super(x, y, width, height);
        this.text = Objects.requireNonNull(text);
        this.caret = text.peek().length();
        this.anchor = this.caret;
    }

    // ---------- Настройки / Settings ----------

    public String getText() { return text.peek(); }

    public Signal<String> textSignal() { return text; }

    // Каретка уходит в конец; недопустимые символы и лишняя длина отбрасываются / The caret goes to the end; invalid characters and excess length are dropped
    public TextField setText(String value) {
        String clean = sanitize(value);
        caret = clean.length();
        anchor = caret;
        text.set(clean);
        resetBlink();
        return this;
    }

    public TextField setPlaceholder(UiText placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public TextField setMaxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
        if (text.peek().length() > this.maxLength) setText(text.peek());
        return this;
    }

    // Какие символы можно вводить, например Character::isDigit / Which characters can be typed, e.g. Character::isDigit
    public TextField setFilter(IntPredicate filter) {
        this.filter = Objects.requireNonNull(filter);
        return this;
    }

    // Вызывается по Enter / Called on Enter
    public TextField setOnSubmit(Consumer<String> onSubmit) {
        this.onSubmit = Objects.requireNonNull(onSubmit);
        return this;
    }

    public FieldStyle getStyle() { return style.get(); }

    public TextField setStyle(FieldStyle value) {
        style.set(value);
        requestLayout();
        return this;
    }

    @Override
    public boolean isFocusable() { return true; }

    // ---------- Текст и выделение / Text and selection ----------

    private boolean allowed(char c) {
        return c >= 32 && c != 127 && c != '\u00a7' && filter.test(c);
    }

    private String sanitize(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < maxLength; i++) {
            char c = s.charAt(i);
            if (allowed(c)) sb.append(c);
        }
        return sb.toString();
    }

    // Текст мог измениться снаружи через сигнал / The text may have changed from outside through the signal
    private void clampCaret() {
        int len = text.peek().length();
        if (caret > len) caret = len;
        if (anchor > len) anchor = len;
    }

    private int selStart() { return Math.min(caret, anchor); }
    private int selEnd() { return Math.max(caret, anchor); }
    private boolean hasSelection() { return caret != anchor; }

    private void resetBlink() { blinkStart = System.currentTimeMillis(); }

    private boolean caretVisible() { return ((System.currentTimeMillis() - blinkStart) / 530L) % 2L == 0L; }

    private void moveCaret(int pos, boolean extend) {
        caret = Math.max(0, Math.min(pos, text.peek().length()));
        if (!extend) anchor = caret;
        resetBlink();
    }

    // Заменяет выделение (или вставляет в позицию каретки) отфильтрованным текстом / Replaces the selection (or inserts at the caret) with filtered text
    private void replaceSelection(String insert) {
        String value = text.peek();
        int start = selStart();
        int end = selEnd();
        int room = maxLength - (value.length() - (end - start));
        StringBuilder add = new StringBuilder();
        for (int i = 0; i < insert.length() && add.length() < room; i++) {
            char c = insert.charAt(i);
            if (allowed(c)) add.append(c);
        }
        if (add.length() == 0 && start == end) return;
        caret = start + add.length();
        anchor = caret;
        text.set(value.substring(0, start) + add + value.substring(end));
        resetBlink();
    }

    private void deleteRange(int from, int to) {
        if (from >= to) return;
        String value = text.peek();
        caret = from;
        anchor = from;
        text.set(value.substring(0, from) + value.substring(to));
        resetBlink();
    }

    private int wordLeft(int from) {
        String v = text.peek();
        int i = from;
        while (i > 0 && v.charAt(i - 1) == ' ') i--;
        while (i > 0 && v.charAt(i - 1) != ' ') i--;
        return i;
    }

    private int wordRight(int from) {
        String v = text.peek();
        int i = from;
        while (i < v.length() && v.charAt(i) == ' ') i++;
        while (i < v.length() && v.charAt(i) != ' ') i++;
        return i;
    }

    // ---------- Измерения / Measuring ----------

    private int widthOf(String s) { return UiContext.platform().textWidth(UiText.literal(s)); }

    private int prefixWidth(int n) { return widthOf(text.peek().substring(0, n)); }

    private int innerLeft() { return absX() + 1 + style.get().paddingX(); }

    private int innerWidth() { return Math.max(0, getWidth() - 2 - 2 * style.get().paddingX()); }

    private void ensureCaretVisible() {
        int iw = innerWidth();
        int caretX = prefixWidth(caret);
        if (caretX - scrollX > iw - 1) scrollX = caretX - iw + 1;
        if (caretX - scrollX < 0) scrollX = caretX;
        int maxScroll = Math.max(0, widthOf(text.peek()) - iw + 1);
        if (scrollX > maxScroll) scrollX = maxScroll;
        if (scrollX < 0) scrollX = 0;
    }

    // Индекс символа, ближайшего к x / The index of the character nearest to x
    private int indexAt(double mx) {
        double px = mx - innerLeft() + scrollX;
        int len = text.peek().length();
        int best = 0;
        double bestDist = Math.abs(px);
        for (int i = 1; i <= len; i++) {
            int w = prefixWidth(i);
            double d = Math.abs(px - w);
            if (d < bestDist) {
                best = i;
                bestDist = d;
            } else if (w > px) {
                break;
            }
        }
        return best;
    }

    @Override
    protected Size measureContent(Constraints c) {
        return new Size(120, style.get().height());
    }

    // ---------- Отрисовка / Rendering ----------

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        FieldStyle s = style.get();
        clampCaret();
        ensureCaretVisible();

        int ax = absX();
        int ay = absY();
        int w = getWidth();
        int h = getHeight();
        boolean focused = isFocused();

        g.fill(ax, ay, ax + w, ay + h, focused ? s.focusedBorder() : s.border());
        g.fill(ax + 1, ay + 1, ax + w - 1, ay + h - 1, s.background());

        UiPlatform p = UiContext.platform();
        int left = innerLeft();
        int textY = ay + (h - p.lineHeight()) / 2;
        int origin = left - scrollX;
        String value = text.peek();

        g.pushClip(left, ay + 1, left + innerWidth(), ay + h - 1);
        if (value.isEmpty()) {
            if (!focused) g.drawText(placeholder, left, textY, s.placeholder(), false);
        } else {
            if (focused && hasSelection()) {
                int x1 = origin + prefixWidth(selStart());
                int x2 = origin + prefixWidth(selEnd());
                g.fill(x1, textY - 1, x2, textY + p.lineHeight(), s.selection());
            }
            g.drawText(UiText.literal(value), origin, textY, isActive() ? s.text() : s.disabledText(), s.textShadow());
        }
        if (focused && isActive() && caretVisible()) {
            int cx = origin + prefixWidth(caret);
            g.fill(cx, textY - 1, cx + 1, textY + p.lineHeight(), s.caret());
        }
        g.popClip();
    }

    // ---------- Мышь / Mouse ----------

    @Override
    protected void onPointerDown(PointerEvent e) {
        if (e.getButton() != MouseButton.LEFT) return;
        clampCaret();
        moveCaret(indexAt(e.getX()), false);
        dragging = true;
        e.stopPropagation();
    }

    @Override
    protected void onPointerDrag(PointerEvent e) {
        if (!dragging) return;
        moveCaret(indexAt(e.getX()), true);
        e.stopPropagation();
    }

    @Override
    protected void onPointerUp(PointerEvent e) {
        if (!dragging) return;
        dragging = false;
        e.stopPropagation();
    }

    // ---------- Клавиатура / Keyboard ----------

    @Override
    protected void onKeyDown(KeyboardEvent e) {
        clampCaret();
        int key = e.getKey();
        boolean shift = e.hasModifier(Keys.MOD_SHIFT);
        // Ctrl на Windows и Linux, Cmd на macOS / Ctrl on Windows and Linux, Cmd on macOS
        boolean cmd = e.hasModifier(Keys.MOD_CONTROL | Keys.MOD_SUPER);
        UiPlatform p = UiContext.platform();
        String value = text.peek();

        if (cmd && key == Keys.A) {
            anchor = 0;
            caret = value.length();
            resetBlink();
        } else if (cmd && key == Keys.C) {
            if (hasSelection()) p.setClipboard(value.substring(selStart(), selEnd()));
        } else if (cmd && key == Keys.X) {
            if (hasSelection()) {
                p.setClipboard(value.substring(selStart(), selEnd()));
                deleteRange(selStart(), selEnd());
            }
        } else if (cmd && key == Keys.V) {
            replaceSelection(p.getClipboard());
        } else if (key == Keys.LEFT) {
            if (hasSelection() && !shift) moveCaret(selStart(), false);
            else moveCaret(cmd ? wordLeft(caret) : caret - 1, shift);
        } else if (key == Keys.RIGHT) {
            if (hasSelection() && !shift) moveCaret(selEnd(), false);
            else moveCaret(cmd ? wordRight(caret) : caret + 1, shift);
        } else if (key == Keys.HOME) {
            moveCaret(0, shift);
        } else if (key == Keys.END) {
            moveCaret(value.length(), shift);
        } else if (key == Keys.BACKSPACE) {
            if (hasSelection()) deleteRange(selStart(), selEnd());
            else deleteRange(cmd ? wordLeft(caret) : Math.max(0, caret - 1), caret);
        } else if (key == Keys.DELETE) {
            if (hasSelection()) deleteRange(selStart(), selEnd());
            else deleteRange(caret, cmd ? wordRight(caret) : Math.min(value.length(), caret + 1));
        } else if (key == Keys.ENTER || key == Keys.KP_ENTER) {
            onSubmit.accept(value);
        } else {
            return; // остальное (Escape, Tab) не наше / the rest (Escape, Tab) is not ours
        }
        e.stopPropagation();
    }

    @Override
    protected void onPointerCancel(PointerEvent e) {
        dragging = false;
    }

    @Override
    protected void onChar(KeyboardEvent e) {
        char c = e.getChar();
        if (!allowed(c)) return;
        clampCaret();
        replaceSelection(String.valueOf(c));
        e.stopPropagation();
    }

    @Override
    public void onFocusChanged(boolean focused) {
        resetBlink();
        if (!focused) dragging = false;
    }
}