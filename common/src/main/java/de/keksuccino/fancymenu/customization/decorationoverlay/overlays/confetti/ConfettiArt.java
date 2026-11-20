package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * The pixel art of the Confetti overlay. Every piece is one sprite: paper and foil pieces in every step of their flip, streamers in
 * {@value #STREAMER_PHASES} steps of their twist, dots, and settled pieces lying flat as litter. All sprites use the codes {@code f}
 * (face), {@code b} (darker back side) and {@code g} (the white glint of foil), so one palette per color pair draws them all.
 */
final class ConfettiArt {

    static final int TYPE_PAPER = 0;
    static final int TYPE_STREAMER = 1;
    static final int TYPE_DOT = 2;
    static final int TYPE_FOIL = 3;
    /** Face and back colors: pink, yellow, teal, blue, purple, orange and white */
    static final int[][] PAIRS = {
            {0xFF5C8A, 0xC23A6B},
            {0xFFD23F, 0xD49A1E},
            {0x2EC4B6, 0x1B8F88},
            {0x4D8BFF, 0x2F5FC4},
            {0x9B5DE5, 0x6B3BB0},
            {0xFF8A3C, 0xC85A1E},
            {0xF4F6FF, 0xB9C2DA}
    };
    /** Foil only comes in gold and silver */
    static final int[] FOIL_PAIRS = {1, 6};
    /** The palette index of the single custom color, used while color mix is off */
    static final int CUSTOM_PAIR = PAIRS.length;
    static final int STREAMER_PHASES = 64;
    static final int STREAMER_MIN_LENGTH = 8;
    static final int STREAMER_MAX_LENGTH = 12;
    static final int PIECE_MIN_SIZE = 3;
    static final int PIECE_MAX_SIZE = 4;
    /** How much darker the back side of a custom color is, per channel, like the built-in pairs */
    private static final float[] BACK_SHADE = {0.76F, 0.79F, 0.85F};

    /** [width - 3][height - 3][visible width - 1][shear + 1][back side ? 1 : 0][glint ? 1 : 0] */
    private static final PixelSprite[][][][][][] PAPER = buildPaperSprites();
    /** [length - 8][phase step] */
    private static final PixelSprite[][] STREAMERS = new PixelSprite[STREAMER_MAX_LENGTH - STREAMER_MIN_LENGTH + 1][STREAMER_PHASES];
    /** How far left of the piece's x a streamer sprite starts, in art pixels, same indices as {@link #STREAMERS} */
    private static final int[][] STREAMER_OFFSETS = new int[STREAMER_MAX_LENGTH - STREAMER_MIN_LENGTH + 1][STREAMER_PHASES];
    static final PixelSprite DOT = PixelSprite.of(
            "ff",
            "fb");
    /**
     * Settled pieces lying flat, by width: dots (2), paper and foil (3 or 4, face with a darker last pixel) and streamers (6, two-tone).
     * Second index is the blink step: 0 whole, 1 without every odd pixel, 2 without every even pixel.
     */
    private static final String[] LITTER_STRIPS = {"ff", "ffb", "fffb", "ffbbff"};
    private static final PixelSprite[][] LITTER = new PixelSprite[LITTER_STRIPS.length][3];

    static {
        for (int length = STREAMER_MIN_LENGTH; length <= STREAMER_MAX_LENGTH; length++) {
            for (int step = 0; step < STREAMER_PHASES; step++) buildStreamer(length, step);
        }
        for (int kind = 0; kind < LITTER_STRIPS.length; kind++) {
            for (int blink = 0; blink < 3; blink++) {
                char[] codes = LITTER_STRIPS[kind].toCharArray();
                if (blink > 0) {
                    for (int i = 0; i < codes.length; i++) {
                        if (((i + blink - 1) & 1) == 1) codes[i] = '.';
                    }
                }
                LITTER[kind][blink] = PixelSprite.fromCodes(codes.length, 1, codes);
            }
        }
    }

    final PixelPalette[] palettes = new PixelPalette[PAIRS.length + 1];

    ConfettiArt() {
        for (int i = 0; i < PAIRS.length; i++) this.palettes[i] = palette(PixelMath.opaque(PAIRS[i][0]), PixelMath.opaque(PAIRS[i][1]));
        this.setCustomColor(0xFFFFFFFF);
    }

    /**
     * Builds the palette of the single custom color: the color itself as face and a darker, slightly cooler back side.
     */
    void setCustomColor(int argb) {
        int alpha = argb >>> 24;
        int r = PixelMath.red(argb);
        int g = PixelMath.green(argb);
        int b = PixelMath.blue(argb);
        int back = PixelMath.argb(alpha, Math.round(r * BACK_SHADE[0]), Math.round(g * BACK_SHADE[1]), Math.round(b * BACK_SHADE[2]));
        this.palettes[CUSTOM_PAIR] = palette(argb, back);
    }

    @NotNull
    private static PixelPalette palette(int face, int back) {
        return PixelPalette.builder().argb('f', face).argb('b', back).argb('g', PixelMath.withAlpha(0xFFFFFF, face >>> 24)).build();
    }

    /**
     * The sprite of a paper or foil piece in one step of its flip.
     *
     * @param visibleWidth how wide the piece shows right now, from 1 (edge-on) to its width
     * @param shear -1, 0 or 1: the top half shifts by one pixel while the piece is turned halfway
     */
    @NotNull
    static PixelSprite paper(int width, int height, int visibleWidth, int shear, boolean backSide, boolean glint) {
        return PAPER[width - PIECE_MIN_SIZE][height - PIECE_MIN_SIZE][visibleWidth - 1][shear + 1][backSide ? 1 : 0][glint ? 1 : 0];
    }

    @NotNull
    static PixelSprite streamer(int length, int phaseStep) {
        return STREAMERS[length - STREAMER_MIN_LENGTH][phaseStep];
    }

    static int streamerOffset(int length, int phaseStep) {
        return STREAMER_OFFSETS[length - STREAMER_MIN_LENGTH][phaseStep];
    }

    /**
     * The phase step of a streamer whose twist is at {@code phase} radians.
     */
    static int streamerPhaseStep(float phase) {
        int step = Math.round(phase / PixelMath.TAU * STREAMER_PHASES) % STREAMER_PHASES;
        return (step < 0) ? (step + STREAMER_PHASES) : step;
    }

    /**
     * A settled piece lying flat, see {@link #litterWidth}.
     *
     * @param width the width of the piece in art pixels: 2, 3, 4 or 6
     * @param blink 0 for the whole piece, 1 or 2 for the two dither steps it blinks through while it dissolves
     */
    @NotNull
    static PixelSprite litter(int width, int blink) {
        int kind = switch (width) {
            case 2 -> 0;
            case 3 -> 1;
            case 4 -> 2;
            default -> 3;
        };
        return LITTER[kind][blink];
    }

    /**
     * How many art pixels wide a settled piece of the type lies: streamers 6, dots 2, paper and foil 3 or 4 (picked when they land).
     */
    static int litterWidth(int type, boolean wide) {
        return switch (type) {
            case TYPE_STREAMER -> 6;
            case TYPE_DOT -> 2;
            default -> wide ? 4 : 3;
        };
    }

    @NotNull
    private static PixelSprite[][][][][][] buildPaperSprites() {
        int sizes = PIECE_MAX_SIZE - PIECE_MIN_SIZE + 1;
        PixelSprite[][][][][][] sprites = new PixelSprite[sizes][sizes][PIECE_MAX_SIZE][3][2][2];
        for (int width = PIECE_MIN_SIZE; width <= PIECE_MAX_SIZE; width++) {
            for (int height = PIECE_MIN_SIZE; height <= PIECE_MAX_SIZE; height++) {
                for (int visible = 1; visible <= width; visible++) {
                    for (int shear = -1; shear <= 1; shear++) {
                        for (int side = 0; side < 2; side++) {
                            for (int glint = 0; glint < 2; glint++) {
                                sprites[width - PIECE_MIN_SIZE][height - PIECE_MIN_SIZE][visible - 1][shear + 1][side][glint] = buildPaper(height, visible, shear, side == 1, glint == 1);
                            }
                        }
                    }
                }
            }
        }
        return sprites;
    }

    /**
     * Builds a paper piece {@code visibleWidth} pixels wide. Its top half sits {@code shear} pixels to the side, so the sprite starts
     * one pixel further left for a shear of -1. The glint replaces the top left pixel of the unsheared piece.
     */
    @NotNull
    private static PixelSprite buildPaper(int height, int visibleWidth, int shear, boolean backSide, boolean glint) {
        int width = visibleWidth + Math.abs(shear);
        int origin = Math.min(shear, 0);
        char[] codes = new char[width * height];
        Arrays.fill(codes, '.');
        char code = backSide ? 'b' : 'f';
        int half = height >> 1;
        for (int row = 0; row < height; row++) {
            int start = ((row < half) ? shear : 0) - origin;
            for (int i = 0; i < visibleWidth; i++) codes[row * width + start + i] = code;
        }
        if (glint) codes[-origin] = 'g';
        return PixelSprite.fromCodes(width, height, codes);
    }

    /**
     * Builds a streamer at one phase step. Every row is a pixel of the twisting ribbon, swinging up to two pixels to the sides,
     * showing its face or its back depending on the twist, with the back side as a second pixel to its right.
     */
    private static void buildStreamer(int length, int step) {
        float phase = step * PixelMath.TAU / STREAMER_PHASES;
        int[] offsets = new int[length];
        boolean[] face = new boolean[length];
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < length; i++) {
            offsets[i] = Math.round((float)Math.sin(i * 0.9F + phase) * 1.6F);
            face[i] = Math.cos(i * 0.9F + phase) > 0.0;
            min = Math.min(min, offsets[i]);
            max = Math.max(max, offsets[i]);
        }
        int width = max - min + 2;
        char[] codes = new char[width * length];
        Arrays.fill(codes, '.');
        for (int i = 0; i < length; i++) {
            int column = offsets[i] - min;
            codes[i * width + column] = face[i] ? 'f' : 'b';
            codes[i * width + column + 1] = 'b';
        }
        STREAMERS[length - STREAMER_MIN_LENGTH][step] = PixelSprite.fromCodes(width, length, codes);
        STREAMER_OFFSETS[length - STREAMER_MIN_LENGTH][step] = min;
    }

}
