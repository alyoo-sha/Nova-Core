package org.mod.nova_core.ui.layout;

// Как виджет получает размер по оси / How a widget gets its size on an axis
public enum SizeMode {
    FIXED, // заданный через setSize / the size set via setSize
    WRAP,  // по содержимому / fit the content
    FILL   // всё доступное место / all available space
}