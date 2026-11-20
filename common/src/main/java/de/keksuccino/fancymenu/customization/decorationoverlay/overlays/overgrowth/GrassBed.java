package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * The ground of the Overgrowth overlay along the bottom edge: a back row of tall dark blades and a front row of short bright ones,
 * in tufts of uneven height, on a solid foot, with leafy sprouts, mushrooms and flowers on stems standing between the two rows.
 * Blades and stems sway in a light wind. Flower stems also stand on the bare floor when the grass is off.<br>
 * The wind moves something every frame, so the ground gets painted again every frame while any of it shows. It is all one strip
 * along the bottom of the screen.
 */
final class GrassBed {

    /** Rows of the strip along the bottom edge that hold the ground, more than its tallest blade or flower */
    static final int STRIP_HEIGHT = 24;
    /** Clock units per pixel a blade or flower stem grows */
    private static final float BLADE_SPEED = 3.5F;
    private static final float STEM_SPEED = 4.0F;

    private int width = 0;
    private int height = 0;
    private Row backRow = new Row(0);
    private Row frontRow = new Row(0);
    private int[] sproutX = new int[0];
    private float[] sproutBirth = new float[0];
    private int[] mushroomX = new int[0];
    private float[] mushroomBirth = new float[0];
    private OvergrowthFlower[] stems = new OvergrowthFlower[0];
    private float[] stemWindSin = new float[0];
    private float[] stemWindCos = new float[0];
    private float[] stemWindSlowSin = new float[0];
    private float[] stemWindSlowCos = new float[0];
    private float[] footDelay = new float[0];
    private boolean[] footDark = new boolean[0];
    private boolean[] footThird = new boolean[0];

    /** The state of the frame being painted, set by {@link #update} */
    private float grassClock = 0.0F;
    private boolean grass = false;
    private float windFastSin = 0.0F;
    private float windFastCos = 1.0F;
    private float windSlowSin = 0.0F;
    private float windSlowCos = 1.0F;
    private float[] stemAges = new float[0];

    /**
     * Grows the ground of a screen of the given size, once.
     *
     * @param spareSeed seed of the separate random source of the spare flower stems
     */
    void build(@NotNull PixelRandom random, int spareSeed, int width, int height) {
        this.width = width;
        this.height = height;
        List<float[]> back = new ArrayList<>();
        List<float[]> front = new ArrayList<>();
        for (int row = 0; row < 2; row++) {
            List<float[]> blades = (row == 0) ? back : front;
            for (double x = -1.0; x < width + 1; x += (row == 1) ? (1.25 + random.next() * 0.9) : (1.6 + random.next() * 1.2)) {
                double tuft = 0.55 + 0.7 * PixelMath.valueNoise((float)(x * 0.035), row * 7 + 3);
                double h = ((row == 1) ? (3.0 + random.next() * random.next() * 8.0) : (6.0 + random.next() * 9.0)) * tuft;
                double lean = (random.next() - 0.5) * 1.8;
                double birth = random.next() * 7.0 + ((row == 1) ? 1.5 : 0.0);
                boolean bright = random.next() < 0.22;
                blades.add(new float[]{(float)x, (float)h, (float)lean, (float)birth, bright ? 1 : 0});
            }
        }
        this.backRow = Row.of(back);
        this.frontRow = Row.of(front);

        List<float[]> sprouts = new ArrayList<>();
        for (double x = 14.0 + random.next() * 20.0; x < width - 10; x += 26.0 + random.next() * 34.0) sprouts.add(new float[]{(int)Math.round(x), (float)(3.0 + random.next() * 7.0)});
        this.sproutX = new int[sprouts.size()];
        this.sproutBirth = new float[sprouts.size()];
        for (int i = 0; i < sprouts.size(); i++) {
            this.sproutX[i] = (int)sprouts.get(i)[0];
            this.sproutBirth[i] = sprouts.get(i)[1];
        }
        List<float[]> mushrooms = new ArrayList<>();
        for (double x = 40.0 + random.next() * 60.0; x < width - 30; x += 70.0 + random.next() * 90.0) mushrooms.add(new float[]{(int)Math.round(x), (float)(7.0 + random.next() * 6.0)});
        this.mushroomX = new int[mushrooms.size()];
        this.mushroomBirth = new float[mushrooms.size()];
        for (int i = 0; i < mushrooms.size(); i++) {
            this.mushroomX[i] = (int)mushrooms.get(i)[0];
            this.mushroomBirth[i] = mushrooms.get(i)[1];
        }

        List<OvergrowthFlower> stems = new ArrayList<>();
        List<Integer> used = new ArrayList<>();
        for (double x = 8.0 + random.next() * 14.0; x < width - 6; x += 13.0 + random.next() * 24.0) {
            int fx = (int)Math.round(x);
            int h = 5 + (int)Math.floor(random.next() * 9.0);
            float lean = (float)((random.next() - 0.5) * 1.6);
            int kind = (int)Math.floor(random.next() * OvergrowthArt.KIND_COUNT);
            float birth = (float)(5.0 + random.next() * 9.0);
            boolean leaf = random.next() < 0.6;
            stems.add(new OvergrowthFlower(fx, kind, false, birth, false, 0, 0, h, lean, leaf));
            used.add(fx);
        }
        // Spare flower stems stand between the first ones, from their own random source
        PixelRandom spare = new PixelRandom(spareSeed);
        for (double x = 6.0 + spare.next() * 5.0; x < width - 6; x += 5.0 + spare.next() * 4.0) {
            int fx = (int)Math.round(x);
            boolean clear = true;
            for (int u : used) {
                if (Math.abs(u - fx) < 5) {
                    clear = false;
                    break;
                }
            }
            if (!clear) continue;
            int h = 4 + (int)Math.floor(spare.next() * 9.0);
            float lean = (float)((spare.next() - 0.5) * 1.6);
            int kind = (int)Math.floor(spare.next() * OvergrowthArt.KIND_COUNT);
            float birth = (float)(5.0 + spare.next() * 9.0);
            boolean leaf = spare.next() < 0.5;
            stems.add(new OvergrowthFlower(fx, kind, false, birth, true, 0, 0, h, lean, leaf));
            used.add(fx);
        }
        stems.sort((a, b) -> (a.spare != b.spare) ? Boolean.compare(a.spare, b.spare) : Integer.compare(a.position, b.position));
        OvergrowthArt.rank(stems);
        this.stems = stems.toArray(new OvergrowthFlower[0]);
        this.stemAges = new float[this.stems.length];
        this.stemWindSin = new float[this.stems.length];
        this.stemWindCos = new float[this.stems.length];
        this.stemWindSlowSin = new float[this.stems.length];
        this.stemWindSlowCos = new float[this.stems.length];
        for (int i = 0; i < this.stems.length; i++) {
            int x = this.stems[i].position;
            this.stemWindSin[i] = (float)Math.sin(x * 0.06);
            this.stemWindCos[i] = (float)Math.cos(x * 0.06);
            this.stemWindSlowSin[i] = (float)Math.sin(x * 0.021);
            this.stemWindSlowCos[i] = (float)Math.cos(x * 0.021);
        }

        this.footDelay = new float[width];
        this.footDark = new boolean[width];
        this.footThird = new boolean[width];
        for (int x = 0; x < width; x++) {
            this.footDelay[x] = PixelMath.hash(x, 31) * 2.5F;
            this.footDark[x] = PixelMath.hash(x, 5) > 0.4F;
            this.footThird[x] = PixelMath.valueNoise(x * 0.09F, 4.0F) > 0.45F;
        }
    }

    /**
     * Brings the flowers of the ground to their clock and sets what the next paint shows.
     *
     * @param grassClock clock of the grass part, 0 while it is off
     * @param floorClock clock of the flowers on the floor, which runs while the grass or these flowers are on
     * @param grass whether the grass is on
     * @param flowers whether the flowers in the grass are on
     * @param bloomThreshold flowers with a rank below it show, 0 for none
     * @return whether anything of the ground shows
     */
    boolean update(float grassClock, float floorClock, float time, boolean grass, boolean flowers, float bloomThreshold) {
        this.grassClock = grassClock;
        this.grass = grass;
        // wind(x) = sin(time * 1.5 + x * 0.06) * 0.9 + sin(time * 0.7 + x * 0.021) * 0.5, split by angle addition
        this.windFastSin = (float)Math.sin(time * 1.5);
        this.windFastCos = (float)Math.cos(time * 1.5);
        this.windSlowSin = (float)Math.sin(time * 0.7);
        this.windSlowCos = (float)Math.cos(time * 0.7);
        boolean shown = grass && (grassClock > 0.0F);
        for (int i = 0; i < this.stems.length; i++) {
            OvergrowthFlower stem = this.stems[i];
            this.stemAges[i] = stem.age(floorClock, flowers && stem.isLetIn(bloomThreshold));
            if (this.stemAges[i] > 0.0F) shown = true;
        }
        return shown;
    }

    private float wind(float sin, float cos, float slowSin, float slowCos) {
        return (this.windFastSin * cos + this.windFastCos * sin) * 0.9F + (this.windSlowSin * slowCos + this.windSlowCos * slowSin) * 0.5F;
    }

    /**
     * Paints the whole ground: foot, back row, sprouts, mushrooms, flower stems and the front row.
     */
    void paint(@NotNull CanvasPainter painter) {
        int[] green = OvergrowthArt.GREEN;
        int bottom = this.height - 1;
        float g = this.grassClock;
        if (this.grass && (g > 0.0F)) {
            // A solid foot under the blades, so the grass reads as one mass
            for (int x = Math.max(0, painter.clipX0()); x < Math.min(this.width, painter.clipX1()); x++) {
                float a = g - this.footDelay[x];
                if (a <= 0.0F) continue;
                painter.pixel(x, bottom, green[4]);
                if (a > 0.4F) painter.pixel(x, bottom - 1, this.footDark[x] ? green[4] : green[3]);
                if ((a > 0.8F) && this.footThird[x]) painter.pixel(x, bottom - 2, green[3]);
            }
            this.paintRow(painter, this.backRow, g, false);
            for (int i = 0; i < this.sproutX.length; i++) {
                float a = g - this.sproutBirth[i];
                if (a <= 0.0F) continue;
                int x = this.sproutX[i];
                if (a < 0.6F) {
                    painter.sprite(OvergrowthArt.LEAF[OvergrowthArt.LEAF_BUD], OvergrowthArt.LEAF_PALETTE[0], x - 1, this.height - 4, false);
                    continue;
                }
                painter.sprite(OvergrowthArt.LEAF[OvergrowthArt.LEAF_IVY], OvergrowthArt.LEAF_PALETTE[1], x - 5, this.height - 7, true);
                painter.sprite(OvergrowthArt.LEAF[OvergrowthArt.LEAF_IVY], OvergrowthArt.LEAF_PALETTE[1], x, this.height - 7, false);
                if (a > 1.2F) painter.sprite(OvergrowthArt.LEAF[OvergrowthArt.LEAF_SMALL], OvergrowthArt.LEAF_PALETTE[0], x - 2, this.height - 9, false);
            }
            for (int i = 0; i < this.mushroomX.length; i++) {
                float a = g - this.mushroomBirth[i];
                if (a > 0.0F) painter.sprite(OvergrowthArt.MUSHROOM, OvergrowthArt.MUSHROOM_PALETTE, this.mushroomX[i] - 2, this.height - ((a < 0.8F) ? 5 : 7), false);
            }
        }
        // Flower stems stand between the two rows of grass, or on the bare floor when the grass is off
        int foot = this.grass ? (this.height - 2) : (this.height - 1);
        for (int i = 0; i < this.stems.length; i++) {
            float a = this.stemAges[i];
            if (a <= 0.0F) continue;
            OvergrowthFlower stem = this.stems[i];
            int h = Math.min(stem.stem, (int)Math.ceil(a * STEM_SPEED));
            float lean = stem.lean + this.wind(this.stemWindSin[i], this.stemWindCos[i], this.stemWindSlowSin[i], this.stemWindSlowCos[i]);
            int tipX = paintBlade(painter, stem.position, foot, h, lean, green[3], green[3], green[2]);
            if (stem.leaf && (h > 4)) painter.sprite(OvergrowthArt.LEAF[OvergrowthArt.LEAF_BUD], OvergrowthArt.LEAF_PALETTE[0], stem.position + 1, foot - 3, false);
            if (h >= stem.stem) {
                // The bloom starts as a bud the moment the stem is done, so a young age still shows the bud
                float bloomAge = a - stem.stem / STEM_SPEED;
                int stage = (bloomAge < 1.0F) ? OvergrowthArt.BLOOM_BUD : OvergrowthArt.bloomStage(bloomAge, false);
                OvergrowthArt.paintBloom(painter, tipX, foot - h, stem.kind, stage);
            }
        }
        if (this.grass && (g > 0.0F)) this.paintRow(painter, this.frontRow, g, true);
    }

    private void paintRow(@NotNull CanvasPainter painter, @NotNull Row row, float clock, boolean front) {
        int[] green = OvergrowthArt.GREEN;
        int bottom = this.height - 1;
        for (int i = 0; i < row.count; i++) {
            float h = Math.min(row.height[i], (clock - row.birth[i]) * BLADE_SPEED);
            if (h < 1.0F) continue;
            float lean = row.lean[i] + this.wind(row.windSin[i], row.windCos[i], row.windSlowSin[i], row.windSlowCos[i]);
            int x = (int)Math.round(row.x[i]);
            int blade = (int)Math.round(h);
            if (!front) {
                paintBlade(painter, x, bottom, blade, lean, green[4], green[3], green[3]);
            } else if (row.bright[i]) {
                paintBlade(painter, x, bottom, blade, lean, green[2], green[1], green[0]);
            } else {
                paintBlade(painter, x, bottom, blade, lean, green[3], green[2], green[2]);
            }
        }
    }

    /**
     * One curved grass blade or flower stem of height h growing up from x, y: dark at the foot, lit at the tip.
     *
     * @return the column of its tip
     */
    static int paintBlade(@NotNull CanvasPainter painter, int x, int y, int h, float lean, int foot, int middle, int tip) {
        int column = x;
        for (int i = 0; i < h; i++) {
            float u = (i + 1) / (float)h;
            column = (int)Math.round(x + lean * u * u * h * 0.3);
            painter.pixel(column, y - i, (i >= h - 1) ? tip : ((i < h * 0.45F) ? foot : middle));
        }
        return column;
    }

    int stemCount() {
        return this.stems.length;
    }

    @NotNull
    OvergrowthFlower stem(int index) {
        return this.stems[index];
    }

    float stemAge(int index) {
        return this.stemAges[index];
    }

    /**
     * One row of blades, sorted along the screen.
     */
    private static final class Row {

        final int count;
        final float[] x;
        final float[] height;
        final float[] lean;
        final float[] birth;
        final boolean[] bright;
        final float[] windSin;
        final float[] windCos;
        final float[] windSlowSin;
        final float[] windSlowCos;

        Row(int count) {
            this.count = count;
            this.x = new float[count];
            this.height = new float[count];
            this.lean = new float[count];
            this.birth = new float[count];
            this.bright = new boolean[count];
            this.windSin = new float[count];
            this.windCos = new float[count];
            this.windSlowSin = new float[count];
            this.windSlowCos = new float[count];
        }

        @NotNull
        static Row of(@NotNull List<float[]> blades) {
            Row row = new Row(blades.size());
            for (int i = 0; i < row.count; i++) {
                float[] blade = blades.get(i);
                row.x[i] = blade[0];
                row.height[i] = blade[1];
                row.lean[i] = blade[2];
                row.birth[i] = blade[3];
                row.bright[i] = blade[4] != 0.0F;
                row.windSin[i] = (float)Math.sin(blade[0] * 0.06);
                row.windCos[i] = (float)Math.cos(blade[0] * 0.06);
                row.windSlowSin[i] = (float)Math.sin(blade[0] * 0.021);
                row.windSlowCos[i] = (float)Math.cos(blade[0] * 0.021);
            }
            return row;
        }

    }

}
