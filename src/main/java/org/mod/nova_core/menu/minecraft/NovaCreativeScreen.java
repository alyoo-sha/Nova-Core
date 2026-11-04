package org.mod.nova_core.menu.minecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.mod.nova_core.mixin.CreativeScreenAccessor;
import org.mod.nova_core.ui.api.Panel;
import org.mod.nova_core.ui.api.SlotFrame;
import org.mod.nova_core.ui.api.TextWidget;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.Draw;
import org.mod.nova_core.ui.graphics.UiGraphics;
import org.mod.nova_core.ui.minecraft.McText;
import org.mod.nova_core.ui.minecraft.UiHost;
import org.mod.nova_core.ui.style.FieldStyle;
import org.mod.nova_core.ui.style.SliderStyle;
import org.mod.nova_core.ui.style.Styles;
import org.mod.nova_core.ui.theme.ThemeManager;

public class NovaCreativeScreen extends CreativeModeInventoryScreen {
    private final UiHost host = new UiHost(this::buildTree, () -> null);
    // Для какой вкладки и набора слотов собрано дерево; при смене пересобирается / The tab and slot set the tree was built for; rebuilt on change
    private CreativeModeTab builtTab = null;
    private int builtSlots = -1;

    public NovaCreativeScreen(Player player, FeatureFlagSet features, boolean operatorTab) {
        super(player, features, operatorTab);
    }

    private static CreativeScreenAccessor access(Object screen) {
        return (CreativeScreenAccessor) screen;
    }

    @Override
    protected void init() {
        super.init();
        host.init(width, height);
    }

    private void buildTree(Widget root) {
        CreativeModeTab tab = CreativeScreenAccessor.nova$getSelectedTab();
        builtTab = tab;
        builtSlots = menu.slots.size();

        Panel panel = new Panel(leftPos, topPos, imageWidth, imageHeight, Styles.POPUP);
        panel.setDebugName("creative");
        if (tab.showTitle()) {
            panel.add(new TextWidget(8, 6, McText.of(tab.getDisplayName().copy().withStyle(ChatFormatting.BOLD))));
        }
        root.add(panel);
        root.add(new SearchFrame());

        // Рамки по настоящим слотам: у вкладки инвентаря набор слотов другой / Frames follow the real slots: the inventory tab has a different set
        for (Slot slot : menu.slots) {
            SlotFrame frame = new SlotFrame(false);
            frame.setPosition(leftPos + slot.x - 1, topPos + slot.y - 1);
            root.add(frame);
        }
        if (tab.canScroll()) root.add(new Scrollbar());
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        host.renderBackdrop(g);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        CreativeModeTab selected = CreativeScreenAccessor.nova$getSelectedTab();
        if (selected != builtTab || menu.slots.size() != builtSlots) host.init(width, height);

        // Как в ванильном: невыбранные вкладки под панелью, выбранная поверх / As in vanilla: unselected tabs below the panel, the selected one above
        for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
            if (tab != selected) renderTabButton(g, tab);
        }
        host.prepare(mouseX, mouseY);
        host.renderContent(g, mouseX, mouseY, partialTick);

        EditBox search = access(this).nova$getSearchBox();
        if (search != null && search.visible) search.render(g, mouseX, mouseY, partialTick);
        renderTabButton(g, selected);

        if (selected.getType() == CreativeModeTab.Type.INVENTORY && minecraft != null && minecraft.player != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, leftPos + 88, topPos + 45, 20,
                    (float) (leftPos + 88 - mouseX), (float) (topPos + 45 - 30 - mouseY), minecraft.player);
        }
    }

    // Название вкладки рисует дерево / The tab title is drawn by the tree
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        host.renderTop(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        host.removed();
        super.removed();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        return host.mouseClicked(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        return host.mouseReleased(mx, my, button) || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return host.mouseDragged(mx, my, button, dx, dy) || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        return host.mouseScrolled(mx, my, delta) || super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        return host.keyPressed(key, scanCode, modifiers) || super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int key, int scanCode, int modifiers) {
        return host.keyReleased(key, scanCode, modifiers) || super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return host.charTyped(c, modifiers) || super.charTyped(c, modifiers);
    }

    // Рамка под ванильным полем поиска; границы поля читаются каждый кадр / A frame under the vanilla search box; its bounds are read every frame
    private final class SearchFrame extends Widget {
        SearchFrame() {
            super(0, 0, 0, 0);
            setPointerTransparent(true);
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            EditBox box = access(NovaCreativeScreen.this).nova$getSearchBox();
            if (box == null || !box.visible) return;
            FieldStyle s = ThemeManager.current().get(Styles.FIELD);
            Draw.box(g, box.getX() - 3, box.getY() - 3, box.getWidth() + 6, box.getHeight() + 6,
                    s.radius(), s.background(), s.border(), 1);
        }
    }

    // Полоса прокрутки: ванильные размеры и положение, вид из темы / The scrollbar: vanilla size and position, the look from the theme
    private final class Scrollbar extends Widget {
        Scrollbar() {
            super(leftPos + 175, topPos + 18, 12, 112);
            setPointerTransparent(true);
        }

        @Override
        protected void renderSelf(UiGraphics g, int mouseX, int mouseY, float partialTick) {
            SliderStyle s = ThemeManager.current().get(Styles.SLIDER);
            int ax = absX();
            int ay = absY();
            g.fillRounded(ax + 4, ay, ax + 8, ay + getHeight(), 2, s.track());
            float offset = access(NovaCreativeScreen.this).nova$getScrollOffs();
            int thumbY = ay + (int) ((getHeight() - 17) * offset);
            g.fillRounded(ax + 2, thumbY, ax + 10, thumbY + 15, 3, menu.canScroll() ? s.thumbHover() : s.thumb());
        }
    }
}