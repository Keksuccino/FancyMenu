package de.keksuccino.fancymenu.util.rendering.gui;

public enum ScreenAxis {

    HORIZONTAL,
    VERTICAL;

    public ScreenAxis orthogonal() {
        return switch (this) {
            case HORIZONTAL -> VERTICAL;
            case VERTICAL -> HORIZONTAL;
        };
    }

    public ScreenDirection getPositive() {
        return switch (this) {
            case HORIZONTAL -> ScreenDirection.RIGHT;
            case VERTICAL -> ScreenDirection.DOWN;
        };
    }

    public ScreenDirection getNegative() {
        return switch (this) {
            case HORIZONTAL -> ScreenDirection.LEFT;
            case VERTICAL -> ScreenDirection.UP;
        };
    }

    public ScreenDirection getDirection(boolean isPositive) {
        return isPositive ? this.getPositive() : this.getNegative();
    }

}
