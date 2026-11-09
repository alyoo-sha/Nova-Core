package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.context.*;
import org.mod.nova_core.ui.graphics.*;
import org.mod.nova_core.ui.input.*;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.style.*;
import org.mod.nova_core.ui.theme.StyleRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Многострочное текстовое поле без переноса: длинные строки прокручиваются по горизонтали. Текст хранится в Signal;
// строки разделены «\n». Курсор, выделение мышью и Shift, буфер обмена, Home/End, PgUp/PgDn, колесо
// A multi-line text field without wrapping: long lines scroll horizontally. The text lives in a Signal;
// lines are separated by "\n". Caret, selection by mouse and Shift, clipboard, Home/End, PgUp/PgDn, wheel
public class TextArea extends Widget {
    private static final int PAD_Y = 3;

    private final StyleRef<FieldStyle> style = new StyleRef<>(Styles.FIELD);
    private final Signal<String> text;

    private UiText placeholder = UiText.literal("");
    private int maxLength = 4096;
    private int caret = 0;
    private int anchor = 0;
    private int scrollLine = 0;
    private int scrollX = 0;
    private boolean dragging = false;
    private long blinkStart = System.currentTimeMillis();

    public TextArea(int x, int y, int width, int height) {
        this(x, y, width, height, Signal.of(""));
    }

    public TextArea(int x, int y, int width, int height, Signal<String> text) {
        super(x, y, width, height);
        this.text = Objects.requireNonNull(text);
        this.caret = text.peek().length();
        this.anchor = caret;
    }

    public String getText() { return text.peek(); }

    public Signal<String> textSignal() { return text; }

    public TextArea setText(String value) {
        String clean = sanitize(value);
        caret = clean.length();
        anchor = caret;
        text.set(clean);
        blink();
        return this;
    }

    public TextArea setPlaceholder(UiText placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    public TextArea setMaxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
        return this;
    }

    public TextArea setStyle(FieldStyle value) {
        style.set(value);
        return this;
    }

    @Override
    public boolean isFocusable() { return true; }

    @Override
    public boolean isTextInput() { return true; }

    // ---------- Текст и позиции / Text and positions ----------

    private static boolean allowed(char c) {
        return c >= 32 && c != 127 && c != '\u00a7';
    }

    private String sanitize(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < maxLength; i++) {
            char c = s.charAt(i);
            if (c == '\n' || allowed(c)) sb.append(c);
        }
        return sb.toString();
    }

    private void blink() { blinkStart = System.currentTimeMillis(); }

    private boolean caretVisible() { return ((System.currentTimeMillis() - blinkStart) / 530L) % 2L == 0L; }

    private void clamp() {
        int len = text.peek().length();
        if (caret > len) caret = len;
        if (anchor > len) anchor = len;
    }

    private int selStart() { return Math.min(caret, anchor); }
    private int selEnd() { return Math.max(caret, anchor); }
    private boolean hasSelection() { return caret != anchor; }

    private static List<String> split(String v) {
        List<String> out = new ArrayList<>();
        int start = 0;
        while (true) {
            int nl = v.indexOf('\n', start);
            if (nl < 0) {
                out.add(v.substring(start));
                return out;
            }
            out.add(v.substring(start, nl));
            start = nl + 1;
        }
    }

    private static int lineOf(String v, int offset) {
        int line = 0;
        for (int i = 0; i < offset && i < v.length(); i++) {
            if (v.charAt(i) == '\n') line++;
        }
        return line;
    }

    private static int lineStart(String v, int offset) {
        return v.lastIndexOf('\n', offset - 1) + 1;
    }

    private static int lineEnd(String v, int offset) {
        int i = v.indexOf('\n', offset);
        return i < 0 ? v.length() : i;
    }

    private void moveCaret(int pos, boolean extend) {
        int len = text.peek().length();
        caret = Math.max(0, Math.min(pos, len));
        if (!extend) anchor = caret;
        blink();
    }

    private void replaceSelection(String insert) {
        String v = text.peek();
        int start = selStart();
        int end = selEnd();
        int room = maxLength - (v.length() - (end - start));
        StringBuilder add = new StringBuilder();
        for (int i = 0; i < insert.length() && add.length() < room; i++) {
            char c = insert.charAt(i);
            if (c == '\n' || allowed(c)) add.append(c);
        }
        if (add.length() == 0 && start == end) return;
        caret = start + add.length();
        anchor = caret;
        text.set(v.substring(0, start) + add + v.substring(end));
        blink();
    }

    private void deleteRange(int from, int to) {
        if (from >= to) return;
        String v = text.peek();
        caret = from;
        anchor = from;
        text.set(v.substring(0, from) + v.substring(to));
        blink();
    }

    // Вертикальное движение с сохранением столбца / Vertical movement keeping the column
    private int verticalTarget(int delta) {
        String v = text.peek();
        int line = lineOf(v, caret);
        List<String> lines = split(v);
        int target = Math.max(0, Math.min(line + delta, lines.size() - 1));
        if (target == line) return delta < 0 ? 0 : v.length();
        int col = caret - lineStart(v, caret);
        int off = 0;
        for (int i = 0; i < target; i++) off += lines.get(i).length() + 1;
        return off + Math.min(col, lines.get(target).length());
    }

    // ---------- Измерения / Measuring ----------

    private int widthOf(String s) { return UiContext.platform().textWidth(UiText.literal(s)); }

    private int lineHeight() { return UiContext.platform().lineHeight() + 1; }

    private int innerLeft() { return absX() + 1 + style.get().paddingX(); }

    private int innerTop() { return absY() + 1 + PAD_Y; }

    private int innerWidth() { return Math.max(0, getWidth() - 2 - 2 * style.get().paddingX()); }

    private int visibleLines() { return Math.max(1, (getHeight() - 2 - 2 * PAD_Y) / lineHeight()); }

    private void ensureCaretVisible() {
        String v = text.peek();
        int line = lineOf(v, caret);
        int visible = visibleLines();
        if (line < scrollLine) scrollLine = line;
        if (line >= scrollLine + visible) scrollLine = line - visible + 1;
        int total = split(v).size();
        scrollLine = Math.max(0, Math.min(scrollLine, Math.max(0, total - visible)));

        int ls = lineStart(v, caret);
        int caretX = widthOf(v.substring(ls, caret));
        int iw = innerWidth();
        if (caretX - scrollX > iw - 1) scrollX = caretX - iw + 1;
        if (caretX - scrollX < 0) scrollX = caretX;
        scrollX = Math.max(0, scrollX);
    }

    // Смещение символа под точкой / The character offset under a point
    private int offsetAt(double mx, double my) {
        String v = text.peek();
        List<String> lines = split(v);
        int row = (int) Math.floor((my - innerTop()) / lineHeight());
        int line = Math.max(0, Math.min(scrollLine + row, lines.size() - 1));
        int off = 0;
        for (int i = 0; i < line; i++) off += lines.get(i).length() + 1;
        String s = lines.get(line);
        double px = mx - innerLeft() + scrollX;
        int col = 0;
        double best = Math.abs(px);
        for (int i = 1; i <= s.length(); i++) {
            double d = Math.abs(px - widthOf(s.substring(0, i)));
            if (d < best) {
                best = d;
                col = i;
            }
        }
        return off + col;
    }

    // ---------- Отрисовка / Rendering ----------

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        FieldStyle s = style.get();
        clamp();
        ensureCaretVisible();

        int ax = absX();
        int ay = absY();
        Draw.box(g, ax, ay, getWidth(), getHeight(), s.radius(), s.background(),
                isFocused() ? s.focusedBorder() : s.border(), 1);

        UiPlatform p = UiContext.platform();
        int lineH = lineHeight();
        int left = innerLeft();
        int top = innerTop();
        String v = text.peek();
        List<String> lines = split(v);
        int caretLine = lineOf(v, caret);

        g.pushClip(left, ay + 1, left + innerWidth(), ay + getHeight() - 1);
        if (v.isEmpty() && !isFocused()) g.drawText(placeholder, left, top, s.placeholder(), false);

        int selS = selStart();
        int selE = selEnd();
        int start = 0;
        for (int li = 0; li < lines.size(); li++) {
            String line = lines.get(li);
            int end = start + line.length();
            int row = li - scrollLine;
            if (row >= 0 && row < visibleLines()) {
                int y = top + row * lineH;
                if (isFocused() && hasSelection() && selE > start && selS <= end) {
                    int a = Math.max(selS, start) - start;
                    int b = Math.min(selE, end) - start;
                    int x1 = left - scrollX + widthOf(line.substring(0, a));
                    int x2 = left - scrollX + widthOf(line.substring(0, b)) + (selE > end ? 4 : 0);
                    g.fill(x1, y - 1, x2, y + p.lineHeight(), s.selection());
                }
                if (!line.isEmpty()) {
                    g.drawText(UiText.literal(line), left - scrollX, y, isActive() ? s.text() : s.disabledText(), s.textShadow());
                }
                if (isFocused() && isActive() && li == caretLine && caretVisible()) {
                    int cx = left - scrollX + widthOf(v.substring(start, caret));
                    g.fill(cx, y - 1, cx + 1, y + p.lineHeight(), s.caret());
                }
            }
            start = end + 1;
        }
        g.popClip();
    }

    // ---------- Мышь / Mouse ----------

    @Override
    protected void onPointerDown(PointerEvent e) {
        if (e.getButton() != MouseButton.LEFT) return;
        clamp();
        moveCaret(offsetAt(e.getX(), e.getY()), e.hasModifier(Keys.MOD_SHIFT));
        dragging = true;
        e.stopPropagation();
    }

    @Override
    protected void onPointerDrag(PointerEvent e) {
        if (!dragging) return;
        moveCaret(offsetAt(e.getX(), e.getY()), true);
        e.stopPropagation();
    }

    @Override
    protected void onPointerUp(PointerEvent e) {
        if (!dragging) return;
        dragging = false;
        e.stopPropagation();
    }

    @Override
    protected void onPointerCancel(PointerEvent e) {
        dragging = false;
    }

    @Override
    protected void onPointerScroll(PointerEvent e) {
        int total = split(text.peek()).size();
        int max = Math.max(0, total - visibleLines());
        int next = Math.max(0, Math.min(scrollLine + (int) Math.round(-e.getScrollDelta() * 3), max));
        if (next == scrollLine) return;
        scrollLine = next;
        e.stopPropagation();
    }

    // ---------- Клавиатура / Keyboard ----------

    @Override
    protected void onKeyDown(KeyboardEvent e) {
        clamp();
        int key = e.getKey();
        boolean shift = e.hasModifier(Keys.MOD_SHIFT);
        boolean cmd = e.hasModifier(Keys.MOD_CONTROL | Keys.MOD_SUPER);
        UiPlatform p = UiContext.platform();
        String v = text.peek();

        if (cmd && key == Keys.A) {
            anchor = 0;
            caret = v.length();
        } else if (cmd && key == Keys.C) {
            if (hasSelection()) p.setClipboard(v.substring(selStart(), selEnd()));
        } else if (cmd && key == Keys.X) {
            if (hasSelection()) {
                p.setClipboard(v.substring(selStart(), selEnd()));
                deleteRange(selStart(), selEnd());
            }
        } else if (cmd && key == Keys.V) {
            replaceSelection(p.getClipboard().replace("\r", ""));
        } else if (key == Keys.ENTER || key == Keys.KP_ENTER) {
            replaceSelection("\n");
        } else if (key == Keys.LEFT) {
            if (hasSelection() && !shift) moveCaret(selStart(), false);
            else moveCaret(caret - 1, shift);
        } else if (key == Keys.RIGHT) {
            if (hasSelection() && !shift) moveCaret(selEnd(), false);
            else moveCaret(caret + 1, shift);
        } else if (key == Keys.UP) {
            moveCaret(verticalTarget(-1), shift);
        } else if (key == Keys.DOWN) {
            moveCaret(verticalTarget(1), shift);
        } else if (key == Keys.PAGE_UP) {
            moveCaret(verticalTarget(-visibleLines()), shift);
        } else if (key == Keys.PAGE_DOWN) {
            moveCaret(verticalTarget(visibleLines()), shift);
        } else if (key == Keys.HOME) {
            moveCaret(cmd ? 0 : lineStart(v, caret), shift);
        } else if (key == Keys.END) {
            moveCaret(cmd ? v.length() : lineEnd(v, caret), shift);
        } else if (key == Keys.BACKSPACE) {
            if (hasSelection()) deleteRange(selStart(), selEnd());
            else deleteRange(Math.max(0, caret - 1), caret);
        } else if (key == Keys.DELETE) {
            if (hasSelection()) deleteRange(selStart(), selEnd());
            else deleteRange(caret, Math.min(v.length(), caret + 1));
        } else {
            return; // остальное (Escape, Tab) не наше / the rest (Escape, Tab) is not ours
        }
        e.stopPropagation();
    }

    @Override
    protected void onChar(KeyboardEvent e) {
        char c = e.getChar();
        if (!allowed(c)) return;
        clamp();
        replaceSelection(String.valueOf(c));
        e.stopPropagation();
    }

    @Override
    public void onFocusChanged(boolean focused) {
        blink();
        if (!focused) dragging = false;
    }
}