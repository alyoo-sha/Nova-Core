package org.mod.nova_core.ui.graphics;

import java.util.ArrayList;
import java.util.List;

// Пиксельные иконки 16x16 из битовых карт: 'X' закрашенный пиксель. Горизонтальные серии сливаются в один прямоугольник
// 16x16 pixel icons from bitmaps: 'X' is a filled pixel. Horizontal runs merge into one rectangle
public enum Icons {
    TRASH("................",
            "................",
            "....XXXXXXXX....",
            "....X......X....",
            "..XXXXXXXXXXXX..",
            "...X........X...",
            "...X..X..X..X...",
            "...X..X..X..X...",
            "...X..X..X..X...",
            "...X..X..X..X...",
            "...X..X..X..X...",
            "...X..X..X..X...",
            "...X........X...",
            "....XXXXXXXX....",
            "................",
            "................"),
    MOON("................",
            "................",
            "........XX......",
            ".........XXX....",
            "..........XXX...",
            "..........XXX...",
            "..........XXXX..",
            "..........XXXX..",
            "..........XXXX..",
            "..........XXXX..",
            ".........XXXX...",
            "........XXXXX...",
            "....XXXXXXXX....",
            "......XXXX......",
            "................",
            "................"),
    SUN("................",
            "................",
            ".......XX.......",
            "..X....XX....X..",
            "...X........X...",
            "......XXXX......",
            ".....XXXXXX.....",
            "..XX.XXXXXX.XX..",
            "..XX.XXXXXX.XX..",
            ".....XXXXXX.....",
            "......XXXX......",
            "...X........X...",
            "..X....XX....X..",
            ".......XX.......",
            "................",
            "................"),
    SURVIVAL(
            "................",
            "................",
            ".....XXXXX......",
            "....XXXXXXX.....",
            ".....XXXXXXX....",
            ".........XXXX...",
            "........XXXXXX..",
            ".......XXX.XXX..",
            "......XXX..XXX..",
            ".....XXX...XXX..",
            "....XXX....XXX..",
            "...XXX......X...",
            "..XXX...........",
            "..XX............",
            "................",
            "................"),
    CREATIVE(
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "...XXXX..XXXX...",
            "..XX..XXXX..XX..",
            "..XX...XX...XX..",
            "..XX...XX...XX..",
            "..XX..XXXX..XX..",
            "...XXXX..XXXX...",
            "................",
            "................",
            "................",
            "................"),
    CLEAR(
            "................",
            "................",
            "................",
            "................",
            ".......XXXX.....",
            "......X....X....",
            "......X....X....",
            "......X....X....",
            "...XXX......X...",
            "..X..........X..",
            "..X..........X..",
            "...XXXXXXXXXX...",
            "................",
            "................",
            "................",
            "................"),
    RAIN(
            "................",
            "................",
            ".......XXXX.....",
            "......X....X....",
            "......X....X....",
            "......X....X....",
            "...XXX......X...",
            "..X..........X..",
            "..X..........X..",
            "...XXXXXXXXXX...",
            "................",
            "....XX..XX..XX..",
            "...XX..XX..XX...",
            "..XX..XX..XX....",
            "................",
            "................");

    public static final int SIZE = 16;
    private static final int SHADOW = 0xFF202020;

    // Серии {строка, столбец, длина}, считаются один раз / Runs {row, column, length}, computed once
    private final int[][] runs;

    Icons(String... rows) {
        List<int[]> list = new ArrayList<>();
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int c = 0;
            while (c < row.length()) {
                if (row.charAt(c) != 'X') {
                    c++;
                    continue;
                }
                int start = c;
                while (c < row.length() && row.charAt(c) == 'X') c++;
                list.add(new int[] {r, start, c - start});
            }
        }
        this.runs = list.toArray(new int[0][]);
    }

    public void draw(UiGraphics g, int x, int y, int color) {
        for (int[] run : runs) g.fill(x + run[1], y + run[0], x + run[1] + run[2], y + run[0] + 1, color);
    }

    // С тёмной тенью со сдвигом (1, 1) / With a dark shadow offset by (1, 1)
    public void drawShadowed(UiGraphics g, int x, int y, int color) {
        draw(g, x + 1, y + 1, SHADOW);
        draw(g, x, y, color);
    }
}