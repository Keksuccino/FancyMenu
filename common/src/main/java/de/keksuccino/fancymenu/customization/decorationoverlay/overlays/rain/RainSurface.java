package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.SurfaceHeadroom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * What the Rain overlay keeps per surface: the glints of the thin wet film on top of it, and the drips that bead up under an element.
 * Drips never reach into an element right below, see {@link #fitDrips}.
 * The mockup stage has 12 glints along the floor (480 pixels) and 4 on every element (150 pixels); other widths get as many glints per pixel.
 */
final class RainSurface {

    private static final float FLOOR_GLINTS_PER_PIXEL = 12.0F / 480.0F;
    private static final float ELEMENT_GLINTS_PER_PIXEL = 4.0F / 150.0F;
    /** Elements narrower than this only get the two drips at their ends */
    private static final int MIN_WIDTH_FOR_MIDDLE_DRIP = 44;
    static final int DRIP_LEFT = -1;
    static final int DRIP_MIDDLE = 0;
    static final int DRIP_RIGHT = 1;

    final int x;
    final int y;
    final int width;
    final int bottom;
    final boolean floor;
    final int[] glintX;
    final float[] glintPhase;
    final float[] glintSpeed;
    final Drip[] drips;

    RainSurface(@NotNull PixelSurfaces.Surface<?> surface, @NotNull PixelRandom random) {
        this.x = surface.x();
        this.y = surface.y();
        this.width = surface.width();
        this.bottom = surface.bottom();
        this.floor = surface.isFloor();
        int glints = Math.max(1, Math.round(this.width * (this.floor ? FLOOR_GLINTS_PER_PIXEL : ELEMENT_GLINTS_PER_PIXEL)));
        this.glintX = new int[glints];
        this.glintPhase = new float[glints];
        this.glintSpeed = new float[glints];
        for (int i = 0; i < glints; i++) {
            this.glintX[i] = this.x + random.rangeInt(2, Math.max(2, this.width - 3));
            this.glintPhase[i] = random.range(0.0F, PixelMath.TAU);
            this.glintSpeed[i] = random.range(1.2F, 3.0F);
        }
        if (this.floor) {
            this.drips = new Drip[0];
        } else if (this.width < 4) {
            this.drips = new Drip[] {new Drip(DRIP_MIDDLE, this.width / 2, random)};
        } else if (this.width < MIN_WIDTH_FOR_MIDDLE_DRIP) {
            this.drips = new Drip[] {new Drip(DRIP_LEFT, 0, random), new Drip(DRIP_RIGHT, 0, random)};
        } else {
            this.drips = new Drip[] {new Drip(DRIP_LEFT, 0, random), new Drip(DRIP_MIDDLE, random.rangeInt(20, this.width - 20), random), new Drip(DRIP_RIGHT, 0, random)};
        }
    }

    /**
     * Measures the room under every drip, so a drip never beads up into an element right below. Call it whenever the elements of the screen
     * or the size changed.
     *
     * @param surface the surface this state belongs to
     * @param elements all element surfaces of the screen
     */
    void fitDrips(@NotNull PixelSurfaces.Surface<?> surface, @NotNull List<? extends PixelSurfaces.Surface<?>> elements, int scale) {
        for (Drip drip : this.drips) drip.room = SurfaceHeadroom.freeRowsBelow(surface, this.dripX(drip, scale), scale, elements);
    }

    /**
     * Whether the column lies over the surface.
     */
    boolean spans(float column) {
        return (column >= this.x) && (column < this.x + this.width);
    }

    /**
     * The GUI x of a drip: one art pixel inside the ends of the element, or its own spot in the middle.
     */
    int dripX(@NotNull Drip drip, int scale) {
        return switch (drip.edge) {
            case DRIP_LEFT -> this.x + scale;
            case DRIP_RIGHT -> this.x + this.width - 2 * scale;
            default -> this.x + drip.offset;
        };
    }

    /**
     * A drip under an element. It beads up through three stages, lets go, falls and splashes on whatever is below, then starts over.
     */
    static final class Drip {

        final int edge;
        final int offset;
        /** Progress of the beading, 0 to 1. Below 0 the drip waits before it starts to bead up again. */
        float progress;
        /** Seconds the beading takes */
        float period;
        boolean falling = false;
        /** Free GUI pixel rows under the drip, up to the next element below */
        int room = Integer.MAX_VALUE;
        float y = 0.0F;
        float vy = 0.0F;

        Drip(int edge, int offset, @NotNull PixelRandom random) {
            this.edge = edge;
            this.offset = offset;
            this.progress = random.range(0.0F, 1.0F);
            this.period = random.range(0.9F, 2.4F);
        }

        /**
         * The beading stage, 1 to 3 art pixels long but never longer than the room below allows, or 0 while the drip waits.
         */
        int stage(int scale) {
            if (this.progress <= 0.0F) return 0;
            int stage = (this.progress < 0.4F) ? 1 : ((this.progress < 0.75F) ? 2 : 3);
            return Math.min(stage, this.maxStage(scale));
        }

        /**
         * How many art pixels the drip can hang down before it reaches an element below. A drip without any room never forms.
         */
        int maxStage(int scale) {
            return Math.min(3, this.room / scale);
        }

        /**
         * Where the drip starts to fall: two art pixels under the element like the mockup, or higher in a tight gap, so it still lands on
         * the element below instead of falling through it.
         */
        int fallStartOffset(int scale) {
            if (this.room == Integer.MAX_VALUE) return 2 * scale;
            return Math.min(2 * scale, Math.max(0, this.room - 2 * scale));
        }

    }

}
