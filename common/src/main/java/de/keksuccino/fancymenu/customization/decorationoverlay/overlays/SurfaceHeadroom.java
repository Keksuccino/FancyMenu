package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * How much room there is above and below a surface, for overlays that pile things up on surfaces (snow caps, confetti litter) or hang
 * things under elements (icicles, drips). They must not grow into an element right above or below, like the next button of a tight
 * button column, so they only get as big as the gap to that element allows.
 */
public final class SurfaceHeadroom {

    private SurfaceHeadroom() {
    }

    /**
     * The free GUI pixel rows above the columns {@code x} (included) to {@code x + width} (excluded) of a surface: the distance from the
     * surface's top to the bottom of the lowest element above it that overlaps these columns, or to the top of the screen.
     * Elements that overlap the top of the surface itself don't count.
     *
     * @param elements the element surfaces of the screen; the surface itself may be part of it
     */
    public static int freeRowsAbove(@NotNull PixelSurfaces.Surface<?> surface, int x, int width, @NotNull List<? extends PixelSurfaces.Surface<?>> elements) {
        int top = surface.y();
        int free = Math.max(0, top);
        for (int i = 0; i < elements.size(); i++) {
            PixelSurfaces.Surface<?> other = elements.get(i);
            if ((other == surface) || (other.bottom() > top)) continue;
            if ((other.x() >= x + width) || (other.right() <= x)) continue;
            free = Math.min(free, top - other.bottom());
        }
        return free;
    }

    /**
     * The free GUI pixel rows below the columns {@code x} (included) to {@code x + width} (excluded) of an element, for things hanging
     * from it (icicles, drips): the distance from the element's bottom to the top of the highest element below it that overlaps these
     * columns, {@link Integer#MAX_VALUE} if there is none. Elements that overlap the bottom of the element itself don't count.
     *
     * @param elements the element surfaces of the screen; the surface itself may be part of it
     */
    public static int freeRowsBelow(@NotNull PixelSurfaces.Surface<?> surface, int x, int width, @NotNull List<? extends PixelSurfaces.Surface<?>> elements) {
        int bottom = surface.bottom();
        int free = Integer.MAX_VALUE;
        for (int i = 0; i < elements.size(); i++) {
            PixelSurfaces.Surface<?> other = elements.get(i);
            if ((other == surface) || (other.y() < bottom)) continue;
            if ((other.x() >= x + width) || (other.right() <= x)) continue;
            free = Math.min(free, other.y() - bottom);
        }
        return free;
    }

}
