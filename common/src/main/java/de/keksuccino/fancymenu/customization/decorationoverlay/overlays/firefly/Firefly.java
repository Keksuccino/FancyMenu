package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;

/**
 * One firefly: its flight, its rests on surfaces and its flashes.<br>
 * Fireflies flash like real ones: a quick flash, a slow fall through three brightness steps, then dark for a few seconds.
 * Some flash twice in a row.
 */
final class Firefly {

    static final int FLYING = 0;
    static final int LANDING = 1;
    static final int LANDED = 2;
    static final int TRAIL_LENGTH = 3;
    /** Seconds of a single and of a double flash */
    private static final float FLASH_SECONDS = 1.15F;
    private static final float DOUBLE_FLASH_SECONDS = 1.65F;

    float x;
    float y;
    /** Favorite spot inside of the group, relative to the group's center */
    float homeX;
    float homeY;
    /** Simple look only: a 2 px firefly instead of a 1 px one */
    boolean big;
    float heading;
    float speed;
    float wobbleFrequency;
    float wobblePhase;
    float nextFlash;
    /** Seconds since the current flash started, below 0 while dark */
    float flashTime = -1.0F;
    boolean doubleFlash;
    final float[] trailX = new float[TRAIL_LENGTH];
    final float[] trailY = new float[TRAIL_LENGTH];
    int trailCount = 0;
    float trailTimer = 0.0F;
    int state = FLYING;
    float restTime = 0.0F;
    float restDuration = 0.0F;
    float landingCooldown = 0.0F;
    float targetX;
    float targetY;
    /** True if the landing spot is on an element, false if it is on the floor */
    boolean targetOnElement;
    float landingSpeed;

    /**
     * Brightness during a flash: 2 on the rise, 3 at the peak, then 2 and 1 while it fades, 0 when dark.
     */
    static int flashProfile(float u) {
        if (u < 0.07F) return 2;
        if (u < 0.36F) return 3;
        if (u < 0.66F) return 2;
        if (u < 1.15F) return 1;
        return 0;
    }

    /**
     * The current brightness, from 0 (dark) to 3 (flash).
     */
    int level() {
        float t = this.flashTime;
        if (t < 0.0F) return 0;
        if (!this.doubleFlash) return flashProfile(t);
        if (t < 0.06F) return 2;
        if (t < 0.22F) return 3;
        if (t < 0.5F) return 1;
        return flashProfile(t - 0.5F);
    }

    /**
     * Advances the flash cycle. Returns true if a new flash started.
     *
     * @param mayDoubleFlash false for fireflies that never flash twice in a row (the far ones)
     */
    boolean advanceFlash(float deltaSeconds, @NotNull PixelRandom random, boolean mayDoubleFlash) {
        if (this.flashTime >= 0.0F) {
            this.flashTime += deltaSeconds;
            if (this.flashTime >= (this.doubleFlash ? DOUBLE_FLASH_SECONDS : FLASH_SECONDS)) {
                this.flashTime = -1.0F;
                this.nextFlash = random.range(2.2F, 5.5F);
                this.doubleFlash = mayDoubleFlash && random.chance(0.3F);
            }
            return false;
        }
        this.nextFlash -= deltaSeconds;
        if (this.nextFlash > 0.0F) return false;
        this.flashTime = 0.0F;
        return true;
    }

    /**
     * A flash nearby makes a dark firefly flash a bit sooner, which pulls a group into loose waves.
     */
    void nudge() {
        if ((this.flashTime < 0.0F) && (this.nextFlash > 0.4F)) this.nextFlash = Math.max(0.05F, this.nextFlash - 0.4F);
    }

    /**
     * Lazy loops around the home spot inside of the group.
     */
    void fly(float deltaSeconds, float time, float groupX, float groupY, float groupRadius, @NotNull PixelRandom random) {
        this.heading += ((float)Math.sin(time * this.wobbleFrequency + this.wobblePhase) * 1.1F + random.range(-1.0F, 1.0F) * 2.4F) * deltaSeconds;
        float toHomeX = groupX + this.homeX - this.x;
        float toHomeY = groupY + this.homeY - this.y;
        float homeDistance = (float)Math.sqrt(toHomeX * toHomeX + toHomeY * toHomeY);
        if (homeDistance > groupRadius * 0.6F) {
            float turn = (float)Math.atan2(toHomeY, toHomeX) - this.heading;
            turn = (float)Math.atan2(Math.sin(turn), Math.cos(turn));
            this.heading += turn * PixelMath.clamp((homeDistance - groupRadius * 0.6F) / groupRadius, 0.0F, 1.0F) * 3.0F * deltaSeconds;
        }
        this.x += (float)Math.cos(this.heading) * this.speed * deltaSeconds;
        this.y += (float)Math.sin(this.heading) * this.speed * 0.75F * deltaSeconds;
    }

    /**
     * Flies straight to the landing spot with a little sideways wobble. Returns true on arrival.
     */
    boolean approachTarget(float deltaSeconds, float time) {
        float dx = this.targetX - this.x;
        float dy = this.targetY - this.y;
        float distance = (float)Math.sqrt(dx * dx + dy * dy);
        float step = this.landingSpeed * deltaSeconds;
        if ((distance <= step) || (distance < 0.6F)) {
            this.x = this.targetX;
            this.y = this.targetY;
            return true;
        }
        this.x += dx / distance * step + (float)Math.sin(time * 5.0F + this.wobblePhase) * 4.0F * deltaSeconds;
        this.y += dy / distance * step;
        return false;
    }

    /**
     * Adds the current position to the front of the trail, a few times per second.
     */
    void recordTrail(float deltaSeconds) {
        this.trailTimer += deltaSeconds;
        if (this.trailTimer <= 0.07F) return;
        this.trailTimer = 0.0F;
        for (int i = TRAIL_LENGTH - 1; i > 0; i--) {
            this.trailX[i] = this.trailX[i - 1];
            this.trailY[i] = this.trailY[i - 1];
        }
        this.trailX[0] = this.x;
        this.trailY[0] = this.y;
        this.trailCount = Math.min(TRAIL_LENGTH, this.trailCount + 1);
    }

}
