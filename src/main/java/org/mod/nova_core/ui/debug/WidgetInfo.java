package org.mod.nova_core.ui.debug;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.layout.Insets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// Текстовое описание виджета и дерева / A text description of a widget and of a tree
public final class WidgetInfo {
    private static final int MAX_PATH_PARTS = 6;
    private static final int MAX_DUMP_DEPTH = 64;
    private static final int MAX_TEXT = 40;

    private WidgetInfo() {}

    // Имя класса; у анонимного класса имя родителя со звёздочкой / The class name; for an anonymous class the parent's name with an asterisk
    public static String typeName(Widget w) {
        Class<?> c = w.getClass();
        if (c.isAnonymousClass()) return c.getSuperclass().getSimpleName() + "*";
        String simple = c.getSimpleName();
        if (!simple.isEmpty()) return simple;
        String full = c.getName();
        return full.substring(full.lastIndexOf('.') + 1);
    }

    // Имя (если задано) и класс / The name (if set) and the class
    public static String label(Widget w) {
        String name = w.getDebugName();
        return name == null ? typeName(w) : name + " (" + typeName(w) + ")";
    }

    private static String shortLabel(Widget w) {
        String name = w.getDebugName();
        return name != null ? name : typeName(w);
    }

    // Сокращает текст для строки описания и убирает переводы строк / Shortens text for a description line and removes line breaks
    public static String shorten(String text) {
        String t = text.replace('\n', ' ');
        return t.length() > MAX_TEXT ? t.substring(0, MAX_TEXT) + "..." : t;
    }

    // Путь от корня до виджета / The path from the root to the widget
    public static String path(Widget w) {
        List<String> parts = new ArrayList<>();
        for (Widget p = w; p != null; p = p.getParent()) parts.add(shortLabel(p));
        Collections.reverse(parts);
        if (parts.size() <= MAX_PATH_PARTS) return String.join(" > ", parts);
        return "... > " + String.join(" > ", parts.subList(parts.size() - MAX_PATH_PARTS, parts.size()));
    }

    // Строки описания одного виджета / The description lines of one widget
    public static List<String> lines(Widget w) {
        List<String> out = new ArrayList<>();
        out.add(label(w));
        out.add("pos " + w.getX() + "," + w.getY() + "  abs " + w.absX() + "," + w.absY()
                + "  size " + w.getWidth() + "x" + w.getHeight());
        out.add("mode W:" + w.getWidthMode() + " H:" + w.getHeightMode()
                + "  weight " + fmt(w.getWeight()) + "  opacity " + fmt(w.getOpacity()));
        Insets m = w.getMargin();
        if (!Insets.ZERO.equals(m)) {
            out.add("margin L" + m.left() + " T" + m.top() + " R" + m.right() + " B" + m.bottom());
        }
        out.add("flags " + flags(w));
        out.add("layout " + (w.needsLayout() ? "DIRTY" : "clean") + "  children " + w.getChildren().size());
        try {
            w.describe(out);
        } catch (RuntimeException ex) {
            out.add("describe() failed: " + ex);
        }
        out.add("path " + path(w));
        return out;
    }

    // Всё дерево, по строке на виджет с отступом по глубине / The whole tree, one line per widget indented by depth
    public static String dump(Widget root) {
        StringBuilder sb = new StringBuilder();
        dump(root, 0, sb);
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    private static void dump(Widget w, int depth, StringBuilder sb) {
        String indent = "  ".repeat(depth);
        if (depth > MAX_DUMP_DEPTH) {
            sb.append(indent).append("...\n");
            return;
        }
        sb.append(indent).append(label(w))
                .append("  @").append(w.absX()).append(',').append(w.absY())
                .append(' ').append(w.getWidth()).append('x').append(w.getHeight());
        if (!w.isVisible()) sb.append("  HIDDEN");
        if (!w.isActive()) sb.append("  INACTIVE");
        if (w.isFocused()) sb.append("  FOCUSED");
        sb.append('\n');
        for (Widget c : w.getChildren()) dump(c, depth + 1, sb);
    }

    private static String flags(Widget w) {
        StringBuilder sb = new StringBuilder();
        sb.append(w.isVisible() ? "visible" : "HIDDEN");
        sb.append(w.isActive() ? " active" : " INACTIVE");
        if (!w.isMounted()) sb.append(" UNMOUNTED");
        if (w.isFocusable()) sb.append(w.isFocused() ? " FOCUSED" : " focusable");
        if (w.isHovered()) sb.append(" hovered");
        if (w.isClipChildren()) sb.append(" clip");
        if (w.isPointerTransparent()) sb.append(" transparent");
        return sb.toString();
    }

    private static String fmt(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}