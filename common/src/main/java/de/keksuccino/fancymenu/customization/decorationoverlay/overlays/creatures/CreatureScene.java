package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPointer;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * What the simulations of the creature overlays see of their overlay in the current frame: screen size, time, pointer, surfaces
 * and the Amount and Size options. The overlay updates it right before its simulation, tests set it up by hand.<br><br>
 *
 * <b>Adapting the mockup to any screen</b><br>
 * The mockup runs on a stage of {@value #STAGE_WIDTH}x{@value #STAGE_HEIGHT} GUI pixels. Distances, speeds and sprite sizes stay in GUI
 * pixels, so the art keeps its size. Heights the mockup gives as fixed rows of its stage (like "birds fly in between row 10 and 110")
 * get scaled to the screen with {@link #stageY(float)}. How many things are around at once follows the screen area
 * ({@link #density()}), and how often groups come along (bat groups, lantern releases) follows its square root ({@link #eventRate()}),
 * so a bigger screen looks as busy as the mockup stage.
 */
public final class CreatureScene {

    public static final int STAGE_WIDTH = 480;
    public static final int STAGE_HEIGHT = 270;
    /** The time step the mockup simulates with, see {@link #jitter(float, float)}. */
    public static final float MOCKUP_STEP = 1.0F / 60.0F;
    static final float MIN_DENSITY = 0.25F;
    /** Keeps the counts of huge GUI screens (GUI scale 1 on big monitors) from exploding */
    static final float MAX_DENSITY = 4.0F;

    @NotNull
    private final PixelRandom random;
    @NotNull
    private final PixelPointer pointer;
    @NotNull
    private final PixelSurfaces<?> surfaces;
    private int width = 1;
    private int height = 1;
    private float time = 0.0F;
    private float amount = 1.0F;
    private int scale = 1;
    private float density = MIN_DENSITY;
    private float eventRate = (float)Math.sqrt(MIN_DENSITY);
    private int surfacesVersion = Integer.MIN_VALUE;
    private boolean surfacesChanged = false;

    public CreatureScene(@NotNull PixelRandom random, @NotNull PixelPointer pointer, @NotNull PixelSurfaces<?> surfaces) {
        this.random = random;
        this.pointer = pointer;
        this.surfaces = surfaces;
    }

    /**
     * Sets the state of the new frame. The surfaces have to be synced with the elements before.
     *
     * @param amount the Amount option as a factor, 1 for overlays without one
     * @param scale the Size option, 1 for overlays without one
     */
    public void update(int width, int height, float time, float amount, int scale) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.time = time;
        this.amount = amount;
        this.scale = Math.max(1, scale);
        this.density = densityOf(this.width, this.height);
        this.eventRate = (float)Math.sqrt(this.density);
        int version = this.surfaces.version();
        this.surfacesChanged = (version != this.surfacesVersion);
        this.surfacesVersion = version;
    }

    /**
     * How much busier a screen of this size should be than the mockup stage, from {@value #MIN_DENSITY} to {@value #MAX_DENSITY}:
     * its area divided by the area of the stage.
     */
    public static float densityOf(int width, int height) {
        return PixelMath.clamp((float)width * height / (STAGE_WIDTH * STAGE_HEIGHT), MIN_DENSITY, MAX_DENSITY);
    }

    @NotNull
    public PixelRandom random() {
        return this.random;
    }

    @NotNull
    public PixelPointer pointer() {
        return this.pointer;
    }

    /**
     * The element boxes and the floor. Use {@link #surfacesChanged()} to find out whether they changed since the last frame.
     */
    @NotNull
    public PixelSurfaces<?> surfaces() {
        return this.surfaces;
    }

    @NotNull
    public List<? extends PixelSurfaces.Surface<?>> elements() {
        return this.surfaces.elements();
    }

    /**
     * Whether surfaces got added, removed or resized since the last frame, which includes every change of the screen size.
     * True in the first frame.
     */
    public boolean surfacesChanged() {
        return this.surfacesChanged;
    }

    /**
     * Whether the surface is still one of the current surfaces. Surfaces that moved or got resized count as gone.
     */
    public boolean isSurfaceLive(@Nullable PixelSurfaces.Surface<?> surface) {
        if (surface == null) return false;
        List<? extends PixelSurfaces.Surface<?>> all = this.surfaces.all();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i) == surface) return true;
        }
        return false;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    /**
     * Seconds the overlay has been running.
     */
    public float time() {
        return this.time;
    }

    /**
     * The Amount option as a factor from 0.25 to 3.0.
     */
    public float amount() {
        return this.amount;
    }

    /**
     * The Size option, 1 to 3: every art pixel is a square of this many GUI pixels.
     */
    public int scale() {
        return this.scale;
    }

    /**
     * Factor for the number of things that are around at once, see {@link #densityOf(int, int)}.
     */
    public float density() {
        return this.density;
    }

    /**
     * Factor for how often groups come along: the square root of {@link #density()}.
     */
    public float eventRate() {
        return this.eventRate;
    }

    /**
     * Converts a row of the mockup stage to a row of this screen.
     */
    public float stageY(float stageRow) {
        return stageRow * this.height / STAGE_HEIGHT;
    }

    /**
     * A random value from {@code min} to {@code max}, or the middle of both when the range is empty (tiny screens).
     */
    public float range(float min, float max) {
        return (max > min) ? this.random.range(min, max) : (min + max) * 0.5F;
    }

    /**
     * The random part of a wandering motion, the mockup's {@code rnd(-1, 1) * rate * dt}. The mockup always steps by
     * {@value #MOCKUP_STEP} seconds. Random steps that just scale with the frame time would wander less at higher frame rates, so this
     * keeps the variance per second of the mockup at any frame time: {@code rnd(-1, 1) * rate * sqrt(dt * MOCKUP_STEP)}.
     */
    public float jitter(float rate, float deltaSeconds) {
        return this.random.range(-1.0F, 1.0F) * rate * (float)Math.sqrt(deltaSeconds * MOCKUP_STEP);
    }

    /**
     * Picks a random spot on top of a surface for a resting creature: a surface weighted by its width (the mockup's {@code pickSurf},
     * floor included) and a column from {@code minInset} to {@code width - maxInset} of it, rounded like the mockup does.<br>
     * Returns false if the spot is no good: the surface is too narrow for the insets, the column is off the screen, the creature
     * ({@code creatureHeight} GUI pixels tall) would stick out of the top of the screen, or another element covers the spot.
     */
    public boolean pickSpot(float minInset, float maxInset, int creatureHeight, @NotNull Spot out) {
        PixelSurfaces.Surface<?> surface = this.surfaces.pick(this.random);
        float span = surface.width() - maxInset - minInset;
        if (span < 0.0F) return false;
        int x = Math.round(surface.x() + minInset + this.random.next() * span);
        int y = surface.restY();
        if ((x < 0) || (x >= this.width)) return false;
        if ((y - creatureHeight + 1 < 0) || (y >= this.height)) return false;
        if (this.surfaces.findElementAt(x, y) != null) return false;
        out.surface = surface;
        out.x = x;
        out.y = y;
        return true;
    }

    /**
     * A spot on top of a surface, filled by {@link #pickSpot}. Keep one per simulation and reuse it.
     */
    public static final class Spot {

        @Nullable
        public PixelSurfaces.Surface<?> surface;
        public int x;
        public int y;

    }

}
