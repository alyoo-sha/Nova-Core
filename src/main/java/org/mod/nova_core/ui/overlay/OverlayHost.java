package org.mod.nova_core.ui.overlay;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.input.EventListener;
import org.mod.nova_core.ui.input.EventType;
import org.mod.nova_core.ui.input.KeyboardEvent;
import org.mod.nova_core.ui.input.Keys;
import org.mod.nova_core.ui.layout.SizeMode;

import java.util.ArrayList;
import java.util.List;

// Слой оверлеев поверх экрана. Создаётся хостом (UIScreen); найти его из любого виджета: OverlayHost.of(widget)
// The overlay layer above the screen. Created by the host (UIScreen); find it from any widget: OverlayHost.of(widget)
public final class OverlayHost extends Widget {
    // Оверлеи, которые нужно закрыть до следующей отрисовки / Overlays to close before the next render
    private final List<OverlayEntry> pendingClose = new ArrayList<>();
    private final EventListener<KeyboardEvent> keyDownListener = this::onKeyDownCapture;
    private final EventListener<KeyboardEvent> keyBlockListener = this::onKeyBlockCapture;

    private int pointerX = 0;
    private int pointerY = 0;

    public OverlayHost() {
        super(0, 0, 0, 0);
        setSizeMode(SizeMode.FILL, SizeMode.FILL);
        setPointerTransparent(true);
    }

    // Слой оверлеев экрана, в котором лежит виджет / The overlay layer of the screen that contains the widget
    public static OverlayHost of(Widget any) {
        for (Widget c : any.root().getChildren()) {
            if (c instanceof OverlayHost host) return host;
        }
        throw new IllegalStateException("No overlay host: the widget is not inside a UIScreen");
    }

    // Последняя известная позиция курсора; обновляет хост каждый кадр / The last known pointer position; updated by the host every frame
    public void setPointer(int x, int y) {
        this.pointerX = x;
        this.pointerY = y;
    }

    public int getPointerX() { return pointerX; }
    public int getPointerY() { return pointerY; }

    // ---------- Открытие и закрытие / Opening and closing ----------

    public Overlay show(Widget content, OverlayOptions options) {
        OverlayEntry entry = new OverlayEntry(this, content, options);
        add(entry);
        entry.onShown();
        return entry.handle();
    }

    public boolean hasModal() { return topModalIndex() >= 0; }
    // Область навигации по фокусу: содержимое верхнего оверлея, принимающего ввод (модальное окно или попап); иначе null
    // The focus navigation scope: the content of the topmost input-taking overlay (a modal or a popup); otherwise null
    public Widget navigationScope() {
        OverlayEntry top = topInteractive();
        return top == null ? null : top.content();
    }

    // Содержимое верхнего модального окна или null / The content of the topmost modal or null
    public Widget modalScope() {
        int i = topModalIndex();
        return i < 0 ? null : ((OverlayEntry) getChildren().get(i)).content();
    }

    // Закрыть все с анимацией / Close all with animation
    public void closeAll() {
        for (OverlayEntry e : entries()) e.requestClose();
    }

    // Убрать все сразу, без анимации; для пересборки и закрытия экрана / Remove all instantly, no animation; for rebuilds and screen close
    public void clearImmediately() {
        pendingClose.clear();
        for (OverlayEntry e : entries()) e.removeNow();
    }

    void deferClose(OverlayEntry entry) {
        if (!pendingClose.contains(entry)) pendingClose.add(entry);
    }

    // Хост экрана вызывает раз в кадр до раскладки / The screen host calls it once per frame before layout
    public void flush() {
        if (pendingClose.isEmpty()) return;
        List<OverlayEntry> copy = new ArrayList<>(pendingClose);
        pendingClose.clear();
        for (OverlayEntry e : copy) e.requestClose();
    }

    private List<OverlayEntry> entries() {
        List<OverlayEntry> result = new ArrayList<>();
        for (Widget c : getChildren()) if (c instanceof OverlayEntry e) result.add(e);
        return result;
    }

    // ---------- Клавиатура: Escape и блокировка под модальным окном / Keyboard: Escape and blocking below a modal ----------

    @Override
    protected void onMount() {
        Widget p = getParent();
        if (p == null) return;
        // Capture на корне дерева видит клавиши раньше любого виджета / Capture on the tree root sees keys before any widget
        p.addListener(EventType.KEY_DOWN, keyDownListener, true);
        p.addListener(EventType.KEY_UP, keyBlockListener, true);
        p.addListener(EventType.CHAR, keyBlockListener, true);
        addCleanup(() -> {
            p.removeListener(keyDownListener);
            p.removeListener(keyBlockListener);
        });
    }

    private void onKeyDownCapture(KeyboardEvent e) {
        if (e.getKey() == Keys.ESCAPE) {
            OverlayEntry top = topInteractive();
            if (top != null) {
                if (top.options().isDismissOnEscape()) {
                    top.requestClose();
                    e.stopPropagation();
                    return;
                }
                if (top.options().isModal()) {
                    e.stopPropagation();
                    return;
                }
            }
        }
        onKeyBlockCapture(e);
    }

    // Пока открыто модальное окно, клавиши, адресованные виджетам вне него (и вне оверлеев над ним), гасятся
    // While a modal is open, keys addressed to widgets outside it (and outside overlays above it) are swallowed
    private void onKeyBlockCapture(KeyboardEvent e) {
        int modalIndex = topModalIndex();
        if (modalIndex < 0) return;
        if (e.getTarget() instanceof Widget target) {
            List<Widget> kids = getChildren();
            for (int i = modalIndex; i < kids.size(); i++) {
                if (isInside(target, kids.get(i))) return;
            }
        }
        e.stopPropagation();
    }

    private OverlayEntry topInteractive() {
        List<Widget> kids = getChildren();
        for (int i = kids.size() - 1; i >= 0; i--) {
            if (kids.get(i) instanceof OverlayEntry e && e.isInteractive()) return e;
        }
        return null;
    }

    private int topModalIndex() {
        List<Widget> kids = getChildren();
        for (int i = kids.size() - 1; i >= 0; i--) {
            if (kids.get(i) instanceof OverlayEntry e && !e.isClosing() && e.options().isModal()) return i;
        }
        return -1;
    }

    private static boolean isInside(Widget w, Widget ancestor) {
        for (Widget p = w; p != null; p = p.getParent()) {
            if (p == ancestor) return true;
        }
        return false;
    }
}