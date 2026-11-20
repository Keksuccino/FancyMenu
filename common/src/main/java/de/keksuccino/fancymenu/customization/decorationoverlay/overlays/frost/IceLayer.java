package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.frost;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

/**
 * The ice of one part of the Frost overlay (the screen edges, the corners or the elements) as two pixel lists: glaze pixels and
 * crystal pixels. Each list holds at most one pixel per spot: freezing a spot twice keeps the earlier time and the thicker glaze
 * or the brighter crystal tone.<br><br>
 *
 * Every pixel freezes at its birth time on the part's clock. It shows fresh for a moment (half-strength glaze, a white crystal),
 * then settles. A blow knocks it out for a while by pushing its time back. The layer keeps a list of the pixels
 * whose look can still change, so a frozen, untouched layer costs nothing per frame.
 */
final class IceLayer {

    static final int HIDDEN = 0;
    static final int FRESH = 1;
    static final int SETTLED = 2;
    private static final float FRESH_GLAZE_AGE = 0.5F;
    private static final float FRESH_CRYSTAL_AGE = 0.3F;

    private final int screenWidth;
    private final int screenHeight;
    final Pixels glaze = new Pixels();
    final Pixels crystals = new Pixels();
    /** Added to the time of every pixel frozen from now on, for ice that shows up later than the rest of its part */
    private double birthDelay = 0.0;

    IceLayer(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /**
     * Freezes a glaze pixel of the given level (0 thin to 2 thick) at the given time. Pixels outside of the screen are ignored.
     */
    void glaze(int x, int y, double birth, int level) {
        this.add(this.glaze, x, y, birth, level, true);
    }

    /**
     * Freezes a crystal pixel of the given tone (0 lit to 3 shaded) at the given time. Pixels outside of the screen are ignored.
     */
    void crystal(int x, int y, double birth, int tone) {
        this.add(this.crystals, x, y, birth, tone, false);
    }

    void setBirthDelay(double delay) {
        this.birthDelay = delay;
    }

    /**
     * Glaze pixels keep the thickest level, crystals the brightest (lowest) tone.
     */
    private void add(@NotNull Pixels pixels, int x, int y, double birth, int value, boolean keepHigher) {
        if ((x < 0) || (y < 0) || (x >= this.screenWidth) || (y >= this.screenHeight)) return;
        birth += this.birthDelay;
        int key = y * this.screenWidth + x;
        int existing = pixels.at.get(key);
        if (existing >= 0) {
            if (pixels.birth[existing] > birth) pixels.birth[existing] = (float)birth;
            int old = pixels.value[existing];
            pixels.value[existing] = (byte)(keepHigher ? Math.max(old, value) : Math.min(old, value));
            return;
        }
        pixels.append(x, y, (float)birth, value, PixelMath.hash(x, y + 90));
        pixels.at.put(key, pixels.count - 1);
    }

    int pixelIndex(int x, int y) {
        return y * this.screenWidth + x;
    }

    /**
     * Returns the glaze pixel at a spot, -1 if there is none.
     */
    int glazeAt(int pixelIndex) {
        return this.glaze.at.get(pixelIndex);
    }

    /**
     * Returns the crystal pixel at a spot, -1 if there is none.
     */
    int crystalAt(int pixelIndex) {
        return this.crystals.at.get(pixelIndex);
    }

    static int stateOf(float age, boolean crystal) {
        if (age < 0.0F) return HIDDEN;
        return (age < (crystal ? FRESH_CRYSTAL_AGE : FRESH_GLAZE_AGE)) ? FRESH : SETTLED;
    }

    /**
     * Brings the pixels whose look can still change to the part's clock and reports every pixel that looks different now.
     * Settled pixels leave the pending lists.
     */
    void settle(float clock, @NotNull PixelListener changed) {
        this.glaze.settle(clock, false, changed);
        this.crystals.settle(clock, true, changed);
    }

    /**
     * Gets told about every pixel whose look changed.
     */
    @FunctionalInterface
    interface PixelListener {

        void changed(int x, int y);

    }

    /**
     * The pixels of one kind of ice, as parallel arrays.
     */
    static final class Pixels {

        int count = 0;
        int[] x = new int[256];
        int[] y = new int[256];
        float[] birth = new float[256];
        /** Glaze level or crystal tone */
        byte[] value = new byte[256];
        /** A fixed random value per pixel, so a blow breaks the ice in a ragged shape */
        float[] grain = new float[256];
        /** How far a blow pushed the pixel's time back */
        float[] knocked = new float[256];
        /** What the pixel shows right now, see {@link IceLayer#stateOf} */
        byte[] shown = new byte[256];
        /** Pixels whose look can still change */
        int[] pending = new int[256];
        int pendingCount = 0;
        boolean[] isPending = new boolean[256];
        final Int2IntOpenHashMap at = new Int2IntOpenHashMap();

        Pixels() {
            this.at.defaultReturnValue(-1);
        }

        void append(int px, int py, float pixelBirth, int pixelValue, float pixelGrain) {
            if (this.count == this.x.length) {
                int size = this.count * 2;
                this.x = Arrays.copyOf(this.x, size);
                this.y = Arrays.copyOf(this.y, size);
                this.birth = Arrays.copyOf(this.birth, size);
                this.value = Arrays.copyOf(this.value, size);
                this.grain = Arrays.copyOf(this.grain, size);
                this.knocked = Arrays.copyOf(this.knocked, size);
                this.shown = Arrays.copyOf(this.shown, size);
                this.isPending = Arrays.copyOf(this.isPending, size);
            }
            this.x[this.count] = px;
            this.y[this.count] = py;
            this.birth[this.count] = pixelBirth;
            this.value[this.count] = (byte)pixelValue;
            this.grain[this.count] = pixelGrain;
            this.count++;
        }

        float age(int index, float clock) {
            return clock - this.birth[index] - this.knocked[index];
        }

        /**
         * Knocks a pixel out of the patch of a blow. It stays away for six seconds or more, longer near the middle of the patch,
         * so the hole freezes over again from its rim. The grain of the ice makes the rim of the hole ragged.
         *
         * @param distance how far the pixel is from where the blow hit
         * @param limit the radius of the patch in the pixel's direction
         * @return whether there was ice to knock out
         */
        boolean knock(int index, float clock, double distance, double limit) {
            if ((index < 0) || (this.age(index, clock) < 0.0F)) return false;
            this.knocked[index] = (float)(clock - this.birth[index] + 6.0 + (limit - distance) * 0.16 + this.grain[index] * 1.5);
            this.markPending(index);
            return true;
        }

        void settle(float clock, boolean crystal, @NotNull PixelListener changed) {
            for (int p = this.pendingCount - 1; p >= 0; p--) {
                int i = this.pending[p];
                int state = stateOf(this.age(i, clock), crystal);
                if (state != this.shown[i]) {
                    this.shown[i] = (byte)state;
                    changed.changed(this.x[i], this.y[i]);
                }
                // The last pending pixel takes this place, and it got its turn already
                if (state == SETTLED) this.dropPending(p);
            }
        }

        void markPending(int index) {
            if (this.isPending[index]) return;
            if (this.pendingCount == this.pending.length) this.pending = Arrays.copyOf(this.pending, this.pendingCount * 2);
            this.pending[this.pendingCount++] = index;
            this.isPending[index] = true;
        }

        void markAllPending() {
            for (int i = 0; i < this.count; i++) this.markPending(i);
        }

        /**
         * Removes the pending pixel at the given position of the pending list. The last one takes its place.
         */
        void dropPending(int position) {
            this.isPending[this.pending[position]] = false;
            this.pending[position] = this.pending[--this.pendingCount];
        }

        void clearPending() {
            for (int i = 0; i < this.pendingCount; i++) this.isPending[this.pending[i]] = false;
            this.pendingCount = 0;
        }

    }

}
