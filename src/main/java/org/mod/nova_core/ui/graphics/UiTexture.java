package org.mod.nova_core.ui.graphics;

// Область текстуры по имени ресурса вида "пространство:путь" / A texture region by resource name like "namespace:path"
public record UiTexture(String location, int u, int v, int regionWidth, int regionHeight,
                        int textureWidth, int textureHeight) implements UiImage {
    // Текстура целиком / The whole texture
    public static UiTexture whole(String location, int width, int height) {
        return new UiTexture(location, 0, 0, width, height, width, height);
    }
}