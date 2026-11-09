package org.mod.nova_core.ui.minecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.mod.nova_core.ui.graphics.UiText;

import java.util.HashMap;
import java.util.Map;

// Все надписи окон. Порядок поиска: таблица в коде, затем файл перевода, затем текст игры
// All window labels. The lookup order: the table in code, then the lang file, then the game's text
public final class NovaTexts {
    public static final UiText SEARCH = tr("nova_core.creative.search");
    public static final UiText HOTBAR = tr("nova_core.creative.hotbar");
    public static final UiText RECENT = tr("nova_core.creative.recent");
    public static final UiText RESULTS = tr("nova_core.creative.results");
    public static final UiText MINECRAFT = tr("nova_core.creative.minecraft");
    public static final UiText OTHER_MODS = tr("nova_core.creative.other_mods");
    public static final UiText SAVE = tr("nova_core.creative.save");
    public static final UiText LOAD = tr("nova_core.creative.load");
    public static final UiText RECIPES = tr("nova_core.recipes");
    public static final UiText RECIPES_SHORT = tr("nova_core.recipes.short");
    public static final UiText CRAFTABLE = tr("nova_core.recipes.craftable");
    public static final UiText RECIPES_ALL = tr("nova_core.recipes.all");
    public static final UiText RECIPE_LOCKED = tr("nova_core.recipes.locked");
    public static final UiText RECIPE_NO_FIT = tr("nova_core.recipes.no_fit");

    private static final Map<String, UiText> TITLES = new HashMap<>();
    private static final Map<String, UiText> TABS = new HashMap<>();
    private static final Map<String, UiText> GROUPS = new HashMap<>();

    private NovaTexts() {}

    public static UiText tr(String key, Object... args) {
        return McText.of(Component.translatable(key, args));
    }

    public static UiText time(boolean day) {
        return tr(day ? "nova_core.world.time.day" : "nova_core.world.time.night");
    }

    public static UiText gameMode(boolean creative) {
        return tr(creative ? "nova_core.world.mode.creative" : "nova_core.world.mode.survival");
    }

    public static UiText weather(boolean raining) {
        return tr(raining ? "nova_core.world.weather.rain" : "nova_core.world.weather.clear");
    }

    // ---------- Переопределения из кода / Overrides from code ----------

    // Заголовок окна по ключу перевода ванильного названия: title("container.crafting", "Крафт")
    // A window title by the translation key of the vanilla name: title("container.crafting", "Crafting")
    public static void title(String vanillaKey, String text) { TITLES.put(vanillaKey, UiText.literal(text)); }

    // Название вкладки креатива по её id: tab("minecraft:combat", "Бой") / A creative tab name by its id
    public static void tab(String tabId, String text) { TABS.put(tabId, UiText.literal(text)); }

    // Название группы вкладок по id мода / A tab group name by the mod id
    public static void group(String modId, String text) { GROUPS.put(modId, UiText.literal(text)); }

    // ---------- Поиск текста / Text lookup ----------

    private static UiText lookup(Map<String, UiText> table, String id, String langKey, UiText fallback) {
        UiText coded = table.get(id);
        if (coded != null) return coded;
        return I18n.exists(langKey) ? tr(langKey) : fallback;
    }

    // Заголовок окна: код, затем nova_core.title.<ключ>, затем ванильное название / A window title: code, then nova_core.title.<key>, then the vanilla name
    public static UiText titleFor(Component vanilla) {
        UiText fallback = McText.of(vanilla);
        if (!(vanilla.getContents() instanceof TranslatableContents contents)) return fallback;
        return lookup(TITLES, contents.getKey(), "nova_core.title." + contents.getKey(), fallback);
    }

    public static UiText tabName(String tabId, UiText fallback) {
        return lookup(TABS, tabId, "nova_core.tab." + tabId.replace(':', '.'), fallback);
    }

    public static UiText groupName(String modId, UiText fallback) {
        return lookup(GROUPS, modId, "nova_core.group." + modId, fallback);
    }

    public static UiText bold(UiText text) {
        return McText.of(McText.toComponent(text).copy().withStyle(ChatFormatting.BOLD));
    }
}