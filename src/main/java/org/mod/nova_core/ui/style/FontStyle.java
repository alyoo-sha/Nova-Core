package org.mod.nova_core.ui.style;

// Шрифт темы: идентификатор шрифта ресурспака вида "пространство:имя"; пусто = ванильный шрифт
// The theme font: a resource pack font id like "namespace:name"; empty = the vanilla font
public record FontStyle(String font) {
    public static final FontStyle DEFAULT = new FontStyle("");
}