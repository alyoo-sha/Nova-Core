package org.mod.nova_core.ui.style;

// Фон (однотонный или градиент) и рамка панели; цвета ARGB / Panel background (solid or gradient) and border; ARGB colors
public record PanelStyle(Gradient fill, int border, int borderWidth, int radius) {
    public static final PanelStyle DEFAULT = new PanelStyle(0xC0101010, 0xFF404040, 1);

    // Прежняя форма: однотонный фон и прямые углы / The old form: a solid background and square corners
    public PanelStyle(int background, int border, int borderWidth) {
        this(Gradient.solid(background), border, borderWidth, 0);
    }

    public int background() { return fill.topLeft(); }

    public PanelStyle withBackground(int argb) { return new PanelStyle(Gradient.solid(argb), border, borderWidth, radius); }
    public PanelStyle withFill(Gradient gradient) { return new PanelStyle(gradient, border, borderWidth, radius); }
    public PanelStyle withBorder(int argb) { return new PanelStyle(fill, argb, borderWidth, radius); }
    public PanelStyle withBorderWidth(int width) { return new PanelStyle(fill, border, width, radius); }
    public PanelStyle withRadius(int radius) { return new PanelStyle(fill, border, borderWidth, radius); }
}