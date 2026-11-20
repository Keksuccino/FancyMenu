package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * The surfaces things can land on or grow on, for overlays that decorate elements: every element box plus the bottom edge of the
 * screen as a floor (the mockup's {@code SURF}, {@code BOTTOM} and {@code ALLS}).<br><br>
 *
 * Each surface can carry a state object of the overlay, like a snow cap or a moss cushion. It gets created by the state factory when
 * a surface appears and stays with the surface as long as its bounds stay the same, so per-surface state survives screen ticks.
 * {@link #version()} changes whenever surfaces got added or removed.
 *
 * @param <S> type of the per-surface state, {@link Void} for none
 */
public final class PixelSurfaces<S> {

    @Nullable
    private final Function<Surface<S>, S> stateFactory;
    private final List<Surface<S>> all = new ArrayList<>();
    private final List<Surface<S>> elements = new ArrayList<>();
    private final List<Surface<S>> allView = Collections.unmodifiableList(this.all);
    private final List<Surface<S>> elementsView = Collections.unmodifiableList(this.elements);
    @NotNull
    private Surface<S> floor = new Surface<>(0, 0, 0, 0, true);
    @Nullable
    private List<CollisionAreaBounds> lastBoxes = null;
    private int screenWidth = -1;
    private int screenHeight = -1;
    private int totalWidth = 0;
    private int version = 0;

    public PixelSurfaces() {
        this(null);
    }

    /**
     * @param stateFactory creates the state of a new surface, NULL for surfaces without state
     */
    public PixelSurfaces(@Nullable Function<Surface<S>, S> stateFactory) {
        this.stateFactory = stateFactory;
        this.all.add(this.floor);
    }

    /**
     * Syncs the surfaces with the element boxes and the screen size. Surfaces whose bounds didn't change keep their state.
     * Cheap when nothing changed, so it can run every frame.
     *
     * @return true if surfaces got added or removed
     */
    public boolean update(@NotNull List<CollisionAreaBounds> boxes, int screenWidth, int screenHeight) {
        boolean sizeChanged = (screenWidth != this.screenWidth) || (screenHeight != this.screenHeight);
        if (!sizeChanged && (boxes == this.lastBoxes)) return false;
        this.lastBoxes = boxes;
        if (!sizeChanged && this.hasSameBounds(boxes)) return false;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        List<Surface<S>> previous = new ArrayList<>(this.elements);
        this.elements.clear();
        for (CollisionAreaBounds box : boxes) {
            if ((box.width() <= 0) || (box.height() < 0)) continue;
            Surface<S> surface = takeMatching(previous, box);
            if (surface == null) surface = this.createSurface(box.x(), box.y(), box.width(), box.height(), false);
            this.elements.add(surface);
        }
        if (sizeChanged) this.floor = this.createSurface(0, screenHeight, screenWidth, 0, true);
        this.all.clear();
        this.all.addAll(this.elements);
        this.all.add(this.floor);
        this.totalWidth = 0;
        for (Surface<S> surface : this.all) this.totalWidth += surface.width;
        this.version++;
        return true;
    }

    private boolean hasSameBounds(@NotNull List<CollisionAreaBounds> boxes) {
        int index = 0;
        for (CollisionAreaBounds box : boxes) {
            if ((box.width() <= 0) || (box.height() < 0)) continue;
            if (index >= this.elements.size()) return false;
            Surface<S> surface = this.elements.get(index++);
            if (!surface.hasBounds(box)) return false;
        }
        return index == this.elements.size();
    }

    @Nullable
    private static <S> Surface<S> takeMatching(@NotNull List<Surface<S>> surfaces, @NotNull CollisionAreaBounds box) {
        for (int i = 0; i < surfaces.size(); i++) {
            if (surfaces.get(i).hasBounds(box)) return surfaces.remove(i);
        }
        return null;
    }

    @NotNull
    private Surface<S> createSurface(int x, int y, int width, int height, boolean floor) {
        Surface<S> surface = new Surface<>(x, y, width, height, floor);
        if (this.stateFactory != null) surface.state = this.stateFactory.apply(surface);
        return surface;
    }

    /**
     * All surfaces: the element boxes first, the floor last.
     */
    @NotNull
    public List<Surface<S>> all() {
        return this.allView;
    }

    /**
     * Only the element boxes.
     */
    @NotNull
    public List<Surface<S>> elements() {
        return this.elementsView;
    }

    /**
     * The bottom edge of the screen: as wide as the screen, 0 pixels high, with its top at the screen height.
     */
    @NotNull
    public Surface<S> floor() {
        return this.floor;
    }

    /**
     * Sum of the widths of all surfaces, floor included.
     */
    public int totalWidth() {
        return this.totalWidth;
    }

    /**
     * Changes whenever surfaces got added or removed.
     */
    public int version() {
        return this.version;
    }

    /**
     * Picks a random surface, wider ones more often (the mockup's {@code pickSurf}). The floor counts with the screen width.
     */
    @NotNull
    public Surface<S> pick(@NotNull PixelRandom random) {
        float r = random.next() * this.totalWidth;
        if (r < this.floor.width) return this.floor;
        r -= this.floor.width;
        for (Surface<S> surface : this.elements) {
            if (r < surface.width) return surface;
            r -= surface.width;
        }
        return this.floor;
    }

    /**
     * Returns the first surface that something falling at column {@code x} reached while its bottom moved from {@code fromY}
     * to {@code toY}: the surface spans the column and its {@link Surface#restY() rest row} lies in {@code (fromY, toY]}.
     * Returns NULL if it reached none.
     */
    @Nullable
    public Surface<S> findLanding(float x, float fromY, float toY) {
        for (Surface<S> surface : this.all) {
            if (!surface.spans(x)) continue;
            int rest = surface.restY();
            if ((fromY < rest) && (toY >= rest)) return surface;
        }
        return null;
    }

    /**
     * Returns the element box that contains the point, or NULL.
     */
    @Nullable
    public Surface<S> findElementAt(float x, float y) {
        for (Surface<S> surface : this.elements) {
            if (surface.contains(x, y)) return surface;
        }
        return null;
    }

    public static final class Surface<S> {

        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final boolean floor;
        @Nullable
        private S state;

        private Surface(int x, int y, int width, int height, boolean floor) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.floor = floor;
        }

        public int x() {
            return this.x;
        }

        public int y() {
            return this.y;
        }

        public int width() {
            return this.width;
        }

        public int height() {
            return this.height;
        }

        /**
         * First column right of the surface.
         */
        public int right() {
            return this.x + this.width;
        }

        /**
         * First row below the surface.
         */
        public int bottom() {
            return this.y + this.height;
        }

        public boolean isFloor() {
            return this.floor;
        }

        /**
         * The row something resting on top of the surface sits on: the row above an element, the last row of the screen for the floor
         * (the mockup's {@code top}).
         */
        public int restY() {
            return this.y - 1;
        }

        /**
         * Whether the column lies over the surface.
         */
        public boolean spans(float x) {
            return (x >= this.x) && (x < this.x + this.width);
        }

        public boolean contains(float x, float y) {
            return this.spans(x) && (y >= this.y) && (y < this.y + this.height);
        }

        @Nullable
        public S state() {
            return this.state;
        }

        public void setState(@Nullable S state) {
            this.state = state;
        }

        private boolean hasBounds(@NotNull CollisionAreaBounds box) {
            return (this.x == box.x()) && (this.y == box.y()) && (this.width == box.width()) && (this.height == box.height());
        }

    }

}
