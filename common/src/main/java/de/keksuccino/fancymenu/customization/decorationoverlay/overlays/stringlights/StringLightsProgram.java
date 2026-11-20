package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import org.jetbrains.annotations.NotNull;

/**
 * The light programs of the String Lights overlay: how the bulbs switch between their three brightness levels over time.
 * Both looks run the same programs. {@link #CYCLE} runs the other programs one after another.
 */
public enum StringLightsProgram {

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
    public static final int DIM = 0;
    public static final int LIT = 1;
    public static final int BRIGHT = 2;

    private static final StringLightsProgram[] CYCLED = {TWINKLE, CHASE, ALTERNATE, BREATHE};
    private static final int[] BREATHE_LEVELS = {DIM, LIT, BRIGHT, BRIGHT, LIT};

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

}
