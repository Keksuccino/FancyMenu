package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.shootingstars;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;

/**
 * Decides when meteors cross the sky and moves them: a single meteor every few seconds, and now and then a shower that sends
 * seven to eleven meteors from the same direction. Positions are GUI pixels, times are seconds.
 */
final class MeteorShower {

    /** More meteors than a shower and a single one can ever have in the sky at once. */
    static final int CAPACITY = 16;
    static final float FIRST_SINGLE_DELAY = 1.2F;
    static final float FIRST_SHOWER_DELAY = 9.0F;
    static final float SINGLE_MIN_INTERVAL = 1.8F;
    static final float SINGLE_MAX_INTERVAL = 4.5F;
    static final float SHOWER_MIN_INTERVAL = 16.0F;
    static final float SHOWER_MAX_INTERVAL = 24.0F;
    static final int SHOWER_MIN_METEORS = 7;
    static final int SHOWER_MAX_METEORS = 11;
    static final float SHOWER_MIN_SPACING = 0.12F;
    static final float SHOWER_MAX_SPACING = 0.4F;
    private static final float DEGREES = (float)(Math.PI / 180.0);
    /** Height of the mockup stage, the meteor start heights are relative to it. */
    private static final float STAGE_HEIGHT = 270.0F;

    final Meteor[] meteors = new Meteor[CAPACITY];
    int count = 0;
    private float nextSingle = FIRST_SINGLE_DELAY;
    private float nextShower = FIRST_SHOWER_DELAY;
    private int showerDirection = 1;
    private int showerLeft = 0;
    private float showerTimer = 0.0F;

    MeteorShower() {
        for (int i = 0; i < CAPACITY; i++) this.meteors[i] = new Meteor();
    }

    /**
     * Launches the meteors that are due, moves all of them and drops the ones that burned out.
     */
    void update(float deltaSeconds, @NotNull PixelRandom random, int screenWidth, int screenHeight) {
        if ((this.nextSingle -= deltaSeconds) <= 0.0F) {
            this.launch((random.next() < 0.5F) ? 1 : -1, random, screenWidth, screenHeight);
            this.nextSingle = random.range(SINGLE_MIN_INTERVAL, SINGLE_MAX_INTERVAL);
        }
        if (this.showerLeft > 0) {
            if ((this.showerTimer -= deltaSeconds) <= 0.0F) {
                this.launch(this.showerDirection, random, screenWidth, screenHeight);
                this.showerTimer = random.range(SHOWER_MIN_SPACING, SHOWER_MAX_SPACING);
                this.showerLeft--;
            }
        } else if ((this.nextShower -= deltaSeconds) <= 0.0F) {
            this.showerDirection = (random.next() < 0.5F) ? 1 : -1;
            this.showerLeft = random.rangeInt(SHOWER_MIN_METEORS, SHOWER_MAX_METEORS);
            // The first meteor of the shower launches with the next update
            this.showerTimer = 0.0F;
            this.nextShower = random.range(SHOWER_MIN_INTERVAL, SHOWER_MAX_INTERVAL);
        }
        int kept = 0;
        for (int i = 0; i < this.count; i++) {
            Meteor m = this.meteors[i];
            m.life += deltaSeconds;
            m.x += m.dirX * m.speed * deltaSeconds;
            m.y += m.dirY * m.speed * deltaSeconds;
            if (m.life > m.maxLife) continue;
            // Keeps the pool order stable while compacting it, so meteors keep their drawing order
            if (kept != i) {
                this.meteors[i] = this.meteors[kept];
                this.meteors[kept] = m;
            }
            kept++;
        }
        this.count = kept;
    }

    /**
     * Whether a shower is sending meteors right now.
     */
    boolean isShowerActive() {
        return this.showerLeft > 0;
    }

    int getShowerDirection() {
        return this.showerDirection;
    }

    /**
     * Starts a meteor that flies to the right ({@code direction} 1) or to the left (-1), falling at 18 to 38 degrees.
     */
    private void launch(int direction, @NotNull PixelRandom random, int screenWidth, int screenHeight) {
        if (this.count >= CAPACITY) return;
        Meteor m = this.meteors[this.count++];
        float slope = random.range(18.0F, 38.0F);
        float angle = ((direction > 0) ? slope : (180.0F - slope)) * DEGREES;
        m.dirX = (float)Math.cos(angle);
        m.dirY = (float)Math.sin(angle);
        m.x = (direction > 0) ? random.range(-20.0F, screenWidth * 0.7F) : random.range(screenWidth * 0.3F, screenWidth + 20.0F);
        m.y = random.range(-10.0F, 90.0F * screenHeight / STAGE_HEIGHT);
        m.speed = random.range(240.0F, 340.0F);
        m.life = 0.0F;
        m.maxLife = random.range(0.5F, 0.9F);
        m.length = random.rangeInt(18, 28);
    }

    static final class Meteor {

        /** Position of the head */
        float x;
        float y;
        /** Unit direction of flight */
        float dirX;
        float dirY;
        float speed;
        float life;
        float maxLife;
        /** Full trail length in pixels */
        int length;

        /**
         * The trail length right now: it grows in over the first fifth of the life and shrinks over the last quarter.
         */
        int currentLength() {
            float f = this.life / this.maxLife;
            float envelope = (f < 0.2F) ? (f / 0.2F) : ((f > 0.75F) ? ((1.0F - f) / 0.25F) : 1.0F);
            return Math.round(this.length * envelope);
        }

        /**
         * Whether the meteor breaks up, which shows as two sparks next to its head.
         */
        boolean isBreakingUp() {
            return this.life / this.maxLife > 0.85F;
        }

    }

}
