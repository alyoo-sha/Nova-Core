package org.mod.nova_core.ui.minecraft.container.interaction;

// Данные мыши для модулей работы с инвентарём; координаты в пикселях экрана (с учётом GUI scale)
// Mouse data for inventory-interaction modules; coordinates are in screen pixels (GUI-scaled)
public record InventoryMouseEvent(double mouseX, double mouseY, int button, double dragX, double dragY, double scrollDelta) {

    // Номер кнопки у колеса / Button value used for the wheel
    public static final int NO_BUTTON = -1;

    public static InventoryMouseEvent press(double mx, double my, int button) {
        return new InventoryMouseEvent(mx, my, button, 0.0, 0.0, 0.0);
    }

    public static InventoryMouseEvent release(double mx, double my, int button) {
        return new InventoryMouseEvent(mx, my, button, 0.0, 0.0, 0.0);
    }

    public static InventoryMouseEvent drag(double mx, double my, int button, double dx, double dy) {
        return new InventoryMouseEvent(mx, my, button, dx, dy, 0.0);
    }

    public static InventoryMouseEvent scroll(double mx, double my, double delta) {
        return new InventoryMouseEvent(mx, my, NO_BUTTON, 0.0, 0.0, delta);
    }
}