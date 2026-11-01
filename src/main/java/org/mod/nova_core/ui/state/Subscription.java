package org.mod.nova_core.ui.state;

// Отменяемая подписка / A cancellable subscription
public interface Subscription {
    // Ничего не делает / Does nothing
    Subscription EMPTY = () -> { };

    // Повторный вызов безопасен / Calling it again is safe
    void cancel();

    // Подписка, которая выполняет действие один раз при отмене / A subscription that runs the action once on cancel
    static Subscription of(Runnable action) {
        return new OnceSubscription(action);
    }
}

final class OnceSubscription implements Subscription {
    private Runnable action;

    OnceSubscription(Runnable action) {
        this.action = action;
    }

    @Override
    public void cancel() {
        Runnable a = action;
        action = null;
        if (a != null) a.run();
    }
}