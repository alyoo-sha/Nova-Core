package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.animation.Tween;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.input.EventListener;
import org.mod.nova_core.ui.input.EventType;
import org.mod.nova_core.ui.input.PointerEvent;
import org.mod.nova_core.ui.state.Subscription;

import java.util.function.Supplier;

// Тултипы: показываются после задержки наведения, скрываются при уходе курсора, клике и размонтировании
// Tooltips: shown after a hover delay, hidden on pointer leave, on click and on unmount
public final class Tooltips {
    public static final long DEFAULT_DELAY_MS = 500L;

    private Tooltips() {}

    public static Subscription attach(Widget anchor, UiText text) {
        return attach(anchor, () -> new TooltipBubble(text), DEFAULT_DELAY_MS);
    }

    public static Subscription attach(Widget anchor, Supplier<? extends Widget> content) {
        return attach(anchor, content, DEFAULT_DELAY_MS);
    }

    // content вызывается при каждом показе, поэтому содержимое всегда свежее / content is called on every show, so it is always fresh
    // Возвращаемая подписка снимает тултип / The returned subscription detaches the tooltip
    public static Subscription attach(Widget anchor, Supplier<? extends Widget> content, long delayMs) {
        Controller c = new Controller(anchor, content, Math.max(0L, delayMs));
        anchor.addListener(EventType.POINTER_ENTER, c.enter);
        anchor.addListener(EventType.POINTER_LEAVE, c.leave);
        // Capture: нажатие внутри якоря скрывает тултип до того, как кто-то заберёт событие
        // Capture: a press inside the anchor hides the tooltip before anyone claims the event
        anchor.addListener(EventType.POINTER_DOWN, c.down, true);
        anchor.whileMounted(() -> anchor.addCleanup(c::hide));
        return Subscription.of(c::detach);
    }

    private static final class Controller {
        private final Widget anchor;
        private final Supplier<? extends Widget> factory;
        private final long delayMs;

        final EventListener<PointerEvent> enter = e -> schedule();
        final EventListener<PointerEvent> leave = e -> hide();
        final EventListener<PointerEvent> down = e -> hide();

        private Tween<Float> timer = null;
        private Overlay shown = null;
        private boolean attached = true;

        Controller(Widget anchor, Supplier<? extends Widget> factory, long delayMs) {
            this.anchor = anchor;
            this.factory = factory;
            this.delayMs = delayMs;
        }

        private void schedule() {
            if (!attached) return;
            cancelTimer();
            if (shown != null && shown.isOpen()) return;
            // Таймер на общем Animator: ноль длительности и задержка = вызов через delayMs
            // A timer on the shared Animator: zero duration plus a delay = a call after delayMs
            timer = Tween.floats(0f, 0f).duration(0L).delay(delayMs).onComplete(this::show).start();
        }

        private void show() {
            timer = null;
            if (!attached || !anchor.isMounted() || !anchor.isHovered()) return;
            Widget content = factory.get();
            if (content == null) return;
            shown = OverlayHost.of(anchor).show(content, OverlayOptions.tooltip(Placement.atPointer()));
        }

        void hide() {
            cancelTimer();
            if (shown != null) {
                shown.close();
                shown = null;
            }
        }

        private void cancelTimer() {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }

        void detach() {
            attached = false;
            hide();
            anchor.removeListener(enter);
            anchor.removeListener(leave);
            anchor.removeListener(down);
        }
    }
}