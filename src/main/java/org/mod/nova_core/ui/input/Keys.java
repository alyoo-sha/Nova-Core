package org.mod.nova_core.ui.input;

// Коды клавиш и модификаторов (как в GLFW) / Key and modifier codes (same as GLFW)
public final class Keys {
    public static final int SPACE = 32;
    public static final int ESCAPE = 256;
    public static final int ENTER = 257;
    public static final int TAB = 258;
    public static final int BACKSPACE = 259;
    public static final int DELETE = 261;
    public static final int RIGHT = 262;
    public static final int LEFT = 263;
    public static final int DOWN = 264;
    public static final int UP = 265;
    public static final int PAGE_UP = 266;
    public static final int PAGE_DOWN = 267;
    public static final int HOME = 268;
    public static final int END = 269;
    public static final int KP_ENTER = 335;
    public static final int A = 65;
    public static final int C = 67;
    public static final int V = 86;
    public static final int X = 88;
    public static final int B = 66;
    public static final int I = 73;
    public static final int L = 76;
    public static final int P = 80;
    public static final int F12 = 301;

    // Биты модификаторов / Modifier bits
    public static final int MOD_SHIFT = 1;
    public static final int MOD_CONTROL = 2;
    public static final int MOD_ALT = 4;
    // Cmd на macOS, Win на остальных / Cmd on macOS, Win elsewhere
    public static final int MOD_SUPER = 8;

    private Keys() {}
}