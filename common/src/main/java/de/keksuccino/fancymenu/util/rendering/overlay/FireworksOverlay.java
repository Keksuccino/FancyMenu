package de.keksuccino.fancymenu.util.rendering.overlay;

import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FireworksOverlay extends AbstractWidget implements NavigatableWidget {

    private static final float MIN_SCALE = 0.01F;
    private static final float MAX_SCALE = 5.0F;
    private static final float MIN_EXPLOSION_SCALE = 0.01F;
    private static final float MAX_EXPLOSION_SCALE = 5.0F;
    private static final float MIN_AMOUNT = 0.01F;
    private static final float MAX_AMOUNT = 5.0F;
    private static final float MAX_DELTA_SECONDS = 0.1F;

    private static final int AREA_PER_EXPLOSION = 300000;
    private static final float MIN_BASE_RATE = 0.12F;
    private static final float MAX_BASE_RATE = 0.85F;
    private static final float MIN_EFFECTIVE_RATE = 0.02F;
    private static final float MAX_EFFECTIVE_RATE = 6.0F;

    private static final float ROCKET_MIN_FLIGHT = 0.9F;
    private static final float ROCKET_MAX_FLIGHT = 2.2F;
    private static final float ROCKET_START_OFFSET_MIN = 12.0F;
    private static final float ROCKET_START_OFFSET_MAX = 38.0F;
    private static final float ROCKET_TARGET_TOP = 0.15F;
    private static final float ROCKET_TARGET_BOTTOM = 0.82F;
    private static final float ROCKET_SWAY_MIN = 1.5F;
    private static final float ROCKET_SWAY_MAX = 6.0F;
    private static final float ROCKET_SWAY_SPEED_MIN = 1.0F;
    private static final float ROCKET_SWAY_SPEED_MAX = 2.6F;

    private static final float MIN_EXPLOSION_SPEED = 70.0F;
    private static final float MAX_EXPLOSION_SPEED = 190.0F;
    private static final int MIN_PARTICLES = 36;
    private static final int MAX_PARTICLES = 140;
    private static final int MAX_PARTICLE_CAP = 260;
    private static final float MIN_PARTICLE_LIFE = 1.0F;
    private static final float MAX_PARTICLE_LIFE = 2.4F;
    private static final float MIN_PARTICLE_SIZE = 1.0F;
    private static final float MAX_PARTICLE_SIZE = 3.2F;
    private static final float GRAVITY = 38.0F;
    private static final float DRAG = 0.12F;

    private static final int[] ACCENT_COLORS = new int[] {
            0xFFFFFF, 0xFFF4C6, 0xFFE082, 0xFFF1A8
    };

    private static final int[] RAINBOW_PALETTE = new int[] {
            0xF44336, 0xFF9800, 0xFFEB3B, 0x4CAF50, 0x00BCD4, 0x2196F3, 0x9C27B0, 0xFF4081
    };

    private static final int[][] COLOR_PALETTES = new int[][] {
            new int[] {0xF44336, 0xFF9800},
            new int[] {0xFFEB3B, 0xFFC107},
            new int[] {0x4CAF50, 0x8BC34A},
            new int[] {0x2196F3, 0x03A9F4},
            new int[] {0x9C27B0, 0xE040FB},
            new int[] {0xFF4081, 0xF8BBD0},
            new int[] {0xFFFFFF, 0xFFD54F},
            new int[] {0x00BCD4, 0x009688, 0x4CAF50},
            new int[] {0xFFEB3B, 0xFF9800, 0xF44336},
            new int[] {0x90CAF9, 0x64B5F6, 0x42A5F5},
            new int[] {0xE1BEE7, 0xBA68C8, 0xCE93D8}
    };

    private final RandomSource random = RandomSource.create();
    private final List<Rocket> rockets = new ArrayList<>();
    private final List<Explosion> explosions = new ArrayList<>();
    private int lastWidth = -1;
    private int lastHeight = -1;
    private long lastUpdateMs = -1L;
    private float spawnTimer = 0.0F;
    private float scale = 1.0F;
    private float explosionScale = 1.0F;
    private float amountMultiplier = 1.0F;
    private boolean rocketTrailEnabled = true;

    public FireworksOverlay(int width, int height) {
        super(0, 0, width, height, Component.empty());
    }

    public void setScale(float scale) {
        this.scale = Mth.clamp(scale, MIN_SCALE, MAX_SCALE);
    }

    public float getScale() {
        return this.scale;
    }

    public void setExplosionScale(float explosionScale) {
        this.explosionScale = Mth.clamp(explosionScale, MIN_EXPLOSION_SCALE, MAX_EXPLOSION_SCALE);
    }

    public float getExplosionScale() {
        return this.explosionScale;
    }

    public void setAmountMultiplier(float amountMultiplier) {
        this.amountMultiplier = Mth.clamp(amountMultiplier, MIN_AMOUNT, MAX_AMOUNT);
    }

    public float getAmountMultiplier() {
        return this.amountMultiplier;
    }

    public void setRocketTrailEnabled(boolean rocketTrailEnabled) {
        this.rocketTrailEnabled = rocketTrailEnabled;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int overlayX = this.getX();
        int overlayY = this.getY();
        int overlayWidth = this.getWidth();
        int overlayHeight = this.getHeight();

        if (overlayWidth <= 0 || overlayHeight <= 0) {
            return;
        }

        boolean sizeChanged = overlayWidth != this.lastWidth || overlayHeight != this.lastHeight;
        if (sizeChanged) {
            this.lastWidth = overlayWidth;
            this.lastHeight = overlayHeight;
            this.rockets.clear();
            this.explosions.clear();
            this.spawnTimer = 0.0F;
        }

        float deltaSeconds = computeDeltaSeconds(sizeChanged);
        if (deltaSeconds > 0.0F) {
            updateSpawn(overlayWidth, overlayHeight, deltaSeconds);
            updateRockets(deltaSeconds);
            updateExplosions(deltaSeconds);
        }

        if (this.rocketTrailEnabled) {
            renderRockets(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
        }
        renderExplosions(graphics, overlayX, overlayY, overlayWidth, overlayHeight);
    }

    private void updateSpawn(int width, int height, float deltaSeconds) {
        float interval = getSpawnInterval(width, height);
        this.spawnTimer += deltaSeconds;
        while (this.spawnTimer >= interval) {
            this.spawnTimer -= interval;
            if (this.rockets.size() < 48) {
                this.rockets.add(createRocket(width, height));
            }
        }
    }

    private float getSpawnInterval(int width, int height) {
        float areaRate = (width * height) / (float)AREA_PER_EXPLOSION;
        float baseRate = Mth.clamp(areaRate, MIN_BASE_RATE, MAX_BASE_RATE);
        float effectiveRate = Mth.clamp(baseRate * this.amountMultiplier, MIN_EFFECTIVE_RATE, MAX_EFFECTIVE_RATE);
        return 1.0F / Math.max(0.001F, effectiveRate);
    }

    private void updateRockets(float deltaSeconds) {
        if (this.rockets.isEmpty()) {
            return;
        }
        Iterator<Rocket> iterator = this.rockets.iterator();
        while (iterator.hasNext()) {
            Rocket rocket = iterator.next();
            rocket.prevX = rocket.x;
            rocket.prevY = rocket.y;
            rocket.time += deltaSeconds;
            float t = rocket.time / rocket.flightTime;
            if (t >= 1.0F) {
                this.explosions.add(createExplosion(rocket.targetX, rocket.targetY));
                iterator.remove();
                continue;
            }
            float eased = 1.0F - (1.0F - t) * (1.0F - t);
            float sway = Mth.sin(eased * (float)Math.PI * rocket.swaySpeed + rocket.swayPhase) * rocket.swayAmplitude * (1.0F - t);
            rocket.x = Mth.lerp(eased, rocket.startX, rocket.targetX) + sway;
            rocket.y = Mth.lerp(eased, rocket.startY, rocket.targetY);
        }
    }

    private void updateExplosions(float deltaSeconds) {
        if (this.explosions.isEmpty()) {
            return;
        }
        Iterator<Explosion> iterator = this.explosions.iterator();
        while (iterator.hasNext()) {
            Explosion explosion = iterator.next();
            explosion.time += deltaSeconds;
            float drag = Math.max(0.0F, 1.0F - DRAG * deltaSeconds);
            Iterator<Particle> particleIterator = explosion.particles.iterator();
            while (particleIterator.hasNext()) {
                Particle particle = particleIterator.next();
                particle.life += deltaSeconds;
                if (particle.life >= particle.lifespan) {
                    particleIterator.remove();
                    continue;
                }
                particle.vy += GRAVITY * explosion.gravityScale * deltaSeconds;
                particle.vx *= drag;
                particle.vy *= drag;
                particle.x += particle.vx * deltaSeconds;
                particle.y += particle.vy * deltaSeconds;
            }
            if (explosion.particles.isEmpty() || explosion.time > explosion.duration) {
                iterator.remove();
            }
        }
    }

    private void renderRockets(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        int size = Math.max(1, Mth.ceil(2.0F * this.scale));
        int half = Math.max(1, size / 2);
        for (Rocket rocket : this.rockets) {
            float x1 = rocket.prevX;
            float y1 = rocket.prevY;
            float x2 = rocket.x;
            float y2 = rocket.y;
            int rgb = rocket.trailRgb;
            int alpha = 200;
            renderLine(graphics, overlayX, overlayY, overlayWidth, overlayHeight, x1, y1, x2, y2, alpha, rgb, Math.max(1, half));
            int renderX = overlayX + Mth.floor(x2);
            int renderY = overlayY + Mth.floor(y2);
            if (renderX + size <= overlayX || renderX >= overlayX + overlayWidth) {
                continue;
            }
            if (renderY + size <= overlayY || renderY >= overlayY + overlayHeight) {
                continue;
            }
            int color = (240 << 24) | rgb;
            graphics.fill(renderX, renderY, renderX + size, renderY + size, color);
        }
    }

    private void renderExplosions(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight) {
        if (this.explosions.isEmpty()) {
            return;
        }
        for (Explosion explosion : this.explosions) {
            for (Particle particle : explosion.particles) {
                float age = particle.life / particle.lifespan;
                if (age >= 1.0F) {
                    continue;
                }
                float fade = 1.0F - age;
                float flicker = particle.sparkle ? (0.45F + 0.55F * Mth.sin(particle.life * particle.flickerSpeed + particle.flickerPhase)) : 1.0F;
                int alpha = Mth.clamp(Mth.floor(particle.baseAlpha * fade * particle.brightness * flicker), 0, 255);
                if (alpha <= 0) {
                    continue;
                }
                int size = Math.max(1, Mth.ceil(particle.size * (0.75F + 0.25F * flicker)));
                int renderX = overlayX + Mth.floor(particle.x - size * 0.5F);
                int renderY = overlayY + Mth.floor(particle.y - size * 0.5F);
                if (renderX + size <= overlayX || renderX >= overlayX + overlayWidth) {
                    continue;
                }
                if (renderY + size <= overlayY || renderY >= overlayY + overlayHeight) {
                    continue;
                }
                int color = (alpha << 24) | particle.rgb;
                graphics.fill(renderX, renderY, renderX + size, renderY + size, color);
            }
        }
    }

    private void renderLine(GuiGraphics graphics, int overlayX, int overlayY, int overlayWidth, int overlayHeight, float x1, float y1, float x2, float y2, int alpha, int rgb, int thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float distance = Mth.sqrt(dx * dx + dy * dy);
        int steps = Mth.clamp(Mth.ceil(distance / 1.6F), 2, 16);
        float half = thickness * 0.5F;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float)steps;
            float x = x1 + dx * t;
            float y = y1 + dy * t;
            int renderX = overlayX + Mth.floor(x - half);
            int renderY = overlayY + Mth.floor(y - half);
            int size = Math.max(1, thickness);
            if (renderX + size <= overlayX || renderX >= overlayX + overlayWidth) {
                continue;
            }
            if (renderY + size <= overlayY || renderY >= overlayY + overlayHeight) {
                continue;
            }
            int color = (alpha << 24) | rgb;
            graphics.fill(renderX, renderY, renderX + size, renderY + size, color);
        }
    }

    private Rocket createRocket(int width, int height) {
        Rocket rocket = new Rocket();
        rocket.startX = this.random.nextFloat() * width;
        rocket.startY = height + nextRange(ROCKET_START_OFFSET_MIN, ROCKET_START_OFFSET_MAX) * this.scale;
        rocket.targetX = this.random.nextFloat() * width;
        rocket.targetY = nextRange(height * ROCKET_TARGET_TOP, height * ROCKET_TARGET_BOTTOM);
        rocket.flightTime = nextRange(ROCKET_MIN_FLIGHT, ROCKET_MAX_FLIGHT) * (0.8F + 0.2F * this.scale);
        rocket.swayAmplitude = nextRange(ROCKET_SWAY_MIN, ROCKET_SWAY_MAX) * this.scale;
        rocket.swaySpeed = nextRange(ROCKET_SWAY_SPEED_MIN, ROCKET_SWAY_SPEED_MAX);
        rocket.swayPhase = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        rocket.x = rocket.startX;
        rocket.y = rocket.startY;
        rocket.prevX = rocket.startX;
        rocket.prevY = rocket.startY;
        rocket.trailRgb = pickTrailColor();
        return rocket;
    }

    private Explosion createExplosion(float x, float y) {
        Explosion explosion = new Explosion();
        explosion.x = x;
        explosion.y = y;
        explosion.time = 0.0F;
        explosion.gravityScale = 0.8F + 0.4F * this.random.nextFloat();
        explosion.duration = nextRange(1.4F, 2.6F) * (0.8F + 0.2F * this.explosionScale);
        ExplosionShape shape = pickShape();
        int[] palette = pickPalette();

        float speedScale = Mth.clamp(this.scale * this.explosionScale, 0.2F, 8.0F);
        float sizeScale = Mth.clamp(this.scale * (0.65F + 0.35F * this.explosionScale), 0.2F, 6.0F);
        int baseCount = Mth.floor(nextRange(MIN_PARTICLES, MAX_PARTICLES));
        int count = Mth.clamp(Math.round(baseCount * (0.7F + 0.3F * this.explosionScale)), MIN_PARTICLES, MAX_PARTICLE_CAP);
        float baseSpeed = nextRange(MIN_EXPLOSION_SPEED, MAX_EXPLOSION_SPEED) * speedScale;
        float sparkleChance = 0.18F + 0.25F * this.random.nextFloat();

        switch (shape) {
            case BALL -> addRadialParticles(explosion, palette, count, baseSpeed * 0.65F, baseSpeed * 1.05F, sizeScale, sparkleChance * 0.6F);
            case RING -> addRadialParticles(explosion, palette, count, baseSpeed * 0.85F, baseSpeed * 1.0F, sizeScale, sparkleChance * 0.4F);
            case STAR -> addStarParticles(explosion, palette, count, baseSpeed, sizeScale, sparkleChance);
            case DOUBLE_RING -> addDoubleRingParticles(explosion, palette, count, baseSpeed, sizeScale, sparkleChance * 0.6F);
            case SPARK_BURST -> addSparkBurst(explosion, palette, count, baseSpeed, sizeScale);
            case FAN -> addFanParticles(explosion, palette, count, baseSpeed, sizeScale, sparkleChance);
            case HEART -> addHeartParticles(explosion, palette, count, baseSpeed, sizeScale, sparkleChance * 0.6F);
            case CRACKLE -> addCrackleParticles(explosion, palette, count, baseSpeed, sizeScale);
        }

        if (this.random.nextFloat() < 0.35F) {
            addSparkles(explosion, palette, Math.max(6, count / 6), baseSpeed * 1.25F, sizeScale * 0.6F, 0.55F);
        }

        return explosion;
    }

    private void addRadialParticles(Explosion explosion, int[] palette, int count, float minSpeed, float maxSpeed, float sizeScale, float sparkleChance) {
        for (int i = 0; i < count; i++) {
            float angle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            float speed = nextRange(minSpeed, maxSpeed);
            float vx = Mth.cos(angle) * speed;
            float vy = Mth.sin(angle) * speed;
            explosion.particles.add(createParticle(explosion.x, explosion.y, vx, vy, palette, sizeScale, sparkleChance));
        }
    }

    private void addStarParticles(Explosion explosion, int[] palette, int count, float baseSpeed, float sizeScale, float sparkleChance) {
        int points = 4 + this.random.nextInt(4);
        for (int i = 0; i < count; i++) {
            float angle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            float starFactor = 0.55F + 0.45F * Math.abs(Mth.sin(angle * points));
            float speed = baseSpeed * starFactor * nextRange(0.85F, 1.05F);
            float vx = Mth.cos(angle) * speed;
            float vy = Mth.sin(angle) * speed;
            explosion.particles.add(createParticle(explosion.x, explosion.y, vx, vy, palette, sizeScale, sparkleChance));
        }
    }

    private void addDoubleRingParticles(Explosion explosion, int[] palette, int count, float baseSpeed, float sizeScale, float sparkleChance) {
        int outerCount = Math.max(8, (int)(count * 0.55F));
        int innerCount = Math.max(6, count - outerCount);
        float outerSpeed = baseSpeed * nextRange(0.9F, 1.05F);
        float innerSpeed = baseSpeed * nextRange(0.55F, 0.75F);
        for (int i = 0; i < outerCount; i++) {
            float angle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            float vx = Mth.cos(angle) * outerSpeed;
            float vy = Mth.sin(angle) * outerSpeed;
            explosion.particles.add(createParticle(explosion.x, explosion.y, vx, vy, palette, sizeScale, sparkleChance));
        }
        for (int i = 0; i < innerCount; i++) {
            float angle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            float vx = Mth.cos(angle) * innerSpeed;
            float vy = Mth.sin(angle) * innerSpeed;
            explosion.particles.add(createParticle(explosion.x, explosion.y, vx, vy, palette, sizeScale * 0.85F, sparkleChance * 0.7F));
        }
    }

    private void addSparkBurst(Explosion explosion, int[] palette, int count, float baseSpeed, float sizeScale) {
        int mainCount = Math.max(10, (int)(count * 0.65F));
        addRadialParticles(explosion, palette, mainCount, baseSpeed * 0.7F, baseSpeed * 1.0F, sizeScale, 0.15F);
        addSparkles(explosion, palette, Math.max(10, count - mainCount), baseSpeed * 1.35F, sizeScale * 0.65F, 0.6F);
    }

    private void addFanParticles(Explosion explosion, int[] palette, int count, float baseSpeed, float sizeScale, float sparkleChance) {
        int arms = 4 + this.random.nextInt(4);
        float centerAngle = (float)Math.PI * 1.5F;
        float spread = (float)Math.PI * 0.75F;
        for (int i = 0; i < count; i++) {
            float armProgress = this.random.nextFloat();
            float armOffset = (this.random.nextInt(arms) / (float)Math.max(1, arms - 1)) - 0.5F;
            float angle = centerAngle + armOffset * spread + nextRange(-0.2F, 0.2F) * armProgress;
            float speed = baseSpeed * nextRange(0.65F, 1.05F);
            float vx = Mth.cos(angle) * speed;
            float vy = Mth.sin(angle) * speed;
            explosion.particles.add(createParticle(explosion.x, explosion.y, vx, vy, palette, sizeScale, sparkleChance));
        }
    }

    private void addHeartParticles(Explosion explosion, int[] palette, int count, float baseSpeed, float sizeScale, float sparkleChance) {
        for (int i = 0; i < count; i++) {
            float t = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            float sinT = Mth.sin(t);
            float cosT = Mth.cos(t);
            float x = 16.0F * sinT * sinT * sinT;
            float y = 13.0F * cosT - 5.0F * Mth.cos(2.0F * t) - 2.0F * Mth.cos(3.0F * t) - Mth.cos(4.0F * t);
            float len = Mth.sqrt(x * x + y * y);
            if (len < 0.001F) {
                len = 1.0F;
            }
            float dirX = x / len;
            float dirY = -y / len;
            float speed = baseSpeed * nextRange(0.65F, 0.95F);
            explosion.particles.add(createParticle(explosion.x, explosion.y, dirX * speed, dirY * speed, palette, sizeScale, sparkleChance));
        }
    }

    private void addCrackleParticles(Explosion explosion, int[] palette, int count, float baseSpeed, float sizeScale) {
        addRadialParticles(explosion, palette, Math.max(10, (int)(count * 0.7F)), baseSpeed * 0.6F, baseSpeed * 0.95F, sizeScale, 0.6F);
        addSparkles(explosion, palette, Math.max(10, count / 3), baseSpeed * 1.45F, sizeScale * 0.55F, 0.85F);
    }

    private void addSparkles(Explosion explosion, int[] palette, int count, float baseSpeed, float sizeScale, float sparkleChance) {
        for (int i = 0; i < count; i++) {
            float angle = this.random.nextFloat() * ((float)Math.PI * 2.0F);
            float speed = baseSpeed * nextRange(0.8F, 1.2F);
            float vx = Mth.cos(angle) * speed;
            float vy = Mth.sin(angle) * speed;
            Particle particle = createParticle(explosion.x, explosion.y, vx, vy, palette, sizeScale, sparkleChance);
            particle.lifespan *= nextRange(0.55F, 0.8F);
            particle.size *= 0.75F;
            explosion.particles.add(particle);
        }
    }

    private Particle createParticle(float x, float y, float vx, float vy, int[] palette, float sizeScale, float sparkleChance) {
        Particle particle = new Particle();
        particle.x = x;
        particle.y = y;
        particle.vx = vx;
        particle.vy = vy;
        particle.life = 0.0F;
        particle.lifespan = nextRange(MIN_PARTICLE_LIFE, MAX_PARTICLE_LIFE) * nextRange(0.85F, 1.15F);
        particle.size = nextRange(MIN_PARTICLE_SIZE, MAX_PARTICLE_SIZE) * sizeScale;
        particle.rgb = pickParticleColor(palette);
        particle.baseAlpha = resolveBaseAlpha(particle.rgb);
        particle.brightness = nextRange(0.75F, 1.15F);
        particle.sparkle = this.random.nextFloat() < sparkleChance;
        particle.flickerSpeed = nextRange(6.0F, 13.0F);
        particle.flickerPhase = this.random.nextFloat() * ((float)Math.PI * 2.0F);
        return particle;
    }

    private int pickParticleColor(int[] palette) {
        int rgb = palette[this.random.nextInt(palette.length)];
        if (this.random.nextFloat() < 0.08F) {
            rgb = ACCENT_COLORS[this.random.nextInt(ACCENT_COLORS.length)];
        }
        return varyColor(rgb, 0.22F);
    }

    private int pickTrailColor() {
        int base = (this.random.nextFloat() < 0.65F) ? 0xFFD27A : 0xFFFFFF;
        return varyColor(base, 0.15F);
    }

    private int[] pickPalette() {
        float roll = this.random.nextFloat();
        if (roll < 0.2F) {
            return new int[] { randomBrightColor() };
        }
        if (roll < 0.55F) {
            return COLOR_PALETTES[this.random.nextInt(COLOR_PALETTES.length)];
        }
        if (roll < 0.8F) {
            float baseHue = this.random.nextFloat();
            return new int[] {
                    Mth.hsvToRgb(baseHue, 0.9F, 1.0F),
                    Mth.hsvToRgb((baseHue + 0.08F) % 1.0F, 0.85F, 1.0F),
                    Mth.hsvToRgb((baseHue + 0.16F) % 1.0F, 0.8F, 1.0F)
            };
        }
        return RAINBOW_PALETTE;
    }

    private int randomBrightColor() {
        float hue = this.random.nextFloat();
        float saturation = 0.75F + this.random.nextFloat() * 0.25F;
        float value = 0.85F + this.random.nextFloat() * 0.15F;
        return Mth.hsvToRgb(hue, saturation, value);
    }

    private int varyColor(int rgb, float variance) {
        float factor = 1.0F + (this.random.nextFloat() * 2.0F - 1.0F) * variance;
        int r = Mth.clamp(Math.round(((rgb >> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp(Math.round(((rgb >> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp(Math.round((rgb & 0xFF) * factor), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private ExplosionShape pickShape() {
        float roll = this.random.nextFloat();
        if (roll < 0.18F) {
            return ExplosionShape.BALL;
        }
        if (roll < 0.32F) {
            return ExplosionShape.RING;
        }
        if (roll < 0.48F) {
            return ExplosionShape.STAR;
        }
        if (roll < 0.6F) {
            return ExplosionShape.DOUBLE_RING;
        }
        if (roll < 0.74F) {
            return ExplosionShape.SPARK_BURST;
        }
        if (roll < 0.84F) {
            return ExplosionShape.FAN;
        }
        if (roll < 0.93F) {
            return ExplosionShape.HEART;
        }
        return ExplosionShape.CRACKLE;
    }

    private float computeDeltaSeconds(boolean sizeChanged) {
        long now = System.currentTimeMillis();
        if (this.lastUpdateMs < 0L || sizeChanged) {
            this.lastUpdateMs = now;
            return 0.0F;
        }
        float deltaSeconds = (now - this.lastUpdateMs) / 1000.0F;
        if (deltaSeconds <= 0.0F) {
            return 0.0F;
        }
        if (deltaSeconds > MAX_DELTA_SECONDS) {
            deltaSeconds = MAX_DELTA_SECONDS;
        }
        this.lastUpdateMs = now;
        return deltaSeconds;
    }

    private int resolveBaseAlpha(int color) {
        int alpha = FastColor.ARGB32.alpha(color);
        if (alpha <= 0) {
            alpha = 235;
        }
        return Mth.clamp(alpha, 0, 255);
    }

    private float nextRange(float min, float max) {
        return min + this.random.nextFloat() * (max - min);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
    }

    private enum ExplosionShape {
        BALL,
        RING,
        STAR,
        DOUBLE_RING,
        SPARK_BURST,
        FAN,
        HEART,
        CRACKLE
    }

    private static final class Rocket {
        private float startX;
        private float startY;
        private float targetX;
        private float targetY;
        private float x;
        private float y;
        private float prevX;
        private float prevY;
        private float time;
        private float flightTime;
        private float swayAmplitude;
        private float swaySpeed;
        private float swayPhase;
        private int trailRgb;
    }

    private static final class Explosion {
        private float x;
        private float y;
        private float time;
        private float duration;
        private float gravityScale;
        private final List<Particle> particles = new ArrayList<>();
    }

    private static final class Particle {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float life;
        private float lifespan;
        private float size;
        private int rgb;
        private int baseAlpha;
        private float brightness;
        private boolean sparkle;
        private float flickerSpeed;
        private float flickerPhase;
    }

}
