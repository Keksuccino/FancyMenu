package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.SurfaceHeadroom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * The litter heights on one surface: how many settled pieces lie on top of each other in every GUI column. Elements hold up to
 * {@value #ELEMENT_MAX_LEVELS} layers and the floor up to {@value #FLOOR_MAX_LEVELS}, so the litter always stays a thin layer.
 * Litter never grows into an element right above the surface, see {@link #fitUnder}.
 */
final class ConfettiSurface {

    static final int ELEMENT_MAX_LEVELS = 3;
    static final int FLOOR_MAX_LEVELS = 5;

    final int x;
    final int y;
    final int width;
    final boolean floor;
    final int maxLevels;
    final int[] levels;
    /** Free GUI pixel rows above every column, up to the lowest element above it */
    final int[] freeRows;
    /** The surfaces version this surface was last seen in, to drop the litter of surfaces that are gone */
    int seenInVersion = -1;

    ConfettiSurface(@NotNull PixelSurfaces.Surface<?> surface) {
        this.x = surface.x();
        this.y = surface.y();
        this.width = surface.width();
        this.floor = surface.isFloor();
        this.maxLevels = this.floor ? FLOOR_MAX_LEVELS : ELEMENT_MAX_LEVELS;
        this.levels = new int[Math.max(0, this.width)];
        this.freeRows = new int[this.levels.length];
        Arrays.fill(this.freeRows, Math.max(0, this.y));
    }

    /**
     * Measures the room above every column, so litter stays out of the elements above. Call it whenever the elements of the screen changed.
     *
     * @param surface the surface this state belongs to
     * @param elements all element surfaces of the screen
     */
    void fitUnder(@NotNull PixelSurfaces.Surface<?> surface, @NotNull List<? extends PixelSurfaces.Surface<?>> elements) {
        for (int i = 0; i < this.freeRows.length; i++) this.freeRows[i] = SurfaceHeadroom.freeRowsAbove(surface, this.x + i, 1, elements);
    }

    /**
     * How many layers of litter fit over the columns {@code from} (included) to {@code from + count} (excluded) when every layer is
     * {@code scale} GUI pixels thick.
     */
    int levelLimit(int from, int count, int scale) {
        int free = Integer.MAX_VALUE;
        for (int i = 0; i < count; i++) free = Math.min(free, this.freeRows[from - this.x + i]);
        return Math.min(this.maxLevels, free / scale);
    }

    boolean spans(int column) {
        return (column >= this.x) && (column < this.x + this.width);
    }

    /**
     * The highest layer of litter over the columns {@code from} (included) to {@code from + count} (excluded), 0 for none.
     */
    int highestLevel(int from, int count) {
        int highest = 0;
        for (int i = 0; i < count; i++) highest = Math.max(highest, this.levels[from - this.x + i]);
        return highest;
    }

    void clear() {
        Arrays.fill(this.levels, 0);
    }

}
