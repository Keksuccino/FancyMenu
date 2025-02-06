package de.keksuccino.fancymenu.util.rendering.gui;

import it.unimi.dsi.fastutil.ints.IntComparator;

public enum ScreenDirection {

    UP,
    DOWN,
    LEFT,
    RIGHT;

    private final IntComparator coordinateValueComparator = (ix, j) -> ix == j ? 0 : (this.isBefore(ix, j) ? -1 : 1);

    public ScreenAxis getAxis() {
        return switch (this) {
            case UP, DOWN -> ScreenAxis.VERTICAL;
            case LEFT, RIGHT -> ScreenAxis.HORIZONTAL;
        };
    }

    public ScreenDirection getOpposite() {
        return switch (this) {
            case UP -> DOWN;
            case DOWN -> UP;
            case LEFT -> RIGHT;
            case RIGHT -> LEFT;
        };
    }

    public boolean isPositive() {
        return switch (this) {
            case UP, LEFT -> false;
            case DOWN, RIGHT -> true;
        };
    }

    public boolean isAfter(int first, int second) {
        return this.isPositive() ? first > second : second > first;
    }

    public boolean isBefore(int first, int second) {
        return this.isPositive() ? first < second : second < first;
    }

    public IntComparator coordinateValueComparator() {
        return this.coordinateValueComparator;
    }

}
