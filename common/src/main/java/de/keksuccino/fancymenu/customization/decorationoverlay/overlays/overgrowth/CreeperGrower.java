package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Grows the climbing plants of one screen side by simulation, once, as a list of {@link Creeper}s that get revealed as their clock runs.<br><br>
 *
 * Everything gets grown for the left side and mirrored for the right. Three stems start in the grass: the main one hugs the frame up
 * the side and along the top, the other two leave the frame part-way up and arch into the screen. Along every stem, shoots branch off
 * (leaning inward from a climbing section, hanging down from a section that runs along the top; both bend down under their own
 * weight) and tendrils curl up. Shoots end in a tendril or a flower, and spare flowers for a higher flower amount sit at the other
 * shoot ends and as small blossoms along the shoots.<br><br>
 *
 * The plants are made for a 480 x 270 screen (GUI scale 4 at 1080p). On other sizes, where the main stem stops along the top and
 * where shoots steer clear of the middle follow the screen width, and the arches follow the screen height.
 */
final class CreeperGrower {

    private static final double UP = -Math.PI / 2.0;
    private static final double DOWN = Math.PI / 2.0;
    private static final double DESIGN_WIDTH = 480.0;
    private static final double DESIGN_HEIGHT = 270.0;

    private final PixelRandom random;
    private final int height;
    private final double widthScale;
    private final double heightScale;
    /** Shoots further in than this turn away from the title and the menu in the middle */
    private final double middleX;
    /** Shoots hanging from the top further out than this can grow long */
    private final double longShootX;
    private final List<Draft> vines = new ArrayList<>();

    private CreeperGrower(@NotNull PixelRandom random, int width, int height) {
        this.random = random;
        this.height = height;
        this.widthScale = width / DESIGN_WIDTH;
        this.heightScale = height / DESIGN_HEIGHT;
        this.middleX = 136.0 * this.widthScale;
        this.longShootX = 128.0 * this.widthScale;
    }

    /**
     * Grows the plants of one side of a screen of the given size.
     *
     * @param random the random source of the plants, it decides their whole shape
     * @param spareSeed seed of the separate random source of the spare flowers, so the plants stay the same whatever it gives
     */
    @NotNull
    static List<Creeper> grow(@NotNull PixelRandom random, int spareSeed, boolean right, int width, int height) {
        CreeperGrower grower = new CreeperGrower(random, width, height);
        grower.growStems();
        grower.addSpareFlowers(new PixelRandom(spareSeed));
        List<OvergrowthFlower> blooms = new ArrayList<>();
        for (Draft vine : grower.vines) blooms.addAll(vine.blooms);
        // A stable sort keeps the vine order within the first and within the spare flowers
        blooms.sort((a, b) -> Boolean.compare(a.spare, b.spare));
        OvergrowthArt.rank(blooms);
        List<Creeper> creepers = new ArrayList<>(grower.vines.size());
        for (Draft vine : grower.vines) creepers.add(vine.build(right ? width : -1));
        return creepers;
    }

    private void growStems() {
        int h = this.height;
        Draft frame = this.frame(5, h - 5, 6, 12.0 + this.random.next() * 6.0, (200.0 + this.random.next() * 34.0) * this.widthScale);
        Draft high = this.arch(12, h - 4, 10, h * (0.42 + this.random.next() * 0.2), (230.0 + this.random.next() * 70.0) * this.heightScale, 1.2);
        Draft low = this.arch(20, h - 3, 16, h * (0.72 + this.random.next() * 0.1), (110.0 + this.random.next() * 40.0) * this.heightScale, 2.5);
        this.sprout(frame);
        this.sprout(high);
        this.sprout(low);
    }

    private double nextSigned() {
        return this.random.next() - 0.5;
    }

    @NotNull
    private Draft make(@NotNull Points points, double birth, double speed, int kind, int leafGap, int leafKind, double sway) {
        Draft vine = new Draft(points, birth, speed, kind, sway, this.random.next() * 6.0);
        if (leafGap != 0) {
            int n = points.count;
            for (int i = 3 + (int)Math.floor(this.random.next() * 3.0), side = (this.random.next() < 0.5) ? 1 : -1; i < n - 2; i += leafGap + (int)Math.floor(this.random.next() * 3.0), side = -side) {
                int a = Math.max(0, i - 2);
                int c = Math.min(n - 1, i + 2);
                double hx = points.xs[c] - points.xs[a];
                double hy = points.ys[c] - points.ys[a];
                double l = Math.hypot(hx, hy);
                if (l == 0.0) l = 1.0;
                int offsetX = (int)Math.round(-hy / l * side * 2.0);
                int offsetY = (int)Math.round(hx / l * side * 2.0);
                int kindOfLeaf = (i > n - 8) ? OvergrowthArt.LEAF_SMALL : leafKind;
                int palette = (this.random.next() < 0.55) ? 0 : 1;
                vine.addLeaf(i, offsetX, offsetY, kindOfLeaf, palette, side < 0);
            }
        }
        this.vines.add(vine);
        return vine;
    }

    @NotNull
    private Draft tendril(int x, int y, double heading, double birth) {
        int sign = (this.random.next() < 0.5) ? 1 : -1;
        Points points = grow(x, y, heading, 7 + (int)Math.floor(this.random.next() * 6.0), (px, py, th, i) -> sign * (0.1 + i * 0.045));
        return this.make(points, birth, 8.0, Creeper.TENDRIL, 0, 0, 0.0);
    }

    /**
     * A shoot wanders a little, turns toward the ground at the rate {@code gravity}, and steers away from the title and the menu
     * in the middle. It ends in a tendril, a flower or nothing.
     */
    @NotNull
    private Draft shoot(int x, int y, double heading, double length, double gravity, double birth, double sway, int leafKind) {
        double phase = this.random.next() * 6.0;
        Points points = grow(x, y, heading, (int)Math.round(length), (px, py, th, i) -> 0.11 * Math.sin(i * 0.23 + phase) + this.nextSigned() * 0.06
                + clamp(angleDifference(DOWN, th), -1.0, 1.0) * gravity
                + (((px > this.middleX) && (py > 18.0)) ? clamp(angleDifference(DOWN + 0.7, th), -0.25, 0.25) : 0.0));
        Draft vine = this.make(points, birth, 13.0, Creeper.SHOOT, 5, leafKind, sway);
        int n = points.count;
        int endX = points.xs[n - 1];
        int endY = points.ys[n - 1];
        int before = Math.max(0, n - 4);
        double endBirth = birth + n / 13.0;
        double roll = this.random.next();
        if (roll < 0.5) {
            this.tendril(endX, endY, Math.atan2(endY - points.ys[before], endX - points.xs[before]), endBirth + 0.2);
        } else if (roll < 0.85) {
            vine.blooms.add(new OvergrowthFlower(n - 1, (int)Math.floor(this.random.next() * OvergrowthArt.MOSS_KIND_COUNT), false, (float)(endBirth + 0.5), false, 0, 2, 0, 0.0F, false));
        }
        return vine;
    }

    /**
     * A woody stem. Its old sections thicken toward the inside of the screen.
     */
    @NotNull
    private Draft stem(@NotNull Points points, double birth, double speed) {
        Draft vine = this.make(points, birth, speed, Creeper.STEM, 7, 0, 0.0);
        int n = points.count;
        vine.thickX = new byte[n];
        vine.thickY = new byte[n];
        for (int i = 0; i < n; i++) {
            int a = Math.max(0, i - 2);
            int c = Math.min(n - 1, i + 2);
            boolean vertical = Math.abs(points.ys[c] - points.ys[a]) > Math.abs(points.xs[c] - points.xs[a]);
            vine.thickX[i] = (byte)(vertical ? 1 : 0);
            vine.thickY[i] = (byte)(vertical ? 0 : 1);
        }
        return vine;
    }

    /**
     * Keeps a climbing tip at a wavering distance from the side edge.
     */
    private static double hug(double x, double heading, int i, double inset, double phase) {
        double target = UP + clamp((inset + 3.0 * Math.sin(i * 0.05 + phase) + 1.6 * Math.sin(i * 0.13 + phase * 2.0) - x) * 0.3, -0.7, 0.7);
        return clamp(angleDifference(target, heading), -0.2, 0.2);
    }

    /**
     * The main stem: up the side edge, then along the top edge until {@code endX}.
     */
    @NotNull
    private Draft frame(int x0, int y0, double inset, double turnY, double endX) {
        double phase = this.random.next() * 6.0;
        // Long enough to reach any corner of any screen
        int maxLength = Math.max(999, 2 * (this.height + (int)Math.ceil(endX)));
        Points points = grow(x0, y0, UP, maxLength, (x, y, th, i) -> {
            if (y > turnY) return hug(x, th, i, inset, phase);
            if (x >= endX) return Double.NaN;
            double target = clamp((inset + 3.0 * Math.sin(i * 0.05 + phase) + 1.6 * Math.sin(i * 0.13 + phase * 2.0) - y) * 0.3, -0.7, 0.7);
            return clamp(angleDifference(target, th), -0.14, 0.14);
        });
        return this.stem(points, 0.0, 26.0);
    }

    /**
     * A stem that climbs the side edge until {@code leaveY} and then arches into the screen.
     */
    @NotNull
    private Draft arch(int x0, int y0, double inset, double leaveY, double length, double birth) {
        double phase = this.random.next() * 6.0;
        int[] leftAt = {-1};
        Points points = grow(x0, y0, UP, (int)Math.round(length), (x, y, th, i) -> {
            if ((leftAt[0] < 0) && (y > leaveY)) return hug(x, th, i, inset, phase);
            if (leftAt[0] < 0) leftAt[0] = i;
            double target = -1.0 + Math.min(2.4, (i - leftAt[0]) * 0.032) + 0.25 * Math.sin(i * 0.17 + phase);
            return clamp(angleDifference(target, th), -0.1, 0.1);
        });
        return this.stem(points, birth, 22.0);
    }

    /**
     * Grows the shoots and tendrils along a stem.
     */
    private void sprout(@NotNull Draft stem) {
        Points p = stem.points;
        double nextShoot = 14.0 + this.random.next() * 16.0;
        double nextTendril = 6.0 + this.random.next() * 8.0;
        for (int i = 6; i < p.count - 4; i++) {
            int x = p.xs[i];
            int y = p.ys[i];
            double heading = Math.atan2(p.ys[i + 3] - p.ys[i - 3], p.xs[i + 3] - p.xs[i - 3]);
            double birth = stem.birth + i / stem.speed;
            if (--nextShoot <= 0.0) {
                if (Math.abs(Math.sin(heading)) > 0.7) {
                    // From a climbing section a shoot leans inward
                    this.shoot(x, y, -0.3 + this.nextSigned() * 0.9, 14.0 + this.random.next() * 30.0, 0.03 + this.random.next() * 0.02, birth + 0.4, 0.0, OvergrowthArt.LEAF_SMALL);
                    nextShoot = 24.0 + this.random.next() * 20.0;
                } else {
                    // From a section along the top a shoot hangs down, and long ones near the corner branch again
                    double length = (x < this.longShootX) ? (16.0 + this.random.next() * this.random.next() * 72.0) : (6.0 + this.random.next() * 9.0);
                    Draft hanging = this.shoot(x, y, DOWN + this.nextSigned() * 0.9, length, 0.05, birth + 0.4, length * 0.05, (length > 34.0) ? OvergrowthArt.LEAF_IVY : OvergrowthArt.LEAF_SMALL);
                    if (length > 42.0) {
                        int j = (int)Math.floor(hanging.points.count * (0.3 + this.random.next() * 0.3));
                        this.shoot(hanging.points.xs[j], hanging.points.ys[j], DOWN + this.nextSigned() * 1.6, 10.0 + this.random.next() * 16.0, 0.05, hanging.birth + j / 13.0 + 0.4, 2.0, OvergrowthArt.LEAF_SMALL);
                    }
                    nextShoot = 15.0 + this.random.next() * 17.0;
                }
            }
            if (--nextTendril <= 0.0) {
                double turn = ((this.random.next() < 0.5) ? 1.0 : -1.0) * (1.1 + this.random.next() * 0.6);
                this.tendril(x, y, heading + turn, birth + 0.8);
                nextTendril = 12.0 + this.random.next() * 12.0;
            }
        }
    }

    /**
     * Spare flowers have their own random source, so the vines themselves stay as they are at any flower amount.
     */
    private void addSpareFlowers(@NotNull PixelRandom spare) {
        for (Draft vine : this.vines) {
            if (vine.kind != Creeper.SHOOT) continue;
            Points p = vine.points;
            int n = p.count;
            if (vine.blooms.isEmpty() && (spare.next() < 0.45)) {
                vine.blooms.add(new OvergrowthFlower(n - 1, (int)Math.floor(spare.next() * OvergrowthArt.MOSS_KIND_COUNT), false, (float)(vine.birth + n / 13.0 + 0.5), true, 0, 2, 0, 0.0F, false));
            }
            for (int i = 9 + (int)Math.floor(spare.next() * 12.0), side = (spare.next() < 0.5) ? 1 : -1; i < n - 6; i += 20 + (int)Math.floor(spare.next() * 16.0), side = -side) {
                if (vine.hasLeafNear(i)) i++;
                int a = Math.max(0, i - 2);
                int c = Math.min(n - 1, i + 2);
                double hx = p.xs[c] - p.xs[a];
                double hy = p.ys[c] - p.ys[a];
                double l = Math.hypot(hx, hy);
                if (l == 0.0) l = 1.0;
                int kind = (int)Math.floor(spare.next() * OvergrowthArt.MOSS_KIND_COUNT);
                vine.blooms.add(new OvergrowthFlower(i, kind, true, (float)(vine.birth + i / 13.0 + 0.8), true, (int)Math.round(-hy / l * side * 2.0), (int)Math.round(hx / l * side * 2.0) + 1, 0, 0.0F, false));
            }
        }
    }

    /**
     * Grows a line pixel by pixel: the tip advances one pixel per step along its heading, and the steering returns how far it turns,
     * or NaN to stop. The corner pixel of every L-shaped step is dropped, so the line stays a clean one-pixel line.
     */
    @NotNull
    static Points grow(double x, double y, double heading, int length, @NotNull Steering steering) {
        Points points = new Points(Math.max(1, Math.min(length, 64)));
        points.add((int)Math.round(x), (int)Math.round(y));
        for (int i = 0; (i < length * 2) && (points.count < length); i++) {
            double turn = steering.turn(x, y, heading, i);
            if (Double.isNaN(turn)) break;
            heading += turn;
            x += Math.cos(heading);
            y += Math.sin(heading);
            int qx = (int)Math.round(x);
            int qy = (int)Math.round(y);
            int last = points.count - 1;
            if ((qx == points.xs[last]) && (qy == points.ys[last])) continue;
            if (points.count >= 2) {
                int before = points.count - 2;
                if ((Math.abs(qx - points.xs[before]) == 1) && (Math.abs(qy - points.ys[before]) == 1)) points.count--;
            }
            points.add(qx, qy);
        }
        return points;
    }

    static double angleDifference(double a, double b) {
        return Math.atan2(Math.sin(a - b), Math.cos(a - b));
    }

    private static double clamp(double value, double min, double max) {
        return (value < min) ? min : Math.min(value, max);
    }

    @FunctionalInterface
    interface Steering {

        /**
         * @return how far the heading turns in this step, NaN to stop growing
         */
        double turn(double x, double y, double heading, int step);

    }

    /**
     * A growing list of pixel positions.
     */
    static final class Points {

        int[] xs;
        int[] ys;
        int count = 0;

        Points(int capacity) {
            this.xs = new int[capacity];
            this.ys = new int[capacity];
        }

        void add(int x, int y) {
            if (this.count == this.xs.length) {
                this.xs = Arrays.copyOf(this.xs, this.count * 2);
                this.ys = Arrays.copyOf(this.ys, this.count * 2);
            }
            this.xs[this.count] = x;
            this.ys[this.count] = y;
            this.count++;
        }

    }

    /**
     * A vine while the plants are still growing, before it gets mirrored and turned into a {@link Creeper}.
     */
    private static final class Draft {

        final Points points;
        final double birth;
        final double speed;
        final int kind;
        final double sway;
        final double phase;
        final List<OvergrowthFlower> blooms = new ArrayList<>();
        private final List<int[]> leaves = new ArrayList<>();
        byte[] thickX = new byte[0];
        byte[] thickY = new byte[0];

        Draft(@NotNull Points points, double birth, double speed, int kind, double sway, double phase) {
            this.points = points;
            this.birth = birth;
            this.speed = speed;
            this.kind = kind;
            this.sway = sway;
            this.phase = phase;
        }

        void addLeaf(int index, int offsetX, int offsetY, int kind, int palette, boolean flip) {
            this.leaves.add(new int[]{index, offsetX, offsetY, kind, palette, flip ? 1 : 0});
        }

        boolean hasLeafNear(int index) {
            for (int[] leaf : this.leaves) {
                if (Math.abs(leaf[0] - index) < 2) return true;
            }
            return false;
        }

        /**
         * @param mirrorWidth screen width to mirror the vine to the right side, -1 for the left side
         */
        @NotNull
        Creeper build(int mirrorWidth) {
            boolean mirror = mirrorWidth >= 0;
            int n = this.points.count;
            int[] xs = Arrays.copyOf(this.points.xs, n);
            int[] ys = Arrays.copyOf(this.points.ys, n);
            byte[] thickX = (this.thickX.length == n) ? this.thickX.clone() : new byte[n];
            byte[] thickY = (this.thickY.length == n) ? this.thickY.clone() : new byte[n];
            if (mirror) {
                for (int i = 0; i < n; i++) {
                    xs[i] = mirrorWidth - 1 - xs[i];
                    thickX[i] = (byte)-thickX[i];
                }
            }
            int leafCount = this.leaves.size();
            int[] leafIndex = new int[leafCount];
            int[] leafOffsetX = new int[leafCount];
            int[] leafOffsetY = new int[leafCount];
            byte[] leafKind = new byte[leafCount];
            byte[] leafPalette = new byte[leafCount];
            boolean[] leafFlip = new boolean[leafCount];
            for (int l = 0; l < leafCount; l++) {
                int[] leaf = this.leaves.get(l);
                leafIndex[l] = leaf[0];
                leafOffsetX[l] = mirror ? -leaf[1] : leaf[1];
                leafOffsetY[l] = leaf[2];
                leafKind[l] = (byte)leaf[3];
                leafPalette[l] = (byte)leaf[4];
                leafFlip[l] = (leaf[5] != 0) != mirror;
            }
            OvergrowthFlower[] blooms = this.blooms.toArray(new OvergrowthFlower[0]);
            if (mirror) {
                for (OvergrowthFlower flower : blooms) flower.offsetX = -flower.offsetX;
            }
            return new Creeper(this.kind, (float)this.birth, (float)this.speed, (float)this.sway, (float)this.phase, xs, ys, thickX, thickY, leafIndex, leafOffsetX, leafOffsetY, leafKind, leafPalette, leafFlip, blooms);
        }

    }

}
