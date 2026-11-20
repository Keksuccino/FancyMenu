package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import org.jetbrains.annotations.NotNull;

/**
 * The simulation of the Fireworks overlay, in art pixels: rockets climb from below the screen with a trail of cooling sparks and burst in
 * the upper half of the screen into one of six shells. Every star of a shell flies on its own, slows down, sags and burns out.<br>
 * Stars are spaced evenly around the shell, so rings, hearts and five-point stars read as clean shapes. All particles live in fixed pools
 * that get reused, so a running show allocates nothing.
 */
final class FireworksShow {

    static final int SHAPE_DOUBLE_RING = 0;
    static final int SHAPE_RING_WITH_CORE = 1;
    static final int SHAPE_WILLOW = 2;
    static final int SHAPE_HEART = 3;
    static final int SHAPE_STAR = 4;
    static final int SHAPE_GLITTER = 5;
    static final int SHAPE_COUNT = 6;
    /** The mockup launches 0.8 rockets per second on its 480 pixel wide stage */
    static final float LAUNCHES_PER_SECOND = 0.8F;
    static final float STAGE_WIDTH = 480.0F;
    static final int MAX_ROCKETS = 64;
    static final int MAX_SPARKS = 2048;
    static final int MAX_STARS = 8192;
    static final int MAX_FLASHES = 64;
    static final float FLASH_LIFE = 0.11F;
    static final float FLASH_DISC_TIME = 0.05F;
    private static final float SPARK_INTERVAL = 0.02F;

    final Rocket[] rockets = new Rocket[MAX_ROCKETS];
    int rocketCount = 0;
    final Spark[] sparks = new Spark[MAX_SPARKS];
    int sparkCount = 0;
    final Star[] stars = new Star[MAX_STARS];
    int starCount = 0;
    final Flash[] flashes = new Flash[MAX_FLASHES];
    int flashCount = 0;
    private float launchTimer = 1.2F;

    /**
     * Advances the show by one frame.
     *
     * @param launchRate rockets per second
     * @param burstScale how big the shells burst, 1 is the normal size
     * @param emitSparks whether climbing rockets leave a spark trail
     */
    void update(float deltaSeconds, @NotNull PixelRandom random, int artWidth, int artHeight, float launchRate, float burstScale, boolean emitSparks) {
        if (launchRate > 0.0F) {
            float interval = 1.0F / launchRate;
            this.launchTimer += deltaSeconds;
            while (this.launchTimer >= interval) {
                this.launchTimer -= interval;
                this.launch(random, artWidth, artHeight);
            }
        }
        int kept = 0;
        for (int i = 0; i < this.rocketCount; i++) {
            Rocket rocket = this.rockets[i];
            rocket.age += deltaSeconds;
            float progress = rocket.age / rocket.duration;
            if (progress >= 1.0F) {
                this.burst(rocket.x, rocket.y, random.index(SHAPE_COUNT), random, burstScale);
                continue;
            }
            float eased = 1.0F - (float)Math.pow(1.0F - progress, 2.2F);
            rocket.x = PixelMath.lerp(rocket.startX, rocket.targetX, eased) + (float)Math.sin(eased * 9.0F + rocket.phase) * 1.5F * (1.0F - eased);
            rocket.y = PixelMath.lerp(rocket.startY, rocket.targetY, eased);
            if ((rocket.emitTimer += deltaSeconds) > SPARK_INTERVAL) {
                rocket.emitTimer = 0.0F;
                if (emitSparks) this.emitSpark(rocket, random);
            }
            // Moves the living ones to the front and keeps the dead objects behind them for reuse
            this.rockets[i] = this.rockets[kept];
            this.rockets[kept++] = rocket;
        }
        this.rocketCount = kept;
        kept = 0;
        for (int i = 0; i < this.sparkCount; i++) {
            Spark spark = this.sparks[i];
            spark.life += deltaSeconds;
            spark.x += spark.vx * deltaSeconds;
            spark.y += spark.vy * deltaSeconds;
            if (spark.life >= spark.maxLife) continue;
            this.sparks[i] = this.sparks[kept];
            this.sparks[kept++] = spark;
        }
        this.sparkCount = kept;
        kept = 0;
        for (int i = 0; i < this.starCount; i++) {
            Star star = this.stars[i];
            star.life += deltaSeconds;
            if (star.life >= star.maxLife) continue;
            float damping = (float)Math.exp(-star.drag * deltaSeconds);
            star.vx *= damping;
            star.vy = star.vy * damping + star.gravity * deltaSeconds;
            star.x += star.vx * deltaSeconds;
            star.y += star.vy * deltaSeconds;
            this.stars[i] = this.stars[kept];
            this.stars[kept++] = star;
        }
        this.starCount = kept;
        kept = 0;
        for (int i = 0; i < this.flashCount; i++) {
            Flash flash = this.flashes[i];
            flash.age += deltaSeconds;
            if (flash.age > FLASH_LIFE) continue;
            this.flashes[i] = this.flashes[kept];
            this.flashes[kept++] = flash;
        }
        this.flashCount = kept;
    }

    /**
     * Removes everything, for a new screen size.
     */
    void clear() {
        this.rocketCount = 0;
        this.sparkCount = 0;
        this.starCount = 0;
        this.flashCount = 0;
    }

    /**
     * Sends up a rocket from below the screen, between 15 and 85 percent of its width. It bursts between 12 and 50 percent of the height,
     * so the shells stay in the upper half of the screen.
     */
    void launch(@NotNull PixelRandom random, int artWidth, int artHeight) {
        if (this.rocketCount >= MAX_ROCKETS) return;
        Rocket rocket = this.rockets[this.rocketCount];
        if (rocket == null) this.rockets[this.rocketCount] = rocket = new Rocket();
        this.rocketCount++;
        rocket.startX = random.range(artWidth * 0.15F, artWidth * 0.85F);
        rocket.startY = artHeight + 4.0F;
        rocket.targetX = rocket.startX + random.range(-40.0F, 40.0F);
        rocket.targetY = random.range(artHeight * 0.12F, artHeight * 0.5F);
        rocket.age = 0.0F;
        rocket.duration = random.range(0.9F, 1.4F);
        rocket.x = rocket.startX;
        rocket.y = rocket.startY;
        rocket.emitTimer = 0.0F;
        rocket.phase = random.range(0.0F, PixelMath.TAU);
    }

    private void emitSpark(@NotNull Rocket rocket, @NotNull PixelRandom random) {
        if (this.sparkCount >= MAX_SPARKS) return;
        Spark spark = this.sparks[this.sparkCount];
        if (spark == null) this.sparks[this.sparkCount] = spark = new Spark();
        this.sparkCount++;
        spark.x = rocket.x + random.range(-0.6F, 0.6F);
        spark.y = rocket.y + 2.0F;
        spark.vx = random.range(-7.0F, 7.0F);
        spark.vy = random.range(6.0F, 24.0F);
        spark.life = 0.0F;
        spark.maxLife = random.range(0.25F, 0.5F);
    }

    /**
     * Bursts a shell: a two-frame flash and the stars of one of the six shapes, with a random ramp.
     */
    void burst(float x, float y, int shape, @NotNull PixelRandom random, float burstScale) {
        int ramp = random.index(FireworksArt.RAMPS.length);
        int secondRamp = random.index(FireworksArt.RAMPS.length);
        float v = random.range(95.0F, 135.0F) * burstScale;
        if (this.flashCount < MAX_FLASHES) {
            Flash flash = this.flashes[this.flashCount];
            if (flash == null) this.flashes[this.flashCount] = flash = new Flash();
            this.flashCount++;
            flash.x = x;
            flash.y = y;
            flash.age = 0.0F;
            flash.ramp = ramp;
        }
        switch (shape) {
            case SHAPE_DOUBLE_RING -> {
                this.ring(x, y, 26, v, ramp, 0.04F, random, -1.0F, StarStyle.NORMAL);
                this.ring(x, y, 14, v * 0.58F, secondRamp, 0.04F, random, -1.0F, StarStyle.NORMAL);
            }
            case SHAPE_RING_WITH_CORE -> {
                this.ring(x, y, 28, v, ramp, 0.01F, random, -1.0F, StarStyle.NORMAL);
                this.ring(x, y, 9, v * 0.3F, FireworksArt.GOLD, 0.04F, random, 0.9F, StarStyle.NORMAL);
            }
            case SHAPE_WILLOW -> {
                // Willow: three gold rings on long, drooping trails, the outer one glitters out
                for (int k = 0; k < 3; k++) {
                    float speed = v * random.range(0.5F, 0.7F) * (1.0F - k * 0.18F);
                    float life = random.range(2.0F, 2.6F);
                    this.ring(x, y, 12, speed, FireworksArt.GOLD, 0.2F, random, life, (k == 0) ? StarStyle.WILLOW_GLITTER : StarStyle.WILLOW);
                }
            }
            case SHAPE_HEART -> {
                int heartRamp = random.chance(0.5F) ? FireworksArt.RED : FireworksArt.PINK;
                // The heart curve itself, not normalized, so the shape survives the burst
                for (int i = 0; i < 36; i++) {
                    float t = i / 36.0F * PixelMath.TAU;
                    float s = (float)Math.sin(t);
                    float hx = 16.0F * s * s * s;
                    float hy = 13.0F * (float)Math.cos(t) - 5.0F * (float)Math.cos(2.0F * t) - 2.0F * (float)Math.cos(3.0F * t) - (float)Math.cos(4.0F * t);
                    this.star(x, y, hx / 17.0F * v, -hy / 17.0F * v, heartRamp, random, -1.0F, StarStyle.NORMAL);
                }
            }
            case SHAPE_STAR -> {
                // A five-point star: ten corners alternating between the full and 42 percent radius, four stars along every edge
                for (int k = 0; k < 10; k++) {
                    float a = -PixelMath.TAU / 4.0F + k * PixelMath.TAU / 10.0F;
                    float b = -PixelMath.TAU / 4.0F + ((k + 1) % 10) * PixelMath.TAU / 10.0F;
                    float ra = ((k & 1) == 1) ? 0.42F : 1.0F;
                    float rb = (((k + 1) % 10 & 1) == 1) ? 0.42F : 1.0F;
                    float ax = (float)Math.cos(a) * ra;
                    float ay = (float)Math.sin(a) * ra;
                    float bx = (float)Math.cos(b) * rb;
                    float by = (float)Math.sin(b) * rb;
                    for (int j = 0; j < 4; j++) {
                        float f = j / 4.0F;
                        this.star(x, y, PixelMath.lerp(ax, bx, f) * v, PixelMath.lerp(ay, by, f) * v, ramp, random, -1.0F, StarStyle.NORMAL);
                    }
                }
                this.ring(x, y, 8, v * 0.2F, FireworksArt.GOLD, 0.04F, random, 0.9F, StarStyle.NORMAL);
            }
            default -> {
                // Glitter: one ring of stars at random speeds that end in blinking sparkles
                float a0 = random.range(0.0F, PixelMath.TAU);
                for (int i = 0; i < 44; i++) {
                    float a = a0 + i / 44.0F * PixelMath.TAU;
                    float speed = v * random.range(0.55F, 1.0F);
                    this.star(x, y, (float)Math.cos(a) * speed, (float)Math.sin(a) * speed, ramp, random, random.range(1.3F, 1.9F), StarStyle.GLITTER);
                }
            }
        }
    }

    /**
     * Stars spaced evenly around a ring, every one with a little random jitter of its angle.
     *
     * @param life the life of all its stars, or -1 for a random life per star
     */
    private void ring(float x, float y, int count, float speed, int ramp, float jitter, @NotNull PixelRandom random, float life, @NotNull StarStyle style) {
        float a0 = random.range(0.0F, PixelMath.TAU);
        for (int i = 0; i < count; i++) {
            float a = a0 + i / (float)count * PixelMath.TAU + random.range(-jitter, jitter);
            this.star(x, y, (float)Math.cos(a) * speed, (float)Math.sin(a) * speed, ramp, random, life, style);
        }
    }

    private void star(float x, float y, float vx, float vy, int ramp, @NotNull PixelRandom random, float life, @NotNull StarStyle style) {
        if (this.starCount >= MAX_STARS) return;
        Star star = this.stars[this.starCount];
        if (star == null) this.stars[this.starCount] = star = new Star();
        this.starCount++;
        star.x = x;
        star.y = y;
        star.vx = vx;
        star.vy = vy;
        star.life = 0.0F;
        star.maxLife = (life > 0.0F) ? life : random.range(1.1F, 1.7F);
        star.ramp = ramp;
        star.glitter = style.glitter;
        star.drag = style.drag;
        star.gravity = style.gravity;
        star.trail = style.trail;
        star.phase = random.range(0.0F, PixelMath.TAU);
    }

    /**
     * How the stars of a shell fly: normal stars, the slow and drooping willow stars with long trails, and stars that glitter out.
     */
    enum StarStyle {

        NORMAL(false, 1.7F, 24.0F, 0.085F),
        GLITTER(true, 1.7F, 24.0F, 0.085F),
        WILLOW(false, 1.1F, 34.0F, 0.2F),
        WILLOW_GLITTER(true, 1.1F, 34.0F, 0.2F);

        final boolean glitter;
        final float drag;
        final float gravity;
        /** How many seconds of flight the streak behind a star shows */
        final float trail;

        StarStyle(boolean glitter, float drag, float gravity, float trail) {
            this.glitter = glitter;
            this.drag = drag;
            this.gravity = gravity;
            this.trail = trail;
        }

    }

    static final class Rocket {

        float startX;
        float startY;
        float targetX;
        float targetY;
        float age;
        float duration;
        float x;
        float y;
        float emitTimer;
        float phase;

    }

    static final class Spark {

        float x;
        float y;
        float vx;
        float vy;
        float life;
        float maxLife;

    }

    static final class Star {

        float x;
        float y;
        float vx;
        float vy;
        float life;
        float maxLife;
        int ramp;
        boolean glitter;
        float drag;
        float gravity;
        float trail;
        float phase;

    }

    static final class Flash {

        float x;
        float y;
        float age;
        int ramp;

    }

}
