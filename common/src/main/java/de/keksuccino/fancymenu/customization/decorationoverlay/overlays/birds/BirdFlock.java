package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.birds;

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
 * Small birds that fly in, land on elements and the floor, hop along them, peck and look around, and take off when the pointer
 * comes close, startling the birds next to them too. Four species share one sprite set. The port of the mockup's {@code birdNew}.
 */
final class BirdFlock extends CreatureSimulation {

    static final PixelSprite STAND = PixelSprite.of(
            "....hh.",
            "...hehk",
            ".bbbbb.",
            "tbbbww.",
            ".tbww..",
            "..l.l..");
    static final PixelSprite PECK = PixelSprite.of(
            ".......",
            ".bbbb..",
            "tbbbbhh",
            ".tbwweh",
            "..ww..k",
            "..l.l..");
    static final PixelSprite WINGS_UP = PixelSprite.of(
            "..gg...",
            ".ggghh.",
            "tbbbhek",
            ".bbww..",
            ".......");
    static final PixelSprite WINGS_DOWN = PixelSprite.of(
            ".......",
            "....hh.",
            "tbbbhek",
            ".gggw..",
            "..gg...");
    /** Head, body, breast, tail and wing colors of sparrow, robin, bluebird and cardinal. */
    static final PixelPalette[] SPECIES = {
            species(0x7A5638, 0x9A7450, 0xE6D6BE, 0x5A4030, 0x6A4A34),
            species(0x6A5A50, 0x7A6A5E, 0xE8683A, 0x4A3E38, 0x5A4C44),
            species(0x3E7ACC, 0x4E8EDC, 0xF0C8A0, 0x2A5AA0, 0x3468B4),
            species(0xD83A2E, 0xC83228, 0xE8604A, 0x8A1E1E, 0x9A2424)
    };
    static final int SPRITE_WIDTH = 7;
    static final int STAND_HEIGHT = 6;
    static final int FLIGHT_HEIGHT = 5;
    /** Birds at the normal amount on the mockup stage */
    static final float COUNT = 6.0F;
    /** Seconds of a hop and of a peck */
    static final float HOP_TIME = 0.16F;
    static final float PECK_TIME = 0.34F;
    /** Spots tried per landing until one is free */
    private static final int LANDING_TRIES = 10;
    /** Flight speed towards the landing spot, in GUI pixels per second */
    private static final float ARRIVAL_SPEED = 100.0F;

    static final int AWAY = 0;
    static final int ARRIVING = 1;
    static final int PERCHED = 2;
    static final int FLEEING = 3;

    static final int ACT_NONE = 0;
    static final int ACT_HOP = 1;
    static final int ACT_PECK = 2;

    private final CreaturePool<Bird> birds = new CreaturePool<>(Bird::new);
    private final CreatureScene.Spot spot = new CreatureScene.Spot();
    private int nextSpecies = 0;
    private boolean started = false;

    BirdFlock(@NotNull CreatureScene scene) {
        super(scene);
    }

    @NotNull
    private static PixelPalette species(int head, int body, int breast, int tail, int wing) {
        return PixelPalette.builder()
                .rgb('h', head).rgb('b', body).rgb('w', breast).rgb('t', tail).rgb('g', wing)
                .rgb('e', 0x1A1410).rgb('k', 0xE8A030).rgb('l', 0xB07840)
                .build();
    }

    /**
     * How many birds visit at the scene's amount and screen size, at least one.
     */
    int wanted() {
        return Math.max(1, Math.round(COUNT * this.scene.amount() * this.scene.density()));
    }

    @Override
    public void update(float dt) {
        CreatureScene s = this.scene;
        if (!this.started) {
            this.started = true;
            this.start();
        }
        int n = s.scale();
        float shy = 28.0F + 8.0F * n;
        PixelPointer pointer = s.pointer();
        int wanted = this.wanted();

        // Missing birds fly in. Surplus ones fly off and are dropped once they are gone.
        while (this.birds.size() < wanted) this.spawn(this.birds.add());
        for (int i = this.birds.size() - 1; i >= wanted; i--) {
            Bird b = this.birds.get(i);
            if (b.state == AWAY) this.birds.remove(i);
            else if (b.state == PERCHED) this.flee(b, b.x - b.dir);
        }

        boolean surfacesChanged = s.surfacesChanged();
        for (int i = 0; i < this.birds.size(); i++) {
            Bird b = this.birds.get(i);
            // A bird can't sit on or fly to an element that moved or went away
            if (surfacesChanged && ((b.state == ARRIVING) || (b.state == PERCHED)) && !s.isSurfaceLive(b.surface)) {
                this.flee(b, b.x - b.dir);
            }
            if (b.state == AWAY) {
                b.timer -= dt;
                if (b.timer <= 0.0F) this.land(b);
            } else if (b.state == ARRIVING) {
                b.progress += dt / b.duration;
                if (b.progress >= 1.0F) {
                    b.x = b.targetX;
                    b.y = b.targetY;
                    this.perch(b, s.random().range(6.0F, 14.0F), s.random().range(0.4F, 1.2F));
                } else {
                    float eased = 1.0F - (1.0F - b.progress) * (1.0F - b.progress);
                    b.x = PixelMath.lerp(b.startX, b.targetX, eased);
                    b.y = PixelMath.lerp(b.startY, b.targetY, eased) - (float)Math.sin(b.progress * Math.PI) * 16.0F;
                }
            } else if (b.state == PERCHED) {
                float pointerDistance = pointer.isInside() ? (float)Math.hypot(b.x - pointer.x(), b.y - 3.0F * n - pointer.y()) : Float.MAX_VALUE;
                boolean startled = pointerDistance < shy;
                if (startled || ((b.stay -= dt) <= 0.0F)) {
                    this.flee(b, startled ? pointer.x() : b.x - b.dir);
                    if (startled) this.startleNeighbors(b, shy + 10.0F, pointer.x());
                    continue;
                }
                this.act(b, dt, n);
            } else {
                b.vy -= 40.0F * dt;
                b.x += b.vx * dt;
                b.y += b.vy * dt;
                if ((b.y < -14.0F * n) || (b.x < -16.0F * n) || (b.x > s.width() + 16.0F * n)) {
                    b.state = AWAY;
                    b.surface = null;
                    b.timer = s.random().range(1.5F, 5.0F);
                }
            }
        }
    }

    /**
     * Two thirds of the first birds already sit somewhere when the overlay appears, the others fly in after a moment.
     */
    private void start() {
        CreatureScene s = this.scene;
        int wanted = this.wanted();
        while (this.birds.size() < wanted) this.spawn(this.birds.add());
        int perched = Math.round(this.birds.size() * 2.0F / 3.0F);
        for (int i = 0; i < perched; i++) {
            Bird b = this.birds.get(i);
            this.land(b);
            if (b.state != ARRIVING) continue;
            b.x = b.targetX;
            b.y = b.targetY;
            this.perch(b, s.random().range(4.0F, 12.0F), s.random().range(0.3F, 1.5F));
        }
    }

    private void spawn(@NotNull Bird b) {
        b.species = this.nextSpecies;
        this.nextSpecies = (this.nextSpecies + 1) % SPECIES.length;
        b.state = AWAY;
        b.timer = this.scene.random().range(0.2F, 3.0F);
        b.x = 0.0F;
        b.y = 0.0F;
        b.dir = 1;
        b.surface = null;
        b.startX = 0.0F;
        b.startY = 0.0F;
        b.targetX = 0;
        b.targetY = 0;
        b.progress = 0.0F;
        b.duration = 1.0F;
        b.act = ACT_NONE;
        b.actTimer = 0.0F;
        b.actTime = 0.0F;
        b.hopFromX = 0.0F;
        b.hopToX = 0.0F;
        b.stay = 0.0F;
        b.vx = 0.0F;
        b.vy = 0.0F;
    }

    /**
     * Picks a free spot and sends the bird there from the side of the screen it flies away from. Tries a few spots until one is
     * free, so birds do not land inside each other. If no spot works at all, the bird stays away a little longer.
     */
    private void land(@NotNull Bird b) {
        CreatureScene s = this.scene;
        int n = s.scale();
        boolean found = false;
        for (int k = 0; k < LANDING_TRIES; k++) {
            if (!s.pickSpot(4.0F * n + 2.0F, 4.0F * n + 4.0F, STAND_HEIGHT * n, this.spot)) continue;
            found = true;
            b.surface = this.spot.surface;
            b.targetX = this.spot.x;
            b.targetY = this.spot.y;
            if (this.isSpotFree(b, this.spot.surface, this.spot.x, n)) break;
        }
        if (!found) {
            b.timer = s.random().range(0.5F, 1.5F);
            return;
        }
        b.dir = s.random().sign();
        b.startX = (b.dir > 0) ? -14.0F * n : s.width() + 14.0F * n;
        b.startY = s.range(s.stageY(10.0F), s.stageY(110.0F));
        b.progress = 0.0F;
        b.duration = (float)Math.hypot(b.targetX - b.startX, b.targetY - b.startY) / ARRIVAL_SPEED;
        b.x = b.startX;
        b.y = b.startY;
        b.state = ARRIVING;
    }

    /**
     * Whether no other bird sits on or flies to a spot of the same surface closer than about one bird width.
     */
    private boolean isSpotFree(@NotNull Bird bird, @Nullable PixelSurfaces.Surface<?> surface, int x, int scale) {
        for (int i = 0; i < this.birds.size(); i++) {
            Bird o = this.birds.get(i);
            if ((o == bird) || (o.surface != surface)) continue;
            if ((o.state != ARRIVING) && (o.state != PERCHED)) continue;
            float otherX = (o.state == PERCHED) ? o.x : o.targetX;
            if (Math.abs(otherX - x) < 8.0F * scale + 2.0F) return false;
        }
        return true;
    }

    private void perch(@NotNull Bird b, float stay, float actTimer) {
        b.state = PERCHED;
        b.stay = stay;
        b.actTimer = actTimer;
        b.act = ACT_NONE;
    }

    /**
     * Sends a bird off, flying away from {@code fromX}.
     */
    private void flee(@NotNull Bird b, float fromX) {
        b.state = FLEEING;
        b.dir = (b.x < fromX) ? -1 : 1;
        b.vx = b.dir * this.scene.random().range(70.0F, 110.0F);
        b.vy = -this.scene.random().range(60.0F, 95.0F);
        b.act = ACT_NONE;
    }

    private void startleNeighbors(@NotNull Bird b, float radius, float fromX) {
        for (int i = 0; i < this.birds.size(); i++) {
            Bird o = this.birds.get(i);
            if ((o != b) && (o.state == PERCHED) && (Math.hypot(o.x - b.x, o.y - b.y) < radius)) this.flee(o, fromX);
        }
    }

    /**
     * What a perched bird does now and then: hop a few steps forward, peck, or turn around.
     */
    private void act(@NotNull Bird b, float dt, int n) {
        CreatureScene s = this.scene;
        if (b.act != ACT_NONE) {
            b.actTime += dt;
            if (b.actTime >= ((b.act == ACT_HOP) ? HOP_TIME : PECK_TIME)) {
                if (b.act == ACT_HOP) b.x = b.hopToX;
                b.act = ACT_NONE;
                b.actTimer = s.random().range(0.5F, 1.7F);
            }
            return;
        }
        if ((b.actTimer -= dt) > 0.0F) return;
        float r = s.random().next();
        if (r < 0.5F) {
            PixelSurfaces.Surface<?> surface = b.surface;
            b.act = ACT_HOP;
            b.actTime = 0.0F;
            b.hopFromX = b.x;
            float hopTo = b.x + b.dir * s.random().rangeInt(3, 7) * n;
            // Hops never leave the surface
            b.hopToX = (surface != null) ? clampTo(hopTo, surface.x() + 4.0F * n, surface.right() - 1.0F - 4.0F * n) : hopTo;
        } else if (r < 0.78F) {
            b.act = ACT_PECK;
            b.actTime = 0.0F;
        } else {
            b.dir = -b.dir;
            b.actTimer = s.random().range(0.3F, 0.9F);
        }
    }

    /**
     * Clamps like the mockup's {@code clamp}: the lower bound wins when the bounds cross on very narrow surfaces.
     */
    private static float clampTo(float value, float min, float max) {
        return (value < min) ? min : Math.min(value, max);
    }

    /**
     * Draws the birds perched on an element. They belong to the element, so they always stay in front of it.
     */
    @Override
    public void drawElement(@NotNull PixelGraphics graphics) {
        for (int i = 0; i < this.birds.size(); i++) {
            Bird b = this.birds.get(i);
            if (isElementContent(b)) this.drawBird(graphics, b);
        }
    }

    /**
     * Draws the flying birds and the ones perched on the floor, which is screen-wide content like the flying ones.
     */
    @Override
    public void drawFront(@NotNull PixelGraphics graphics) {
        for (int i = 0; i < this.birds.size(); i++) {
            Bird b = this.birds.get(i);
            if ((b.state != AWAY) && !isElementContent(b)) this.drawBird(graphics, b);
        }
    }

    /**
     * Whether a bird belongs to an element: while it is perched on one. Birds perched on the floor don't.
     */
    static boolean isElementContent(@NotNull Bird b) {
        return (b.state == PERCHED) && (b.surface != null) && !b.surface.isFloor();
    }

    private void drawBird(@NotNull PixelGraphics graphics, @NotNull Bird b) {
        int n = this.scene.scale();
        int halfWidth = (SPRITE_WIDTH * n) >> 1;
        if (b.state == PERCHED) {
            float x = b.x;
            int y = Math.round(b.y);
            if (b.act == ACT_HOP) {
                float u = b.actTime / HOP_TIME;
                x = PixelMath.lerp(b.hopFromX, b.hopToX, u);
                y -= Math.round((float)Math.sin(u * Math.PI) * 2.0F * n);
            }
            boolean pecking = (b.act == ACT_PECK) && (b.actTime > 0.08F) && (b.actTime < 0.28F);
            this.sprite(graphics, pecking ? PECK : STAND, SPECIES[b.species], Math.round(x) - halfWidth, y - (STAND_HEIGHT * n - 1), b.dir < 0, n);
        } else {
            PixelSprite frame = ((PixelMath.floor(this.scene.time() * 12.0F + b.species) & 1) != 0) ? WINGS_UP : WINGS_DOWN;
            this.sprite(graphics, frame, SPECIES[b.species], Math.round(b.x) - halfWidth, Math.round(b.y) - (FLIGHT_HEIGHT * n - 1), b.dir < 0, n);
        }
    }

    int birdCount() {
        return this.birds.size();
    }

    @NotNull
    Bird bird(int index) {
        return this.birds.get(index);
    }

    /**
     * One bird. Its position is the middle of its feet: the row it stands on while perched.
     */
    static final class Bird {

        int species;
        int state;
        /** Seconds until an away bird lands again */
        float timer;
        float x;
        float y;
        /** 1 while it faces right, -1 while it faces left */
        int dir;
        @Nullable
        PixelSurfaces.Surface<?> surface;
        float startX;
        float startY;
        int targetX;
        int targetY;
        float progress;
        float duration;
        int act;
        /** Seconds until a perched bird does something */
        float actTimer;
        /** Seconds since the current hop or peck started */
        float actTime;
        float hopFromX;
        float hopToX;
        /** Seconds a perched bird stays */
        float stay;
        float vx;
        float vy;

    }

}
