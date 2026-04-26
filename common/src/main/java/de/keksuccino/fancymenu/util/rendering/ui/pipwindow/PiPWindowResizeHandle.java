package de.keksuccino.fancymenu.util.rendering.ui.pipwindow;

public enum PiPWindowResizeHandle {
    NONE,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    public boolean hasLeftEdge() {
        return this == LEFT || this == TOP_LEFT || this == BOTTOM_LEFT;
    }

    public boolean hasRightEdge() {
        return this == RIGHT || this == TOP_RIGHT || this == BOTTOM_RIGHT;
    }

    public boolean hasTopEdge() {
        return this == TOP || this == TOP_LEFT || this == TOP_RIGHT;
    }

    public boolean hasBottomEdge() {
        return this == BOTTOM || this == BOTTOM_LEFT || this == BOTTOM_RIGHT;
    }
}
