package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Colors, sprites and shared drawing rules of the Overgrowth overlay.
 */
final class OvergrowthArt {

    /** Greens from sunlit to deep shade, shared by moss, leaves and grass. */
    static final int[] GREEN = {0xFFC2E273, 0xFF90C754, 0xFF5FA041, 0xFF3C7B37, 0xFF26572D};
    /** Bark of old, woody stem sections: lit side and shaded side. */
    static final int BARK_LIT = 0xFF7A6840;
    static final int BARK_SHADE = 0xFF4E4129;
    /** The soft shadow thick moss throws onto the top edge of an element. */
    static final int MOSS_SHADOW = 0x6E0C0F16;

    /** Ivy leaf, small leaf and leaf bud, each lit from the upper left. */
    static final PixelSprite[] LEAF = {
            PixelSprite.of(
                    ".m.m.",
                    "mhmmd",
                    "mhmdd",
                    ".mdd.",
                    "..d.."),
            PixelSprite.of(
                    ".mm.",
                    "mhmd",
                    ".mdd"),
            PixelSprite.of(
                    "mh",
                    "dm")};
    static final int LEAF_IVY = 0;
    static final int LEAF_SMALL = 1;
    static final int LEAF_BUD = 2;
    /** Leaves in the light and leaves in the shade. */
    static final PixelPalette[] LEAF_PALETTE = {
            PixelPalette.builder().argb('h', GREEN[0]).argb('m', GREEN[1]).argb('d', GREEN[3]).build(),
            PixelPalette.builder().argb('h', GREEN[1]).argb('m', GREEN[2]).argb('d', GREEN[4]).build()};

    private static final PixelSprite ROSE = PixelSprite.of(
            "..P..",
            ".PPp.",
            "PPcpq",
            ".ppq.",
            "..q..");
    private static final PixelSprite DAISY = PixelSprite.of(
            ".P.p.",
            "PPcpp",
            ".ccc.",
            "ppcqq",
            ".p.q.");
    private static final PixelSprite TULIP = PixelSprite.of(
            "P.p",
            "PPp",
            "ppq",
            ".q.");
    private static final PixelSprite BELL = PixelSprite.of(
            ".P.",
            "Pp.",
            ".pq",
            "pq.",
            ".q.");
    /** The small bloom every flower opens into first, and the only bloom of small flowers. */
    private static final PixelSprite SMALL_BLOOM = PixelSprite.of(
            ".P.",
            "pcq",
            ".q.");
    /** Per flower color: lit petal, petal, shaded petal, center. */
    private static final int[][] BLOOM_COLORS = {
            {0xFFFFFF, 0xECEEF8, 0xC4C9E0, 0xFFD84A},
            {0xFFC8E2, 0xFF8FC2, 0xD65C98, 0xFFEBA6},
            {0xFF9078, 0xEE4E3E, 0xB42E2E, 0xFFD84A},
            {0xFFF7A6, 0xFFD84A, 0xE2A122, 0xF08A2A},
            {0xC8E0FF, 0x86B4FF, 0x5A84E0, 0xFFFFFF},
            {0xDCC0FF, 0xAA80F0, 0x7A54C8, 0xFFFFFF}};
    private static final PixelPalette[] BLOOM_PALETTE = new PixelPalette[BLOOM_COLORS.length];
    /** Flower kinds as shape and color. The first five grow on moss and vines, all of them in the grass. */
    private static final PixelSprite[] KIND_SHAPE = {DAISY, ROSE, ROSE, DAISY, ROSE, TULIP, TULIP, BELL, TULIP, BELL};
    private static final int[] KIND_COLOR = {0, 1, 2, 3, 4, 2, 1, 5, 3, 4};
    static final int KIND_COUNT = KIND_SHAPE.length;
    /** Flowers on moss and vines pick from the first kinds only. */
    static final int MOSS_KIND_COUNT = 5;

    static final PixelSprite MUSHROOM = PixelSprite.of(
            ".rRr.",
            "rRwrr",
            "rrrwr",
            "..s..",
            "..s..");
    static final PixelPalette MUSHROOM_PALETTE = PixelPalette.builder().rgb('r', 0xD8402E).rgb('R', 0xF2745A).rgb('w', 0xFFFFFF).rgb('s', 0xEADFC8).build();

    /** Bloom stages, see {@link #bloomStage}. */
    static final int BLOOM_HIDDEN = 0;
    static final int BLOOM_BUD = 1;
    static final int BLOOM_SMALL = 2;
    static final int BLOOM_FULL = 3;
    /** Age at which a bud opens into the small bloom, and the small bloom into the full one. */
    private static final float BUD_AGE = 1.0F;
    private static final float FULL_AGE = 2.2F;

    static {
        for (int i = 0; i < BLOOM_COLORS.length; i++) {
            int[] c = BLOOM_COLORS[i];
            BLOOM_PALETTE[i] = PixelPalette.builder().rgb('P', c[0]).rgb('p', c[1]).rgb('q', c[2]).rgb('c', c[3]).build();
        }
    }

    private OvergrowthArt() {
    }

    /**
     * The stage a flower of this age shows: hidden at 0 or below, then bud, small bloom and full bloom. Small flowers stay small.
     */
    static int bloomStage(float age, boolean small) {
        if (age <= 0.0F) return BLOOM_HIDDEN;
        if (age < BUD_AGE) return BLOOM_BUD;
        return ((age < FULL_AGE) || small) ? BLOOM_SMALL : BLOOM_FULL;
    }

    /**
     * Paints a flower standing on the pixel x, y in a stage of {@link #bloomStage}: a bud is a green pixel with a petal pixel on top,
     * the blooms are sprites with their bottom row on y.
     */
    static void paintBloom(@NotNull CanvasPainter painter, int x, int y, int kind, int stage) {
        if (stage == BLOOM_HIDDEN) return;
        if (stage == BLOOM_BUD) {
            painter.pixel(x, y, GREEN[2]);
            painter.pixel(x, y - 1, 0xFF000000 | BLOOM_COLORS[KIND_COLOR[kind]][1]);
            return;
        }
        PixelSprite sprite = (stage == BLOOM_SMALL) ? SMALL_BLOOM : KIND_SHAPE[kind];
        painter.sprite(sprite, BLOOM_PALETTE[KIND_COLOR[kind]], x - (sprite.width() >> 1), y - sprite.height() + 1, false);
    }


    /**
     * Gives every flower of one place a rank: a flower shows while its rank is below a third of the flower amount.
     * The first flowers share the lowest third, so all of them are there at 100 %, and the spare ones fill the rest up to 300 %.
     * Ranks step by the golden ratio along the list, so any amount thins out or fills up evenly instead of in clumps.
     * The list has to hold the first flowers before the spare ones.
     */
    static void rank(@NotNull List<OvergrowthFlower> flowers) {
        int first = 0;
        int spare = 0;
        for (OvergrowthFlower flower : flowers) {
            flower.rank = flower.spare ? (1.0F / 3.0F + goldenStep(++spare) * 2.0F / 3.0F) : (goldenStep(++first) / 3.0F);
        }
    }

    private static float goldenStep(int n) {
        double v = n * 0.618034;
        return (float)(v - Math.floor(v));
    }


}
