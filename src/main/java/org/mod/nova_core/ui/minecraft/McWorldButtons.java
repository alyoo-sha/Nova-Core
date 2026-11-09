package org.mod.nova_core.ui.minecraft;

import net.minecraft.client.Minecraft;
import org.mod.nova_core.ui.api.IconButton;
import org.mod.nova_core.ui.component.Widget;
import org.mod.nova_core.ui.graphics.Icons;
import org.mod.nova_core.ui.graphics.UiText;
import org.mod.nova_core.ui.overlay.TooltipBubble;
import org.mod.nova_core.ui.overlay.Tooltips;

import java.util.function.Supplier;

// Три кнопки: время, режим игры, погода. Отправляют обычные команды, поэтому нужны права на них
// Three buttons: time, game mode, weather. They send regular commands, so permissions are required
public final class McWorldButtons {
    private McWorldButtons() {}

    public static void add(Widget root, int x, int y) {
        root.add(button(x, y, () -> isDay() ? Icons.SUN : Icons.MOON, () -> NovaTexts.time(isDay()),
                () -> command("time set " + (isDay() ? "night" : "day"))));
        root.add(button(x + 24, y, () -> isCreative() ? Icons.CREATIVE : Icons.SURVIVAL,
                () -> NovaTexts.gameMode(isCreative()),
                () -> command("gamemode " + (isCreative() ? "survival" : "creative"))));
        root.add(button(x + 48, y, () -> isRaining() ? Icons.RAIN : Icons.CLEAR,
                () -> NovaTexts.weather(isRaining()),
                () -> command("weather " + (isRaining() ? "clear" : "rain"))));
    }

    private static IconButton button(int x, int y, Supplier<Icons> icon, Supplier<UiText> tip, Runnable action) {
        IconButton b = new IconButton(x, y, icon, action);
        Tooltips.attach(b, () -> new TooltipBubble(tip.get()), 150L);
        return b;
    }

    private static boolean isDay() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.getDayTime() % 24000L < 12000L;
    }

    private static boolean isCreative() {
        Minecraft mc = Minecraft.getInstance();
        return mc.gameMode != null && mc.gameMode.hasInfiniteItems();
    }

    private static boolean isRaining() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.isRaining();
    }

    private static void command(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand(command);
    }
}