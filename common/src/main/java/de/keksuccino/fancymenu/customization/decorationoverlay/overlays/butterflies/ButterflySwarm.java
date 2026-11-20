package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.butterflies;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreaturePool;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureScene;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureSimulation;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPointer;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Butterflies in five wing patterns that wander in loops with a bobbing flight, land on elements and the floor and slowly open and
 * close their wings there, and shy away from the pointer. A few small ones flutter far behind the elements.
 * The port of the mockup's {@code butterflyNew}.
 */
final class ButterflySwarm extends CreatureSimulation {

    static final PixelSprite OPEN = PixelSprite.of(
            "ww...ww",
            "wiwbwiw",
            "wwwbwww",
            ".wwbww.",
            ".w...w.");
    static final PixelSprite HALF = PixelSprite.of(
            ".w...w.",
            ".wibiw.",
            ".wwbww.",
            "..wbw..",
            ".......");
    static final PixelSprite SHUT = PixelSprite.of(
            "...w...",
            "...w...",
            "...b...",
            "...b...",
            ".......");
    static final PixelSprite FAR_OPEN = PixelSprite.of(
            "w.w",
            ".b.");
    static final PixelSprite FAR_SHUT = PixelSprite.of(
            ".w.",
            ".b.");
    /** Wing, inner wing and body colors of the five patterns: monarch, blue morpho, brimstone, cabbage white, pink. */
    static final PixelPalette[] PATTERNS = {
            pattern(0xF08A2A, 0xFFD27A, 0x2A1A12),
            pattern(0x3E8CF0, 0xA8D8FF, 0x14203A),
            pattern(0xF6D84A, 0xFFF6B0, 0x3A2E12),
            pattern(0xF4F6FF, 0xC9D4EA, 0x3A3A44),
            pattern(0xF07AB8, 0xFFC4E0, 0x3A1A2E)
    };
    private static final PixelSprite[] FLY_FRAMES = {OPEN, HALF, SHUT, HALF};
    /** Resting butterflies keep their wings open a bit longer than they keep them closed. */
    private static final PixelSprite[] SIT_FRAMES = {OPEN, OPEN, HALF, SHUT, HALF};
    static final int SPRITE_WIDTH = 7;
    static final int SPRITE_HEIGHT = 5;
    /** Butterflies at the normal amount on the mockup stage */
    static final float NEAR_COUNT = 9.0F;
    static final float FAR_COUNT = 7.0F;
    /** Distance to the screen edges at which a butterfly turns back to the middle */
    private static final float EDGE_MARGIN_X = 24.0F;
    private static final float EDGE_MARGIN_TOP = 20.0F;
    private static final float EDGE_MARGIN_BOTTOM = 40.0F;

    static final int FLYING = 0;
    static final int LANDING = 1;
    static final int RESTING = 2;

    private final CreaturePool<Butterfly> near = new CreaturePool<>(Butterfly::new);
    private final CreaturePool<FarButterfly> far = new CreaturePool<>(FarButterfly::new);
    private final CreatureScene.Spot spot = new CreatureScene.Spot();

    ButterflySwarm(@NotNull CreatureScene scene) {
        super(scene);
    }

    @NotNull
    private static PixelPalette pattern(int wing, int inner, int body) {
        return PixelPalette.builder().rgb('w', wing).rgb('i', inner).rgb('b', body).build();
    }

    /**
     * How many near butterflies there are at the scene's amount and screen size, at least one.
     */
    int wantedNear() {
        return Math.max(1, Math.round(NEAR_COUNT * this.scene.amount() * this.scene.density()));
    }

    int wantedFar() {
        return Math.round(FAR_COUNT * this.scene.amount() * this.scene.density());
    }

    @Override
    public void update(float dt) {
        CreatureScene s = this.scene;
        int n = s.scale();
        int rest = restOffset(n);
        float shy = 38.0F + 8.0F * n;
        float w = s.width();
        float h = s.height();
        float time = s.time();
        PixelPointer pointer = s.pointer();

        // The amount can change while the overlay runs: new butterflies appear somewhere, surplus ones are gone
        while (this.near.size() < this.wantedNear()) this.spawn(this.near.add());
        this.near.truncate(this.wantedNear());
        while (this.far.size() < this.wantedFar()) this.spawnFar(this.far.add());
        this.far.truncate(this.wantedFar());

        float farTop = s.stageY(20.0F);
        float farBottom = s.stageY(210.0F);
        for (int i = 0; i < this.far.size(); i++) {
            FarButterfly f = this.far.get(i);
            f.heading += s.jitter(4.0F, dt);
            f.x += (float)Math.cos(f.heading) * 9.0F * dt;
            f.y += (float)Math.sin(f.heading) * 6.0F * dt;
            if (f.x < -4.0F) f.x += w + 8.0F;
            else if (f.x > w + 4.0F) f.x -= w + 8.0F;
            f.y = PixelMath.clamp(f.y, farTop, farBottom);
        }

        boolean surfacesChanged = s.surfacesChanged();
        for (int i = 0; i < this.near.size(); i++) {
            Butterfly f = this.near.get(i);
            f.cooldown = Math.max(0.0F, f.cooldown - dt);
            // An element that moved or went away can't hold a butterfly anymore
            if (surfacesChanged && (f.state != FLYING) && !s.isSurfaceLive(f.surface)) {
                if (f.state == RESTING) this.takeOff(f);
                else f.state = FLYING;
            }
            float mdx = f.x - pointer.x();
            float mdy = f.y + 2.0F * n - pointer.y();
            boolean scared = pointer.isInside() && (mdx * mdx + mdy * mdy < shy * shy);
            if (f.state == RESTING) {
                f.y = f.targetY - rest;
                f.restTime += dt;
                if (scared || (f.restTime >= f.restDuration)) this.takeOff(f);
                continue;
            }
            float speed = f.speed;
            if (scared) {
                f.heading = turnTo(f.heading, (float)Math.atan2(mdy, mdx), 6.0F * dt);
                speed *= 1.9F;
                f.state = FLYING;
            } else if (f.state == LANDING) {
                float ex = f.targetX - f.x;
                float ey = f.targetY - rest - f.y;
                float distance = (float)Math.sqrt(ex * ex + ey * ey);
                if (distance < 1.5F) {
                    f.x = f.targetX;
                    f.y = f.targetY - rest;
                    f.state = RESTING;
                    f.restTime = 0.0F;
                    f.restDuration = s.random().range(3.0F, 8.0F);
                    continue;
                }
                // Vertical motion is slower (see below), so aim as if the target were further away vertically
                f.heading = turnTo(f.heading, (float)Math.atan2(ey / 0.8F, ex), 7.0F * dt);
                speed = Math.min(speed, distance * 2.0F + 6.0F);
            } else {
                f.heading += (float)Math.sin(time * f.wanderFrequency + f.wanderPhase) * 1.6F * dt + s.jitter(3.0F, dt);
                if ((f.x < EDGE_MARGIN_X) || (f.x > w - EDGE_MARGIN_X) || (f.y < EDGE_MARGIN_TOP) || (f.y > h - EDGE_MARGIN_BOTTOM)) {
                    f.heading = turnTo(f.heading, (float)Math.atan2(h * 0.45F - f.y, w * 0.5F - f.x), 2.5F * dt);
                }
                if ((f.cooldown <= 0.0F) && s.random().chance(0.25F * dt) && s.pickSpot(4.0F * n, 4.0F * n, SPRITE_HEIGHT * n, this.spot)) {
                    f.surface = this.spot.surface;
                    f.targetX = this.spot.x;
                    f.targetY = this.spot.y;
                    f.state = LANDING;
                }
            }
            f.x += (float)Math.cos(f.heading) * speed * dt;
            f.y += (float)Math.sin(f.heading) * speed * 0.8F * dt;
        }
    }

    /**
     * How far above its resting row the top of a resting butterfly is.
     */
    static int restOffset(int scale) {
        return SPRITE_HEIGHT * scale - 1;
    }

    private void spawn(@NotNull Butterfly f) {
        CreatureScene s = this.scene;
        f.x = s.range(30.0F, s.width() - 30.0F);
        f.y = s.range(30.0F, s.height() - 60.0F);
        f.heading = s.random().range(0.0F, PixelMath.TAU);
        f.speed = s.random().range(16.0F, 28.0F);
        f.pattern = s.random().rangeInt(0, PATTERNS.length - 1);
        f.frameOffset = s.random().range(0.0F, 9.0F);
        f.wanderFrequency = s.random().range(0.7F, 1.5F);
        f.wanderPhase = s.random().range(0.0F, PixelMath.TAU);
        f.state = FLYING;
        f.surface = null;
        f.targetX = 0;
        f.targetY = 0;
        f.restTime = 0.0F;
        f.restDuration = 0.0F;
        f.cooldown = s.random().range(2.0F, 9.0F);
    }

    private void spawnFar(@NotNull FarButterfly f) {
        CreatureScene s = this.scene;
        f.x = s.random().range(0.0F, s.width());
        f.y = s.range(s.stageY(30.0F), s.stageY(200.0F));
        f.heading = s.random().range(0.0F, PixelMath.TAU);
        f.pattern = s.random().rangeInt(0, PATTERNS.length - 1);
        f.frameOffset = s.random().range(0.0F, 9.0F);
    }

    private void takeOff(@NotNull Butterfly f) {
        f.state = FLYING;
        f.surface = null;
        f.cooldown = this.scene.random().range(4.0F, 10.0F);
        f.heading = -PixelMath.TAU / 4.0F + this.scene.random().range(-0.9F, 0.9F);
    }

    /**
     * Turns an angle towards another one by a fraction of the difference, always the short way round (the mockup's {@code turnTo}).
     */
    static float turnTo(float heading, float target, float fraction) {
        float difference = target - heading;
        return heading + (float)Math.atan2(Math.sin(difference), Math.cos(difference)) * fraction;
    }

    @Override
    public void drawBack(@NotNull PixelGraphics graphics) {
        int n = this.scene.scale();
        float time = this.scene.time();
        for (int i = 0; i < this.far.size(); i++) {
            FarButterfly f = this.far.get(i);
            PixelSprite frame = ((PixelMath.floor(time * 8.0F + f.frameOffset) & 1) != 0) ? FAR_OPEN : FAR_SHUT;
            this.sprite(graphics, frame, PATTERNS[f.pattern], PixelMath.floor(f.x), PixelMath.floor(f.y), false, n);
        }
    }

    /**
     * Draws the butterflies resting on an element. They belong to the element, so they always stay in front of it.
     */
    @Override
    public void drawElement(@NotNull PixelGraphics graphics) {
        for (int i = 0; i < this.near.size(); i++) {
            Butterfly f = this.near.get(i);
            if (isElementContent(f)) this.drawButterfly(graphics, f);
        }
    }

    /**
     * Draws the flying butterflies and the ones resting on the floor, which is screen-wide content like the flying ones.
     */
    @Override
    public void drawFront(@NotNull PixelGraphics graphics) {
        for (int i = 0; i < this.near.size(); i++) {
            Butterfly f = this.near.get(i);
            if (!isElementContent(f)) this.drawButterfly(graphics, f);
        }
    }

    /**
     * Whether a butterfly belongs to an element: while it rests on one. Butterflies resting on the floor don't.
     */
    static boolean isElementContent(@NotNull Butterfly f) {
        return (f.state == RESTING) && (f.surface != null) && !f.surface.isFloor();
    }

    private void drawButterfly(@NotNull PixelGraphics graphics, @NotNull Butterfly f) {
        int n = this.scene.scale();
        float time = this.scene.time();
        int x = PixelMath.floor(f.x) - ((SPRITE_WIDTH * n) >> 1);
        if (f.state == RESTING) {
            PixelSprite frame = SIT_FRAMES[PixelMath.floor(time * 2.0F + f.frameOffset) % SIT_FRAMES.length];
            this.sprite(graphics, frame, PATTERNS[f.pattern], x, f.targetY - restOffset(n), false, n);
        } else {
            PixelSprite frame = FLY_FRAMES[PixelMath.floor(time * 12.0F + f.frameOffset) & 3];
            int bob = Math.round((float)Math.sin(time * 9.0F + f.frameOffset) * 1.2F * n);
            this.sprite(graphics, frame, PATTERNS[f.pattern], x, PixelMath.floor(f.y) + bob, false, n);
        }
    }

    int nearCount() {
        return this.near.size();
    }

    int farCount() {
        return this.far.size();
    }

    @NotNull
    Butterfly nearButterfly(int index) {
        return this.near.get(index);
    }

    /**
     * A butterfly that flies in front of the elements and lands on them. Its position is the top middle of its sprite.
     */
    static final class Butterfly {

        float x;
        float y;
        float heading;
        float speed;
        int pattern;
        float frameOffset;
        float wanderFrequency;
        float wanderPhase;
        int state;
        @Nullable
        PixelSurfaces.Surface<?> surface;
        int targetX;
        int targetY;
        float restTime;
        float restDuration;
        /** Seconds until it may land again */
        float cooldown;

    }

    /**
     * A small butterfly far behind the elements.
     */
    static final class FarButterfly {

        float x;
        float y;
        float heading;
        int pattern;
        float frameOffset;

    }

}
