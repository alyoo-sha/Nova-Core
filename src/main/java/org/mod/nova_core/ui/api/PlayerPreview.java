package org.mod.nova_core.ui.api;

import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.UiGraphics;

// Предпросмотр локального игрока, следящий за курсором / A preview of the local player that follows the cursor
public class PlayerPreview extends Widget {
    private int scale = 30;

    public PlayerPreview(int x, int y, int width, int height) {
        super(x, y, width, height);
        setPointerTransparent(true);
    }

    public PlayerPreview setScale(int scale) {
        this.scale = Math.max(1, scale);
        return this;
    }

    @Override
    protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.drawPlayerPreview(absX(), absY(), absX() + getWidth(), absY() + getHeight(), scale, mouseX, mouseY);
    }
}