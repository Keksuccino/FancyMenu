package de.keksuccino.fancymenu.util.rendering.gui;

public record ScreenPosition(int x, int y) {

    public static ScreenPosition of(ScreenAxis axis, int primaryPosition, int secondaryPosition) {
        return switch (axis) {
            case HORIZONTAL -> new ScreenPosition(primaryPosition, secondaryPosition);
            case VERTICAL -> new ScreenPosition(secondaryPosition, primaryPosition);
        };
    }

    public ScreenPosition step(ScreenDirection direction) {
        return switch (direction) {
            case DOWN -> new ScreenPosition(this.x, this.y + 1);
            case UP -> new ScreenPosition(this.x, this.y - 1);
            case LEFT -> new ScreenPosition(this.x - 1, this.y);
            case RIGHT -> new ScreenPosition(this.x + 1, this.y);
        };
    }

    public int getCoordinate(ScreenAxis axis) {
        return switch (axis) {
            case HORIZONTAL -> this.x;
            case VERTICAL -> this.y;
        };
    }

}
