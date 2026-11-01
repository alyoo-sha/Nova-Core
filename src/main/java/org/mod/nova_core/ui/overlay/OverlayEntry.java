package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.layout.SizeMode;
import org.mod.nova_core.ui.navigation.FocusNavigator;

// Один оверлей: затемнение/перехватчик кликов (если нужен) + содержимое. Занимает весь экран, сам ввод не принимает
// One overlay: a scrim / click catcher (if needed) + the content. Fills the screen, takes no input itself
final class OverlayEntry extends Widget {
    private final OverlayHost host;
    private final Widget content;
    private final OverlayOptions options;
    private final Overlay handle;

    private boolean closing = false;
    private boolean removed = false;
    // Кто был в фокусе до показа; ему вернётся фокус при закрытии / Who had focus before showing; gets the focus back on close
    private Widget previousFocus = null;

    OverlayEntry(OverlayHost host, Widget content, OverlayOptions options) {
        super(0, 0, 0, 0);
        this.host = host;
        this.content = content;
        this.options = options;
        this.handle = new Overlay(this);
        setSizeMode(SizeMode.FILL, SizeMode.FILL);
        setPointerTransparent(true);
        if (!options.isInteractive()) setActive(false);
        if (options.isModal() || options.isDismissOnOutsideClick()) add(new Backdrop(this, options));
        add(content);
    }

    Widget content() { return content; }
    OverlayOptions options() { return options; }
    Overlay handle() { return handle; }
    boolean isClosing() { return closing; }
    boolean isInteractive() { return options.isInteractive() && !closing; }

    // Вызывается хостом сразу после добавления / Called by the host right after adding
    void onShown() {
        previousFocus = host.getFocusOwner();
        if (options.isAutoFocus()) {
            Widget first = FocusNavigator.first(content);
            if (first != null) first.requestFocus();
        }
        // Под модальным окном фокуса быть не должно / There must be no focus below a modal
        if (options.isModal() && !isFocusInside()) host.clearFocus();
    }

    private boolean isFocusInside() {
        for (Widget p = host.getFocusOwner(); p != null; p = p.getParent()) {
            if (p == this) return true;
        }
        return false;
    }

    // Если фокус сейчас внутри оверлея, вернуть его тому, у кого он был до показа
    // If the focus is inside the overlay right now, give it back to whoever had it before showing
    private void returnFocus() {
        if (!isFocusInside()) return;
        host.clearFocus();
        if (previousFocus != null && previousFocus.isMounted()) previousFocus.requestFocus();
    }

    @Override
    protected void onMount() {
        long ms = options.getFadeMs();
        if (ms > 0L) {
            setOpacity(0f);
            animateOpacity(1f, ms);
        }
    }

    void requestClose() {
        if (closing) return;
        closing = true;
        setActive(false);
        returnFocus();
        long ms = options.getFadeMs();
        if (ms > 0L && isMounted()) animateOpacity(0f, ms, this::finish);
        else finish();
    }

    // Мгновенное удаление без анимации и без возврата фокуса / Instant removal with no animation and no focus return
    void removeNow() {
        closing = true;
        finish();
    }

    private void finish() {
        if (removed) return;
        removed = true;
        host.remove(this);
        handle.fireClosed();
    }

    // Положение содержимого пересчитывается каждый кадр, поэтому оверлей следует за якорем
    // The content position is recomputed every frame, so the overlay follows its anchor
    private void reposition() {
        Widget anchor = options.getPlacement().anchor();
        if (anchor != null && anchor.root() != host.root()) {
            host.deferClose(this); // якорь убрали из дерева; закрывать из отрисовки нельзя / the anchor left the tree; closing from rendering is unsafe
            return;
        }
        Placement.Position p = options.getPlacement().resolve(
                getWidth(), getHeight(), content.getWidth(), content.getHeight(),
                host.getPointerX(), host.getPointerY());
        content.setPosition(p.x(), p.y());
    }

    @Override
    protected void layoutChildren() {
        reposition();
        super.layoutChildren();
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        reposition();
    }

    // Перехватывает клики вне содержимого; рисует затемнение для модальных / Catches clicks outside the content; draws the scrim for modals
    private static final class Backdrop extends Widget {
        private final OverlayEntry entry;
        private final boolean modal;
        private final boolean dismiss;
        private final int scrim;

        Backdrop(OverlayEntry entry, OverlayOptions options) {
            super(0, 0, 0, 0);
            this.entry = entry;
            this.modal = options.isModal();
            this.dismiss = options.isDismissOnOutsideClick();
            this.scrim = options.getScrimColor();
            setSizeMode(SizeMode.FILL, SizeMode.FILL);
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            if (modal && (scrim >>> 24) != 0) g.fill(absX(), absY(), absX() + getWidth(), absY() + getHeight(), scrim);
        }

        @Override
        protected void onPointerDown(PointerEvent e) {
            if (dismiss) entry.requestClose();
            e.stopPropagation(); // клик не должен дойти до того, что под оверлеем / the click must not reach what is below the overlay
        }
    }
}