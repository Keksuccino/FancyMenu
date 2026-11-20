package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.bats;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Where bats can hang: up to {@value #MAX_ROOSTS} spots under the bottom edges of the elements, for any number and placement of
 * elements.<br><br>
 *
 * The mockup has six fixed spots on its four buttons of 150 pixels (the index of a button and an offset from its left or right end).
 * Here the usable bottom edges of all elements, sorted top to bottom and left to right, get laid end to end as one line, and every spot
 * sits at a fixed fraction of that line. The fractions come from the mockup's spots, so its layout gets exactly its spots, and
 * other layouts get them spread over the elements by width. Spots that don't work out (another element right below, too close to
 * another spot) get skipped, so there can be fewer than six.
 */
final class BatRoosts {

    static final int MAX_ROOSTS = 6;
    /** Width and height of a hanging bat at size 1 */
    static final int BAT_WIDTH = 5;
    static final int BAT_HEIGHT = 7;
    /**
     * The mockup's spots [button, offset] {@code [1,28], [3,-34], [0,-52], [2,46], [3,18], [1,-24]} on four buttons of 150 pixels,
     * as fractions of the four bottom edges laid end to end. A negative offset counts from the right end.
     */
    static final float[] SPOTS = {
            (1 + 28.0F / 150.0F) / 4.0F,
            (3 + 116.0F / 150.0F) / 4.0F,
            (0 + 98.0F / 150.0F) / 4.0F,
            (2 + 46.0F / 150.0F) / 4.0F,
            (3 + 18.0F / 150.0F) / 4.0F,
            (1 + 126.0F / 150.0F) / 4.0F
    };

    private final List<PixelSurfaces.Surface<?>> usable = new ArrayList<>();
    private final PixelSurfaces.Surface<?>[] surfaces = new PixelSurfaces.Surface<?>[MAX_ROOSTS];
    private final int[] xs = new int[MAX_ROOSTS];
    private int count = 0;

    /**
     * Lays out the spots for the current elements, screen size and bat size.
     */
    void layout(@NotNull PixelSurfaces<?> all, int screenWidth, int screenHeight, int scale) {
        int batWidth = BAT_WIDTH * scale;
        // Bigger bats get drawn a bit to the left of the spot, like in the mockup, so that they stay about centered on it
        int drawOffset = (BAT_WIDTH * (scale - 1)) >> 1;
        this.collectUsable(all, screenWidth, screenHeight, scale);
        float total = 0.0F;
        for (int i = 0; i < this.usable.size(); i++) total += usableWidth(this.usable.get(i), screenWidth);
        this.count = 0;
        for (int k = 0; (k < MAX_ROOSTS) && (total > 0.0F); k++) {
            float position = SPOTS[k] * total;
            PixelSurfaces.Surface<?> surface = null;
            int segmentLeft = 0;
            int segmentRight = 0;
            for (int i = 0; i < this.usable.size(); i++) {
                PixelSurfaces.Surface<?> candidate = this.usable.get(i);
                int width = usableWidth(candidate, screenWidth);
                if ((position < width) || (i == this.usable.size() - 1)) {
                    surface = candidate;
                    segmentLeft = Math.max(0, candidate.x());
                    segmentRight = Math.min(screenWidth, candidate.right());
                    break;
                }
                position -= width;
            }
            if (surface == null) break;
            int x = segmentLeft + Math.round(position);
            // The whole bat hangs under the element
            x = Math.max(segmentLeft + drawOffset, Math.min(x, segmentRight - batWidth + drawOffset));
            if (all.findElementAt(x - drawOffset + (batWidth >> 1), surface.bottom()) != null) continue;
            if (this.isTooClose(surface, x, batWidth + 2)) continue;
            this.surfaces[this.count] = surface;
            this.xs[this.count] = x;
            this.count++;
        }
        for (int k = this.count; k < MAX_ROOSTS; k++) this.surfaces[k] = null;
    }

    /**
     * Collects the elements a bat can hang under, sorted top to bottom and left to right: wide enough for a bat, and with room for
     * the whole bat between their bottom edge and the bottom of the screen.
     */
    private void collectUsable(@NotNull PixelSurfaces<?> all, int screenWidth, int screenHeight, int scale) {
        this.usable.clear();
        List<? extends PixelSurfaces.Surface<?>> elements = all.elements();
        for (int i = 0; i < elements.size(); i++) {
            PixelSurfaces.Surface<?> element = elements.get(i);
            if (usableWidth(element, screenWidth) < BAT_WIDTH * scale) continue;
            if ((element.bottom() < 0) || (element.bottom() + BAT_HEIGHT * scale > screenHeight)) continue;
            int at = this.usable.size();
            while ((at > 0) && isAfter(this.usable.get(at - 1), element)) at--;
            this.usable.add(at, element);
        }
    }

    private static boolean isAfter(@NotNull PixelSurfaces.Surface<?> a, @NotNull PixelSurfaces.Surface<?> b) {
        return (a.y() != b.y()) ? (a.y() > b.y()) : (a.x() > b.x());
    }

    /**
     * The part of an element's width that is on the screen.
     */
    private static int usableWidth(@NotNull PixelSurfaces.Surface<?> element, int screenWidth) {
        return Math.min(screenWidth, element.right()) - Math.max(0, element.x());
    }

    private boolean isTooClose(@NotNull PixelSurfaces.Surface<?> surface, int x, int minDistance) {
        for (int k = 0; k < this.count; k++) {
            if ((this.surfaces[k] == surface) && (Math.abs(this.xs[k] - x) < minDistance)) return true;
        }
        return false;
    }

    /**
     * Number of spots, at most {@value #MAX_ROOSTS}.
     */
    int count() {
        return this.count;
    }

    /**
     * The element of a spot, NULL for indices from {@link #count()} on.
     */
    @Nullable
    PixelSurfaces.Surface<?> surface(int index) {
        return this.surfaces[index];
    }

    /**
     * The column of a spot: the left end of a bat of size 1 hanging there.
     */
    int x(int index) {
        return this.xs[index];
    }

}
