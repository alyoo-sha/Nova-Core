package org.mod.nova_core.ui.overlay;

import java.util.Objects;

// Параметры оверлея. Готовые наборы: popup, modal, tooltip / Overlay options. Ready-made sets: popup, modal, tooltip
public final class OverlayOptions {
    private Placement placement = Placement.centered();
    // Затемнение и блокировка всего, что под оверлеем / Dimming and blocking of everything below the overlay
    private boolean modal = false;
    private boolean dismissOnOutsideClick = false;
    private boolean dismissOnEscape = false;
    // false = оверлей не принимает ввод (тултипы) / false = the overlay takes no input (tooltips)
    private boolean interactive = true;
    // Поставить фокус на первый focusable виджет и вернуть при закрытии / Focus the first focusable widget and restore on close
    private boolean autoFocus = false;
    private int scrimColor = 0x80000000;
    private long fadeMs = 120L;

    // Контекстное меню, выпадающий список: закрывается кликом мимо и Escape / Context menu, dropdown: closes on outside click and Escape
    public static OverlayOptions popup(Placement placement) {
        return new OverlayOptions()
                .setPlacement(placement)
                .setDismissOnOutsideClick(true)
                .setDismissOnEscape(true)
                .setFadeMs(80L);
    }

    // Диалог по центру с затемнением / A centered dialog with dimming
    public static OverlayOptions modal() {
        return new OverlayOptions()
                .setPlacement(Placement.centered())
                .setModal(true)
                .setDismissOnEscape(true)
                .setAutoFocus(true)
                .setFadeMs(150L);
    }

    public static OverlayOptions tooltip(Placement placement) {
        return new OverlayOptions()
                .setPlacement(placement)
                .setInteractive(false)
                .setFadeMs(100L);
    }

    public Placement getPlacement() { return placement; }
    public boolean isModal() { return modal; }
    public boolean isDismissOnOutsideClick() { return dismissOnOutsideClick; }
    public boolean isDismissOnEscape() { return dismissOnEscape; }
    public boolean isInteractive() { return interactive; }
    public boolean isAutoFocus() { return autoFocus; }
    public int getScrimColor() { return scrimColor; }
    public long getFadeMs() { return fadeMs; }

    public OverlayOptions setPlacement(Placement placement) { this.placement = Objects.requireNonNull(placement); return this; }
    public OverlayOptions setModal(boolean modal) { this.modal = modal; return this; }
    public OverlayOptions setDismissOnOutsideClick(boolean value) { this.dismissOnOutsideClick = value; return this; }
    public OverlayOptions setDismissOnEscape(boolean value) { this.dismissOnEscape = value; return this; }
    public OverlayOptions setInteractive(boolean value) { this.interactive = value; return this; }
    public OverlayOptions setAutoFocus(boolean value) { this.autoFocus = value; return this; }
    public OverlayOptions setScrimColor(int argb) { this.scrimColor = argb; return this; }
    public OverlayOptions setFadeMs(long millis) { this.fadeMs = Math.max(0L, millis); return this; }
}