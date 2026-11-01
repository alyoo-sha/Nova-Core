package org.mod.nova_core.ui.style;

// Ключи встроенных стилей / Keys of the built-in styles
public final class Styles {
    public static final StyleKey<PanelStyle> PANEL =
            StyleKey.of("nova_core:panel", PanelStyle.class, PanelStyle.DEFAULT);
    public static final StyleKey<ButtonStyle> BUTTON =
            StyleKey.of("nova_core:button", ButtonStyle.class, ButtonStyle.DEFAULT);
    public static final StyleKey<TextStyle> TEXT =
            StyleKey.of("nova_core:text", TextStyle.class, TextStyle.DEFAULT);
    public static final StyleKey<ScrollbarStyle> SCROLLBAR =
            StyleKey.of("nova_core:scrollbar", ScrollbarStyle.class, ScrollbarStyle.DEFAULT);
    public static final StyleKey<TooltipStyle> TOOLTIP =
            StyleKey.of("nova_core:tooltip", TooltipStyle.class, TooltipStyle.DEFAULT);
    public static final StyleKey<FieldStyle> FIELD =
            StyleKey.of("nova_core:field", FieldStyle.class, FieldStyle.DEFAULT);
    public static final StyleKey<SliderStyle> SLIDER =
            StyleKey.of("nova_core:slider", SliderStyle.class, SliderStyle.DEFAULT);
    // Непрозрачная поверхность для попапов, меню и диалогов / An opaque surface for popups, menus and dialogs
    public static final StyleKey<PanelStyle> POPUP =
            StyleKey.of("nova_core:popup", PanelStyle.class, new PanelStyle(0xFF181818, 0xFF505050, 1));

    private Styles() {}
}