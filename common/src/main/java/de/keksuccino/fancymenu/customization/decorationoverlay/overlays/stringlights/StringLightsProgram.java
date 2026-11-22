package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.jetbrains.annotations.NotNull;

/**
 * The light programs of the String Lights overlay: how the bulbs switch between their three brightness levels over time.
 * Both looks run the same programs. {@link #ON}, {@link #FULLY_LIT}, {@link #HALF_LIT} and {@link #OFF} keep every bulb at one level,
 * {@link #CYCLE} runs the animated programs one after another.
 */
public enum StringLightsProgram {

    /** All bulbs are fully lit. Now and then a single bulb slowly {@link #sag sags} a little darker and back, like a real light. */
    ON("on"),
    /** All bulbs are fully lit ({@link #BRIGHT}). */
    FULLY_LIT("fully_lit"),
    /** All bulbs are half lit ({@link #LIT}). */
    HALF_LIT("half_lit"),
    /** All bulbs are off ({@link #DIM}). */
    OFF("off"),
    /** Runs twinkle, chase, alternate and breathe one after another, each for {@value #CYCLE_SECONDS} seconds. */
    CYCLE("cycle"),
    /** Every bulb picks a random level four times per second: mostly lit, now and then bright or dim. */
    TWINKLE("twinkle"),
    /** A bright bulb with a lit tail runs along the string. */
    CHASE("chase"),
    /** Every second bulb is bright, and the two halves swap. */
    ALTERNATE("alternate"),
    /** All bulbs slowly brighten and dim together. */
    BREATHE("breathe");

    public static final float CYCLE_SECONDS = 7.0F;
    /** Off */
    public static final int DIM = 0;
    /** Half lit */
    public static final int LIT = 1;
    /** Fully lit */
    public static final int BRIGHT = 2;

    private static final StringLightsProgram[] CYCLED = {TWINKLE, CHASE, ALTERNATE, BREATHE};
    private static final int[] BREATHE_LEVELS = {DIM, LIT, BRIGHT, BRIGHT, LIT};
    /** {@link #ON} splits its time into slots of this many seconds. A bulb sags at most once per slot, and every sag fits into its slot. */
    private static final float SAG_SLOT_SECONDS = 7.0F;
    /** Chance of a bulb to sag in a slot, so a bulb sags about every 28 seconds */
    private static final float SAG_CHANCE = 0.25F;
    private static final float SAG_MIN_SECONDS = 2.5F;
    private static final float SAG_MAX_SECONDS = 4.5F;
    /** Depth of the shallowest sag, as a part of the deepest one */
    private static final float SAG_MIN_DEPTH = 0.45F;

    @NotNull
    private final String name;

    StringLightsProgram(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public String getLocalizationKey() {
        return "fancymenu.decoration_overlays.string_lights.program." + this.name;
    }

    /**
     * The program that runs after the given seconds: the program itself, or for {@link #CYCLE} the one whose turn it is.
     */
    @NotNull
    public StringLightsProgram resolve(float cycleSeconds) {
        if (this != CYCLE) return this;
        return CYCLED[Math.floorMod(PixelMath.floor(cycleSeconds / CYCLE_SECONDS), CYCLED.length)];
    }

    /**
     * The brightness level ({@link #DIM}, {@link #LIT} or {@link #BRIGHT}) of a bulb after the given seconds.
     * {@link #CYCLE} has to be {@link #resolve resolved} first and counts as {@link #TWINKLE} here.
     *
     * @param bulb index of the bulb on its string, counted from the start of the string
     * @param string index of the string, so strings twinkle differently
     */
    public int level(int bulb, int string, float seconds) {
        return switch (this) {
            case ON, FULLY_LIT -> BRIGHT;
            case HALF_LIT -> LIT;
            case OFF -> DIM;
            case CYCLE, TWINKLE -> {
                float h = PixelMath.hash(bulb + string * 40, PixelMath.floor(seconds * 4.0F));
                yield (h < 0.1F) ? DIM : ((h > 0.88F) ? BRIGHT : LIT);
            }
            case CHASE -> {
                int step = Math.floorMod(bulb - PixelMath.floor(seconds * 7.0F), 4);
                yield (step == 0) ? BRIGHT : ((step == 1) ? LIT : DIM);
            }
            case ALTERNATE -> (((bulb + PixelMath.floor(seconds * 1.6F)) & 1) != 0) ? BRIGHT : DIM;
            case BREATHE -> {
                float phase = seconds * 0.5F;
                phase -= (float)Math.floor(phase);
                yield BREATHE_LEVELS[Math.min(BREATHE_LEVELS.length - 1, (int)(phase * BREATHE_LEVELS.length))];
            }
        };
    }

    /**
     * How far below its {@link #level} a bulb glows after the given seconds, from 0 (not at all) to 1 (the deepest sag).
     * Only {@link #ON} sags: every bulb on its own and only now and then, easing a bit darker and back over a few seconds.
     *
     * @param bulb index of the bulb on its string, counted from the start of the string
     * @param string index of the string, so strings sag differently
     */
    public float sag(int bulb, int string, float seconds) {
        if (this != ON) return 0.0F;
        int slot = PixelMath.floor(seconds / SAG_SLOT_SECONDS);
        float key = bulb + string * LightString.MAX_BULBS;
        // Four independent hashes per slot and bulb: whether, how long, when and how deep the bulb sags
        float k = slot * 4.0F;
        if (PixelMath.hash(key, k) >= SAG_CHANCE) return 0.0F;
        float duration = PixelMath.lerp(SAG_MIN_SECONDS, SAG_MAX_SECONDS, PixelMath.hash(key, k + 1.0F));
        float start = slot * SAG_SLOT_SECONDS + PixelMath.hash(key, k + 2.0F) * (SAG_SLOT_SECONDS - duration);
        float t = (seconds - start) / duration;
        if ((t <= 0.0F) || (t >= 1.0F)) return 0.0F;
        float depth = PixelMath.lerp(SAG_MIN_DEPTH, 1.0F, PixelMath.hash(key, k + 3.0F));
        return depth * (0.5F - 0.5F * (float)Math.cos(t * PixelMath.TAU));
    }

}
