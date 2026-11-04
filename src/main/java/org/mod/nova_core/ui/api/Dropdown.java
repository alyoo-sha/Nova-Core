package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.context.UiContext;
import org.mod.nova_core.ui.context.UiPlatform;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.layout.Constraints;
import org.mod.nova_core.ui.layout.Size;
import org.mod.nova_core.ui.overlay.MenuItem;
import org.mod.nova_core.ui.overlay.Menus;
import org.mod.nova_core.ui.overlay.Overlay;
import org.mod.nova_core.ui.overlay.Placement;
import org.mod.nova_core.ui.state.Signal;
import org.mod.nova_core.ui.style.ButtonStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

// Выпадающий список: кнопка с текущим пунктом, по клику открывает попап той же ширины.
// Выбранный индекс хранится в Signal: можно передать свой
// A dropdown: a button with the current option that opens a popup of the same width on click.
// The selected index lives in a Signal: pass your own
public class Dropdown extends ButtonWidget {
    private final List<UiText> options;
    private final Signal<Integer> selected;

    private IntConsumer onSelect = i -> { };
    private Overlay open = null;

    public Dropdown(int x, int y, int width, int height, List<UiText> options, int initial) {
        this(x, y, width, height, options, Signal.of(initial));
    }

    public Dropdown(int x, int y, int width, int height, List<UiText> options, Signal<Integer> selected) {
        super(x, y, width, height, initialLabel(options, selected), () -> { });
        this.options = List.copyOf(options);
        this.selected = Objects.requireNonNull(selected);
        setOnClick(this::toggleMenu);
        // Подпись следует за сигналом, даже если его меняют снаружи / The label follows the signal even if it is changed from outside
        bind(selected, i -> setLabel(this.options.get(clamp(i, this.options.size()))));
    }

    private static UiText initialLabel(List<UiText> options, Signal<Integer> selected) {
        if (options.isEmpty()) throw new IllegalArgumentException("A dropdown needs at least one option");
        return options.get(clamp(selected.peek(), options.size()));
    }

    private static int clamp(int index, int size) {
        return Math.max(0, Math.min(index, size - 1));
    }

    public int getSelectedIndex() { return clamp(selected.peek(), options.size()); }

    public Signal<Integer> selectedSignal() { return selected; }

    public Dropdown setSelectedIndex(int index) {
        selected.set(clamp(index, options.size()));
        return this;
    }

    // Вызывается, когда пользователь выбрал пункт в списке / Called when the user picks an option in the list
    public Dropdown setOnSelect(IntConsumer onSelect) {
        this.onSelect = Objects.requireNonNull(onSelect);
        return this;
    }

    private void toggleMenu() {
        if (open != null && open.isOpen()) {
            open.close();
            open = null;
            return;
        }
        List<MenuItem> items = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            items.add(MenuItem.of(options.get(i), () -> {
                setSelectedIndex(index);
                onSelect.accept(index);
            }));
        }
        open = Menus.show(this, Placement.anchored(this, Placement.Side.BELOW), items, getWidth(), 160, getSelectedIndex());
    }

    // В режиме WRAP ширина по самому длинному пункту плюс место под стрелку / In WRAP mode the width fits the longest option plus room for the arrow
    @Override
    protected Size measureContent(Constraints c) {
        ButtonStyle s = getStyle();
        UiPlatform p = UiContext.platform();
        int widest = 0;
        for (UiText option : options) widest = Math.max(widest, p.textWidth(option));
        return new Size(widest + s.paddingX() * 2 + 14, s.height());
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderSelf(g, mouseX, mouseY, partialTick);
        ButtonStyle s = getStyle();
        int color = isActive() ? s.text() : s.disabledText();
        // Стрелка вниз из четырёх строк / A down arrow of four rows
        int ax = absX() + getWidth() - 12;
        int ay = absY() + (getHeight() - 4) / 2;
        for (int i = 0; i < 4; i++) g.fill(ax + i, ay + i, ax + 8 - i, ay + i + 1, color);
    }
}