package org.mod.nova_core.ui.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.joml.Matrix4f;
import org.mod.nova_core.Nova_core;
import org.mod.nova_core.ui.graphics.*;

import java.util.ArrayDeque;

// UiGraphics поверх GuiGraphics (Forge 1.20.1) / UiGraphics over GuiGraphics (Forge 1.20.1)
public final class McGraphics implements UiGraphics {
    private static final int MAX_SEGMENTS = 12;
    private static final int MAX_LOOP = 4 * (MAX_SEGMENTS + 1);

    private GuiGraphics delegate = null;
    // Стек множителей прозрачности и текущее произведение / Opacity multiplier stack and the current product
    private final ArrayDeque<Float> opacityStack = new ArrayDeque<>();
    private float opacity = 1f;

    // Рабочие массивы контуров, создаются сразу и переиспользуются / Scratch arrays for outlines, created eagerly and reused
    private final float[][] loopX = new float[4][MAX_LOOP];
    private final float[][] loopY = new float[4][MAX_LOOP];
    // Текущий градиент: его границы и цвета четырёх углов / The current gradient: its bounds and the four corner colors
    private float gx1;
    private float gy1;
    private float gx2;
    private float gy2;
    private int gTl;
    private int gTr;
    private int gBl;
    private int gBr;

    // Начало и конец кадра / Start and end of a frame
    public void begin(GuiGraphics g) {
        this.delegate = g;
        opacityStack.clear();
        opacity = 1f;
    }

    public void end() { this.delegate = null; }

    // Доступ к настоящему GuiGraphics для того, чего ещё нет в UiGraphics (например, предметы)
    // Access to the real GuiGraphics for what UiGraphics lacks yet (e.g. items)
    public GuiGraphics raw() { return delegate; }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        int c = applyOpacity(argb);
        if ((c >>> 24) == 0) return;
        delegate.fill(x1, y1, x2, y2, c);
    }

    @Override
    public void drawText(UiText text, int x, int y, int argb, boolean shadow) {
        // Font считает альфу меньше 4 «отсутствующей» и рисует непрозрачно; повторяем это правило здесь,
        // чтобы затухание не давало вспышек
        // Font treats alpha below 4 as "missing" and draws opaque; we mirror that rule here
        // so fading produces no flashes
        if ((argb & 0xFC000000) == 0) argb |= 0xFF000000;
        int c = applyOpacity(argb);
        if ((c >>> 24) < 4) return;

        Font font = Minecraft.getInstance().font;
        Component component = McText.toComponent(text);

        // Тень рисуем сами (встроенная сдвигает текст по глубине): затемнённая копия со сдвигом (1, 1), шрифт тот же
        // We draw the shadow ourselves (the built-in one shifts the text in depth): a dimmed copy offset by (1, 1), the same font
        if (shadow) {
            int shadowColor = ((c & 0xFCFCFC) >> 2) | (c & 0xFF000000);
            Component shadowText = Component.literal(component.getString())
                    .withStyle(Style.EMPTY.withFont(component.getStyle().getFont()));
            delegate.drawString(font, shadowText, x + 1, y + 1, shadowColor, false);
        }
        delegate.drawString(font, component, x, y, c, false);
    }

    @Override
    public void pushClip(int x1, int y1, int x2, int y2) {
        delegate.enableScissor(x1, y1, x2, y2);
    }

    @Override
    public void popClip() {
        delegate.disableScissor();
    }

    @Override
    public void pushOpacity(float value) {
        opacityStack.push(opacity);
        opacity *= Math.max(0f, Math.min(1f, value));
    }

    @Override
    public void popOpacity() {
        opacity = opacityStack.isEmpty() ? 1f : opacityStack.pop();
    }

    private int applyOpacity(int argb) {
        if (opacity >= 1f) return argb;
        int a = Math.round((argb >>> 24) * opacity);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    // ---------- Скруглённые фигуры / Rounded shapes ----------

    @Override
    public void fillRounded(float x1, float y1, float x2, float y2, float radius, int argb) {
        fillRoundedGradient(x1, y1, x2, y2, radius, argb, argb, argb, argb);
    }

    @Override
    public void fillRoundedGradient(float x1, float y1, float x2, float y2, float radius,
                                    int topLeft, int topRight, int bottomLeft, int bottomRight) {
        int a = applyOpacity(topLeft);
        int b = applyOpacity(topRight);
        int c = applyOpacity(bottomLeft);
        int d = applyOpacity(bottomRight);
        if (((a | b | c | d) >>> 24) == 0 || x2 <= x1 || y2 <= y1) return;

        // Без скругления и градиента обычная заливка быстрее / Without rounding and a gradient a plain fill is faster
        if (radius <= 0f && a == b && a == c && a == d) {
            delegate.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), a);
            return;
        }

        setGradient(x1, y1, x2, y2, a, b, c, d);
        float r = clampRadius(radius, x1, y1, x2, y2);
        int seg = segments(r);
        float fw = feather();
        // Ядро чуть внутри границы и кромка чуть снаружи: между ними сглаживание / The core slightly inside the bounds and the edge slightly outside: smoothing between them
        int n = buildLoop(loopX[0], loopY[0], x1, y1, x2, y2, r, fw, seg);
        buildLoop(loopX[1], loopY[1], x1, y1, x2, y2, r, -fw, seg);

        begin3d();
        Matrix4f m = delegate.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();

        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        vertex(bb, m, (x1 + x2) * 0.5f, (y1 + y2) * 0.5f, 1f);
        for (int i = 0; i < n; i++) vertex(bb, m, loopX[0][i], loopY[0][i], 1f);
        vertex(bb, m, loopX[0][0], loopY[0][0], 1f);
        BufferUploader.drawWithShader(bb.end());

        strip(bb, m, 0, 1, n, 1f, 0f);
        end3d();
    }

    @Override
    public void strokeRounded(float x1, float y1, float x2, float y2, float radius, float thickness, int argb) {
        int c = applyOpacity(argb);
        if ((c >>> 24) == 0 || thickness <= 0f || x2 <= x1 || y2 <= y1) return;

        setGradient(x1, y1, x2, y2, c, c, c, c);
        float r = clampRadius(radius, x1, y1, x2, y2);
        int seg = segments(r);
        float fw = feather();
        float t = Math.max(thickness, 2f * fw);
        t = Math.min(t, Math.max(2f * fw, Math.min(x2 - x1, y2 - y1) / 2f - fw));

        // Четыре контура: кромка снаружи, ядро снаружи, ядро внутри, кромка внутри / Four outlines: outer edge, outer core, inner core, inner edge
        int n = buildLoop(loopX[0], loopY[0], x1, y1, x2, y2, r, -fw, seg);
        buildLoop(loopX[1], loopY[1], x1, y1, x2, y2, r, fw, seg);
        buildLoop(loopX[2], loopY[2], x1, y1, x2, y2, r, t - fw, seg);
        buildLoop(loopX[3], loopY[3], x1, y1, x2, y2, r, t + fw, seg);

        begin3d();
        Matrix4f m = delegate.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        strip(bb, m, 0, 1, n, 0f, 1f);
        strip(bb, m, 1, 2, n, 1f, 1f);
        strip(bb, m, 2, 3, n, 1f, 0f);
        end3d();
    }

    // Половина ширины сглаживания в GUI-пикселях: около одного физического пикселя / Half the smoothing width in GUI pixels: about one physical pixel
    private float feather() {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        return (float) Math.max(0.2, Math.min(0.6, 0.7 / scale));
    }

    private static float clampRadius(float radius, float x1, float y1, float x2, float y2) {
        return Math.max(0f, Math.min(radius, Math.min(x2 - x1, y2 - y1) / 2f));
    }

    private static int segments(float r) {
        if (r <= 0f) return 1;
        return Math.max(3, Math.min(MAX_SEGMENTS, (int) Math.ceil(r * 1.3f)));
    }

    // Контур скруглённого прямоугольника по часовой стрелке. inset > 0 сдвигает внутрь, < 0 наружу.
    // Все контуры одной фигуры имеют одинаковое число точек, поэтому их можно соединять полосами
    // An outline of a rounded rectangle, clockwise. inset > 0 moves it inward, < 0 outward.
    // All outlines of one shape have the same point count, so they can be joined by strips
    private static int buildLoop(float[] xs, float[] ys, float x1, float y1, float x2, float y2,
                                 float radius, float inset, int seg) {
        float l = x1 + inset;
        float t = y1 + inset;
        float r = x2 - inset;
        float b = y2 - inset;
        float rad = Math.max(0f, radius - inset);
        int n = 0;
        for (int corner = 0; corner < 4; corner++) {
            // Углы: левый верхний, правый верхний, правый нижний, левый нижний / Corners: top-left, top-right, bottom-right, bottom-left
            float cx = (corner == 0 || corner == 3) ? l + rad : r - rad;
            float cy = corner < 2 ? t + rad : b - rad;
            double start = Math.toRadians(180.0 + 90.0 * corner);
            for (int i = 0; i <= seg; i++) {
                double angle = start + (Math.PI / 2.0) * i / seg;
                xs[n] = (float) (cx + rad * Math.cos(angle));
                ys[n] = (float) (cy + rad * Math.sin(angle));
                n++;
            }
        }
        return n;
    }

    private void setGradient(float x1, float y1, float x2, float y2, int tl, int tr, int bl, int br) {
        gx1 = x1;
        gy1 = y1;
        gx2 = x2;
        gy2 = y2;
        gTl = tl;
        gTr = tr;
        gBl = bl;
        gBr = br;
    }

    private int gradientAt(float x, float y) {
        float u = gx2 > gx1 ? Math.max(0f, Math.min(1f, (x - gx1) / (gx2 - gx1))) : 0f;
        float v = gy2 > gy1 ? Math.max(0f, Math.min(1f, (y - gy1) / (gy2 - gy1))) : 0f;
        return lerpColor(lerpColor(gTl, gTr, u), lerpColor(gBl, gBr, u), v);
    }

    private static int lerpColor(int a, int b, float t) {
        int result = 0;
        for (int shift = 24; shift >= 0; shift -= 8) {
            int ca = (a >>> shift) & 0xFF;
            int cb = (b >>> shift) & 0xFF;
            result |= (Math.round(ca + (cb - ca) * t) & 0xFF) << shift;
        }
        return result;
    }

    // Полоса между двумя контурами; alpha* масштабирует прозрачность на каждом из них / A strip between two outlines; alpha* scales the opacity on each of them
    private void strip(BufferBuilder bb, Matrix4f m, int la, int lb, int n, float alphaA, float alphaB) {
        bb.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int k = 0; k <= n; k++) {
            int i = k % n;
            vertex(bb, m, loopX[la][i], loopY[la][i], alphaA);
            vertex(bb, m, loopX[lb][i], loopY[lb][i], alphaB);
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private void vertex(BufferBuilder bb, Matrix4f m, float x, float y, float alphaScale) {
        int c = gradientAt(x, y);
        int a = Math.round(((c >>> 24) & 0xFF) * alphaScale);
        bb.vertex(m, x, y, 0f).color((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, a).endVertex();
    }

    @Override
    public void drawPlayerPreview(int x1, int y1, int x2, int y2, int scale, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        delegate.flush(); // сначала дорисовать всё накопленное / first finish everything pending
        int cx = (x1 + x2) / 2;
        int feetY = y2 - 6;
        // Как в ванильном инвентаре: аргументы мыши заданы относительно точки модели
        // As in the vanilla inventory: the mouse arguments are relative to the model's point
        InventoryScreen.renderEntityInInventoryFollowsMouse(delegate, cx, feetY, scale,
                (float) (cx - mouseX), (float) (feetY - 50 - mouseY), mc.player);
    }

    @Override
    public void drawMapPreview(UiItem map, int x, int y, int size) {
        if (!(map instanceof McItem m) || m.stack().isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Integer id = MapItem.getMapId(m.stack());
        if (id == null || mc.level == null) return;
        MapItemSavedData data = MapItem.getSavedData(id, mc.level);
        if (data == null) return;
        delegate.flush();
        delegate.pose().pushPose();
        delegate.pose().translate((float) x, (float) y, 1.0F);
        float scale = size / 128f;
        delegate.pose().scale(scale, scale, 1.0F);
        mc.gameRenderer.getMapRenderer().render(delegate.pose(), delegate.bufferSource(), id, data, true, 15728880);
        delegate.flush();
        delegate.pose().popPose();
    }

    @Override
    public void drawImage(UiImage image, int x, int y, int width, int height) {
        if (opacity <= 0f) return;
        delegate.flush();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, opacity); // прозрачность действует на текстуры / opacity applies to textures
        if (image instanceof McSprite s) {
            delegate.blit(x, y, 0, width, height, s.sprite());
        } else if (image instanceof UiTexture t) {
            delegate.blit(Nova_core.parse(t.location()), x, y, width, height,
                    t.u(), t.v(), t.regionWidth(), t.regionHeight(), t.textureWidth(), t.textureHeight());
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    @Override
    public void drawItem(UiItem item, int x, int y, boolean decorations) {
        if (!(item instanceof McItem m) || m.stack().isEmpty()) return;
        delegate.renderItem(m.stack(), x, y);
        if (decorations) delegate.renderItemDecorations(Minecraft.getInstance().font, m.stack(), x, y);
    }

    @Override
    public void drawItemTooltip(UiItem item, int mouseX, int mouseY) {
        if (!(item instanceof McItem m) || m.stack().isEmpty()) return;
        delegate.renderTooltip(Minecraft.getInstance().font, m.stack(), mouseX, mouseY);
    }

    private void begin3d() {
        delegate.flush(); // сначала дорисовать всё накопленное, чтобы сохранить порядок слоёв / first finish everything pending to keep the layer order
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private void end3d() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}