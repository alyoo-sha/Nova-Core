package org.mod.nova_core.menu;

// Геометрия контейнерного экрана: панель механизма сверху, стандартная панель инвентаря снизу.
// Общий для меню и экрана, поэтому позиции слотов никогда не расходятся
// The geometry of a container screen: the machine panel on top, the standard inventory panel below.
// Shared by the menu and the screen, so slot positions never diverge
public final class MachineLayout {
    public static final int SLOT = 18;
    public static final int INVENTORY_WIDTH = 176;
    public static final int INVENTORY_HEIGHT = 101;
    public static final int PANEL_GAP = 4;

    private final int machineWidth;
    private final int machineHeight;
    private final int imageWidth;
    private final int imageHeight;
    private final int machineLeft;
    private final int inventoryLeft;
    private final int inventoryTop;

    public MachineLayout(int machineWidth, int machineHeight) {
        if (machineWidth < 1 || machineHeight < 1) throw new IllegalArgumentException("Panel size must be positive");
        this.machineWidth = machineWidth;
        this.machineHeight = machineHeight;
        this.imageWidth = Math.max(machineWidth, INVENTORY_WIDTH);
        this.machineLeft = (imageWidth - machineWidth) / 2;
        this.inventoryLeft = (imageWidth - INVENTORY_WIDTH) / 2;
        this.inventoryTop = machineHeight + PANEL_GAP;
        this.imageHeight = inventoryTop + INVENTORY_HEIGHT;
    }

    public int machineWidth() { return machineWidth; }
    public int machineHeight() { return machineHeight; }
    public int imageWidth() { return imageWidth; }
    public int imageHeight() { return imageHeight; }
    public int machineLeft() { return machineLeft; }
    public int inventoryLeft() { return inventoryLeft; }
    public int inventoryTop() { return inventoryTop; }

    // Координаты верхнего левого угла предмета в слоте. Для слотов механизма localX и localY измеряются
    // от угла панели механизма; рамка слота на пиксель левее и выше
    // The top-left of the item in a slot. For machine slots localX and localY are measured
    // from the machine panel's corner; the slot frame is one pixel to the left and above
    public int machineX(int localX) { return machineLeft + localX; }
    public int machineY(int localY) { return localY; }

    // Слоты инвентаря игрока: колонка и ряд, ряд хотбара отдельно / Player inventory slots: column and row, the hotbar row separately
    public int inventoryX(int column) { return inventoryLeft + 8 + column * SLOT; }
    public int inventoryY(int row) { return inventoryTop + 19 + row * SLOT; }
    public int hotbarY() { return inventoryTop + 77; }
}