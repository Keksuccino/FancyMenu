package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.bats;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreaturePool;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureScene;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureSimulation;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPointer;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bats that cross the screen in small flapping, swooping groups and dodge the pointer, distant groups behind the elements, and bats
 * that hang asleep under the elements, open their eyes when the pointer comes near and drop off when it gets too close.
 * After a while another bat flies back to the empty spot. The port of the mockup's {@code batNew}.
 */
final class BatColony extends CreatureSimulation {

    static final PixelSprite WINGS_UP = PixelSprite.of(
            "m.......m",
            "mb.k.k.bm",
            "bbbrbrbbb",
            ".b.bbb.b.",
            "....b....");
    static final PixelSprite WINGS_MID = PixelSprite.of(
            ".........",
            "...k.k...",
            "mbbrbrbbm",
            "bb.bbb.bb",
            "b...b...b");
    static final PixelSprite WINGS_DOWN = PixelSprite.of(
            ".........",
            "...k.k...",
            "..brbrb..",
            ".mbbbbbm.",
            "mb..b..bm");
    static final PixelSprite FAR_UP = PixelSprite.of(
            "b...b",
            ".bbb.",
            "..b..");
    static final PixelSprite FAR_DOWN = PixelSprite.of(
            ".....",
            "bbbbb",
            "b.b.b");
    static final PixelSprite HANGING = PixelSprite.of(
            ".l.l.",
            ".mbb.",
            "mbbbb",
            "mbbbb",
            ".mbb.",
            ".bbb.",
            ".k.k.");
    static final PixelSprite HANGING_AWAKE = PixelSprite.of(
            ".l.l.",
            ".mbb.",
            "mbbbb",
            "mbbbb",
            ".mbb.",
            ".rbr.",
            ".k.k.");
    static final PixelPalette PALETTE = PixelPalette.builder()
            .rgb('b', 0x0B0616).rgb('k', 0x0B0616).rgb('m', 0x4A3668).rgb('r', 0xE04848).rgb('l', 0x4A3668)
            .build();
    private static final PixelSprite[] FLAP = {WINGS_UP, WINGS_MID, WINGS_DOWN, WINGS_MID};
    /** Most bats of a group */
    static final int MAX_GROUP_SIZE = 6;
    /** How far a returning bat flies per second */
    private static final float RETURN_SPEED = 85.0F;

    static final int EMPTY = 0;
    static final int HANGING_STATE = 1;
    static final int INBOUND = 2;

    private final CreaturePool<Group> groups = new CreaturePool<>(Group::new);
    private final CreaturePool<FreeBat> free = new CreaturePool<>(FreeBat::new);
    private final BatRoosts layout = new BatRoosts();
    private final Roost[] roosts = new Roost[BatRoosts.MAX_ROOSTS];
    private float nextGroup = 0.5F;
    private float nextFarGroup = 1.0F;
    private int layoutScale = -1;
    private boolean started = false;

    BatColony(@NotNull CreatureScene scene) {
        super(scene);
        for (int k = 0; k < this.roosts.length; k++) this.roosts[k] = new Roost();
    }

    /**
     * Bigger bats fly further apart, so a group does not turn into one blob.
     */
    static float gap(int scale) {
        return 1.0F + (scale - 1) * 0.7F;
    }

    /**
     * How many of the roosts are in use at an amount: two at the normal amount.
     */
    static int hangingCount(float amount) {
        return PixelMath.clamp(Math.round(2.0F * amount), 1, BatRoosts.MAX_ROOSTS);
    }

    @Override
    public void update(float dt) {
        CreatureScene s = this.scene;
        PixelRandom random = s.random();
        PixelPointer pointer = s.pointer();
        int n = s.scale();
        float gap = gap(n);
        float dodge = 47.0F + 8.0F * n;
        float wake = 62.0F + 8.0F * n;
        float drop = 24.0F + 8.0F * n;
        float time = s.time();
        float w = s.width();

        if (s.surfacesChanged() || (n != this.layoutScale)) this.relayout();

        // The amount sets how often groups cross
        float rate = dt * s.amount() * s.eventRate();
        if ((this.nextGroup -= rate) <= 0.0F) {
            this.spawnGroup(false);
            this.nextGroup = random.range(3.5F, 7.0F);
        }
        if ((this.nextFarGroup -= rate) <= 0.0F) {
            this.spawnGroup(true);
            this.nextFarGroup = random.range(4.0F, 8.0F);
        }

        for (int i = this.groups.size() - 1; i >= 0; i--) {
            Group g = this.groups.get(i);
            g.x += g.dir * g.speed * dt;
            if (!g.far) {
                for (int j = 0; j < g.size; j++) {
                    Bat a = g.bats[j];
                    if (pointer.isInside()) {
                        float bx = g.x - g.dir * a.offsetX * gap;
                        float by = g.y + a.offsetY * gap + (float)Math.sin(time * 2.0F + a.phase) * 14.0F + a.dodgeY;
                        float dx = bx - pointer.x();
                        float dy = by - pointer.y();
                        if (dx * dx + dy * dy < dodge * dodge) a.dodgeSpeed += ((dy < 0.0F) ? -1.0F : 1.0F) * 260.0F * dt;
                    }
                    // A damped spring pulls a dodging bat back into the group
                    a.dodgeSpeed += (-a.dodgeY * 6.0F - a.dodgeSpeed * 3.0F) * dt;
                    a.dodgeY += a.dodgeSpeed * dt;
                }
            }
            if ((g.x < -130.0F * gap) || (g.x > w + 130.0F * gap)) this.groups.remove(i);
        }

        int active = Math.min(hangingCount(s.amount()), this.layout.count());
        for (int k = 0; k < this.layout.count(); k++) {
            Roost r = this.roosts[k];
            PixelSurfaces.Surface<?> surface = r.surface;
            if (surface == null) continue;
            boolean used = k < active;
            int hx = r.x;
            int hy = surface.bottom();
            if (r.state == HANGING_STATE) {
                r.timer += dt;
                float distance = pointer.isInside() ? (float)Math.hypot(hx + 2.0F - pointer.x(), hy + 3.0F * n - pointer.y()) : Float.MAX_VALUE;
                r.awake = distance < wake;
                boolean tooClose = distance < drop;
                if (!used || tooClose || (r.timer > r.duration)) {
                    r.state = EMPTY;
                    r.timer = used ? random.range(6.0F, 12.0F) : random.range(0.5F, 2.0F);
                    // A bat the pointer came too close to flies away from it
                    float vx = ((tooClose && (pointer.x() > hx)) ? -1.0F : 1.0F) * random.range(60.0F, 90.0F);
                    this.release(hx + 2.0F, hy + 6.0F * n, vx);
                }
            } else if (used && (r.state == EMPTY) && ((r.timer -= dt) <= 0.0F)) {
                r.state = INBOUND;
                FreeBat f = this.free.add();
                f.x = random.chance(0.5F) ? -12.0F : w + 12.0F;
                f.y = s.range(s.stageY(60.0F), s.stageY(200.0F));
                f.vx = 0.0F;
                f.vy = 0.0F;
                f.roost = r;
                f.frameOffset = random.rangeInt(0, 3);
            }
        }

        for (int i = this.free.size() - 1; i >= 0; i--) {
            FreeBat f = this.free.get(i);
            Roost r = f.roost;
            PixelSurfaces.Surface<?> target = (r != null) ? r.surface : null;
            if ((r != null) && (target != null)) {
                float tx = r.x + 2.0F;
                float ty = target.bottom() + 5.0F * n;
                float ex = tx - f.x;
                float ey = ty - f.y;
                float distance = (float)Math.sqrt(ex * ex + ey * ey);
                float step = RETURN_SPEED * dt;
                if (distance <= step) {
                    r.state = HANGING_STATE;
                    r.timer = 0.0F;
                    r.duration = random.range(14.0F, 26.0F);
                    r.awake = false;
                    this.free.remove(i);
                } else {
                    f.x += ex / distance * step;
                    f.y += ey / distance * step + (float)Math.sin(time * 8.0F + f.frameOffset) * 10.0F * dt;
                }
            } else {
                f.vy -= 90.0F * dt;
                f.x += f.vx * dt;
                f.y += f.vy * dt;
                if ((f.y < -12.0F) || (f.x < -14.0F) || (f.x > w + 14.0F)) this.free.remove(i);
            }
        }
    }

    /**
     * Places the roosts for the current elements and bat size. Roosts that keep their spot keep their bat, bats of roosts that
     * moved or are gone drop off and fly away. In the very first layout, the roosts in use start with a sleeping bat.
     */
    private void relayout() {
        CreatureScene s = this.scene;
        PixelRandom random = s.random();
        int n = s.scale();
        int oldScale = Math.max(1, this.layoutScale);
        this.layout.layout(s.surfaces(), s.width(), s.height(), n);
        this.layoutScale = n;
        // The first layout with roosts starts with sleeping bats, also when the elements show up a few frames late
        boolean first = !this.started && (this.layout.count() > 0);
        int active = hangingCount(s.amount());
        for (int k = 0; k < BatRoosts.MAX_ROOSTS; k++) {
            Roost r = this.roosts[k];
            boolean exists = k < this.layout.count();
            PixelSurfaces.Surface<?> surface = exists ? this.layout.surface(k) : null;
            boolean same = exists && (r.surface == surface) && (r.x == this.layout.x(k));
            if (first) {
                r.state = (exists && (k < active)) ? HANGING_STATE : EMPTY;
                r.timer = (r.state == HANGING_STATE) ? random.range(0.0F, 8.0F) : random.range(0.5F, 2.0F);
                r.duration = random.range(14.0F, 26.0F);
                r.phase = random.range(0.0F, PixelMath.TAU);
                r.awake = false;
            } else if (!same) {
                if ((r.state == HANGING_STATE) && (r.surface != null)) {
                    this.release(r.x + 2.0F, r.surface.bottom() + 6.0F * oldScale, random.sign() * random.range(60.0F, 90.0F));
                }
                if (r.state != INBOUND) {
                    r.state = EMPTY;
                    r.timer = random.range(6.0F, 12.0F);
                }
                r.awake = false;
            }
            r.surface = surface;
            r.x = exists ? this.layout.x(k) : 0;
            if (!exists && (r.state == INBOUND)) {
                // Its spot is gone, so the returning bat turns around
                this.abandonInbound(r);
                r.state = EMPTY;
            }
        }
        if (first) this.started = true;
    }

    private void abandonInbound(@NotNull Roost roost) {
        for (int i = 0; i < this.free.size(); i++) {
            FreeBat f = this.free.get(i);
            if (f.roost != roost) continue;
            f.roost = null;
            f.vx = this.scene.random().sign() * this.scene.random().range(60.0F, 90.0F);
            f.vy = 0.0F;
        }
    }

    /**
     * Lets a bat drop off a roost: it falls a bit, then flies up and away.
     */
    private void release(float x, float y, float vx) {
        FreeBat f = this.free.add();
        f.x = x;
        f.y = y;
        f.vx = vx;
        f.vy = 30.0F;
        f.roost = null;
        f.frameOffset = this.scene.random().rangeInt(0, 3);
    }

    private void spawnGroup(boolean far) {
        CreatureScene s = this.scene;
        PixelRandom random = s.random();
        Group g = this.groups.add();
        g.far = far;
        g.dir = random.sign();
        g.x = (g.dir > 0) ? -20.0F : s.width() + 20.0F;
        g.y = far ? s.range(s.stageY(30.0F), s.stageY(120.0F)) : s.range(s.stageY(40.0F), s.stageY(150.0F));
        g.speed = far ? random.range(40.0F, 60.0F) : random.range(75.0F, 115.0F);
        g.size = far ? random.rangeInt(3, 5) : random.rangeInt(3, MAX_GROUP_SIZE);
        for (int i = 0; i < g.size; i++) {
            Bat a = g.bats[i];
            a.offsetX = i * random.range(9.0F, 15.0F);
            a.offsetY = random.range(-14.0F, 14.0F);
            a.phase = random.range(0.0F, PixelMath.TAU);
            a.frameOffset = random.rangeInt(0, 3);
            a.dodgeY = 0.0F;
            a.dodgeSpeed = 0.0F;
        }
    }

    @Override
    public void drawBack(@NotNull PixelGraphics graphics) {
        int n = this.scene.scale();
        float gap = gap(n);
        float time = this.scene.time();
        for (int i = 0; i < this.groups.size(); i++) {
            Group g = this.groups.get(i);
            if (!g.far) continue;
            for (int j = 0; j < g.size; j++) {
                Bat a = g.bats[j];
                PixelSprite frame = ((PixelMath.floor(time * 7.0F + a.frameOffset) & 1) != 0) ? FAR_UP : FAR_DOWN;
                float x = g.x - g.dir * a.offsetX * 0.7F * gap;
                float y = g.y + a.offsetY * 0.6F * gap + (float)Math.sin(time * 2.0F + a.phase) * 6.0F;
                this.sprite(graphics, frame, PALETTE, PixelMath.floor(x), PixelMath.floor(y), false, n);
            }
        }
    }

    @Override
    public void drawElement(@NotNull PixelGraphics graphics) {
        int n = this.scene.scale();
        float time = this.scene.time();
        int drawOffset = (BatRoosts.BAT_WIDTH * (n - 1)) >> 1;
        for (int k = 0; k < this.layout.count(); k++) {
            Roost r = this.roosts[k];
            PixelSurfaces.Surface<?> surface = r.surface;
            if ((r.state != HANGING_STATE) || (surface == null)) continue;
            // Now and then a sleeping bat sways a pixel to the side
            int sway = (Math.sin(time * 1.3F + r.phase) > 0.6) ? n : 0;
            this.sprite(graphics, r.awake ? HANGING_AWAKE : HANGING, PALETTE, r.x - drawOffset + sway, surface.bottom(), false, n);
        }
    }

    @Override
    public void drawFront(@NotNull PixelGraphics graphics) {
        int n = this.scene.scale();
        float gap = gap(n);
        float time = this.scene.time();
        int groupFrame = PixelMath.floor(time * 10.0F);
        for (int i = 0; i < this.groups.size(); i++) {
            Group g = this.groups.get(i);
            if (g.far) continue;
            for (int j = 0; j < g.size; j++) {
                Bat a = g.bats[j];
                float x = g.x - g.dir * a.offsetX * gap - 4.0F * n;
                float y = g.y + a.offsetY * gap + (float)Math.sin(time * 2.0F + a.phase) * 14.0F + a.dodgeY - 2.0F * n;
                this.sprite(graphics, FLAP[(groupFrame + a.frameOffset) & 3], PALETTE, PixelMath.floor(x), PixelMath.floor(y), false, n);
            }
        }
        int freeFrame = PixelMath.floor(time * 12.0F);
        for (int i = 0; i < this.free.size(); i++) {
            FreeBat f = this.free.get(i);
            this.sprite(graphics, FLAP[(freeFrame + f.frameOffset) & 3], PALETTE, PixelMath.floor(f.x - 4.0F * n), PixelMath.floor(f.y - 2.0F * n), false, n);
        }
    }

    int roostCount() {
        return this.layout.count();
    }

    @NotNull
    Roost roost(int index) {
        return this.roosts[index];
    }

    int roostX(int index) {
        return this.roosts[index].x;
    }

    int hangingBats() {
        int hanging = 0;
        for (int k = 0; k < this.layout.count(); k++) {
            if (this.roosts[k].state == HANGING_STATE) hanging++;
        }
        return hanging;
    }

    int groupCount() {
        return this.groups.size();
    }

    @NotNull
    Group group(int index) {
        return this.groups.get(index);
    }

    int freeBatCount() {
        return this.free.size();
    }

    /**
     * A group of bats crossing the screen. Near groups fly in front of the elements, far ones behind them.
     */
    static final class Group {

        boolean far;
        /** 1 for groups flying to the right, -1 for groups flying to the left */
        int dir;
        float x;
        float y;
        float speed;
        int size;
        final Bat[] bats = new Bat[MAX_GROUP_SIZE];

        Group() {
            for (int i = 0; i < this.bats.length; i++) this.bats[i] = new Bat();
        }

    }

    static final class Bat {

        /** Distance behind the head of the group */
        float offsetX;
        float offsetY;
        float phase;
        int frameOffset;
        /** How far it dodged the pointer, and how fast it moves back */
        float dodgeY;
        float dodgeSpeed;

    }

    /**
     * A spot under an element where a bat can hang.
     */
    static final class Roost {

        @Nullable
        PixelSurfaces.Surface<?> surface;
        /** The left end of a bat of size 1 hanging here */
        int x;
        int state;
        /** Seconds it hangs there while hanging, seconds until a bat comes back while empty */
        float timer;
        float duration;
        float phase;
        boolean awake;

    }

    /**
     * A bat on its own: one that dropped off a roost and flies away, or one that flies back to a roost.
     */
    static final class FreeBat {

        float x;
        float y;
        float vx;
        float vy;
        /** The roost it flies back to, NULL while it flies away */
        @Nullable
        Roost roost;
        int frameOffset;

    }

}
