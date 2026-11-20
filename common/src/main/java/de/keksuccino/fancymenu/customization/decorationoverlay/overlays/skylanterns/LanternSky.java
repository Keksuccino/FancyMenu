package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.skylanterns;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreaturePool;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureScene;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureSimulation;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Paper lanterns in three sizes (depths) that float up and drift on a light wind. Small ones are far away and pass behind the
 * elements, the flames flicker, near lanterns carry a dithered warm halo, and every so often a whole group gets released together
 * from one spot. The port of the mockup's {@code lanternNew}.
 */
final class LanternSky extends CreatureSimulation {

    static final PixelSprite[] LANTERNS = {
            PixelSprite.of(
                    "ttt",
                    "tmt",
                    "mym",
                    ".w."),
            PixelSprite.of(
                    ".ttt.",
                    "ttttt",
                    "tmmmd",
                    "tmymd",
                    ".ywy.",
                    ".k.k."),
            PixelSprite.of(
                    ".ttttt.",
                    "ttttttt",
                    "tmmmmmd",
                    "tmmmmmd",
                    "tmyyymd",
                    ".myyym.",
                    ".mywym.",
                    "..ywy..",
                    "..k.k..")
    };
    /** Far lanterns in steady light and while flickering */
    static final PixelPalette FAR_STEADY = PixelPalette.builder().rgb('t', 0x9A5630).rgb('m', 0xB87A3C).rgb('y', 0xD0A85A).rgb('w', 0xE8D8A0).build();
    static final PixelPalette FAR_FLICKER = PixelPalette.builder().rgb('t', 0x9A5630).rgb('m', 0xAE7038).rgb('y', 0xC49A50).rgb('w', 0xD8C080).build();
    /** Middle and near lanterns in steady light and while flickering */
    static final PixelPalette NEAR_STEADY = PixelPalette.builder()
            .rgb('t', 0xE8782E).rgb('m', 0xFFB347).rgb('y', 0xFFE08A).rgb('w', 0xFFFFFF).rgb('d', 0xC25A22).rgb('k', 0x6A2A16)
            .build();
    static final PixelPalette NEAR_FLICKER = PixelPalette.builder()
            .rgb('t', 0xE8782E).rgb('m', 0xF8A63E).rgb('y', 0xFFD070).rgb('w', 0xFFF0B0).rgb('d', 0xC25A22).rgb('k', 0x6A2A16)
            .build();
    static final PixelSprite HALO = halo(5);
    static final PixelPalette HALO_PALETTE = PixelPalette.builder().rgb('a', 0xFFB347, 46).rgb('b', 0xFFB347, 26).build();
    /** Lanterns per depth at the normal amount on the mockup stage */
    static final float[] LAYER_COUNTS = {22.0F, 10.0F, 5.0F};
    /** Rising speed range and sway amplitude per depth */
    private static final float[] RISE_MIN = {4.0F, 6.0F, 8.0F};
    private static final float[] RISE_MAX = {7.0F, 10.0F, 13.0F};
    private static final float[] SWAY = {1.5F, 3.0F, 5.0F};
    static final int FAR = 0;
    static final int NEAR = 2;

    /** The lanterns that are always around, one pool per depth */
    private final List<CreaturePool<Lantern>> layers = List.of(new CreaturePool<>(Lantern::new), new CreaturePool<>(Lantern::new), new CreaturePool<>(Lantern::new));
    /** Lanterns of released groups, they are gone once they leave the screen */
    private final CreaturePool<Lantern> released = new CreaturePool<>(Lantern::new);
    private float nextRelease = 6.0F;

    LanternSky(@NotNull CreatureScene scene) {
        super(scene);
    }

    /**
     * The mockup's {@code lightHalo}: a diamond of dots, a dense checkerboard in the middle and sparse dots further out.
     */
    @NotNull
    static PixelSprite halo(int radius) {
        int size = radius * 2 + 1;
        char[] codes = new char[size * size];
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                int m = Math.abs(x) + Math.abs(y);
                char c = '.';
                if ((m <= Math.min(4, radius)) && (((x + y) & 1) == 0)) c = 'a';
                else if ((m <= radius) && ((x & 1) == 0) && ((y & 1) == 0)) c = 'b';
                codes[(y + radius) * size + (x + radius)] = c;
            }
        }
        return PixelSprite.fromCodes(size, size, codes);
    }

    int wantedInLayer(int depth) {
        return Math.max(1, Math.round(LAYER_COUNTS[depth] * this.scene.amount() * this.scene.density()));
    }

    @Override
    public void update(float dt) {
        CreatureScene s = this.scene;
        PixelRandom random = s.random();
        int n = s.scale();
        float amount = s.amount();
        // The amount can change while the overlay runs: new lanterns appear somewhere, surplus ones are gone
        for (int depth = 0; depth < this.layers.size(); depth++) {
            CreaturePool<Lantern> layer = this.layers.get(depth);
            int wanted = this.wantedInLayer(depth);
            while (layer.size() < wanted) this.spawn(layer.add(), depth, true);
            layer.truncate(wanted);
        }
        if ((this.nextRelease -= dt * s.eventRate()) <= 0.0F) {
            this.nextRelease = random.range(11.0F, 16.0F);
            float centerX = s.range(80.0F, s.width() - 80.0F);
            for (int i = Math.max(1, Math.round(random.rangeInt(6, 10) * amount)); i > 0; i--) {
                Lantern l = this.released.add();
                this.spawn(l, random.chance(0.4F) ? NEAR : 1, false);
                l.x = centerX + random.range(-60.0F, 60.0F);
                l.y = s.height() + random.range(2.0F, 50.0F);
            }
        }
        float lift = 0.75F + 0.25F * n;
        for (int depth = 0; depth < this.layers.size(); depth++) {
            CreaturePool<Lantern> layer = this.layers.get(depth);
            for (int i = 0; i < layer.size(); i++) {
                Lantern l = layer.get(i);
                if (rise(l, dt, lift, n)) this.spawn(l, l.depth, false);
            }
        }
        for (int i = this.released.size() - 1; i >= 0; i--) {
            if (rise(this.released.get(i), dt, lift, n)) this.released.remove(i);
        }
    }

    /**
     * Moves a lantern up and with the wind. Returns true once it is gone above the screen.
     */
    private static boolean rise(@NotNull Lantern l, float dt, float lift, int scale) {
        l.phase += l.frequency * dt;
        l.y -= l.speed * lift * dt;
        l.x += (2.0F + l.depth * 1.5F) * dt;
        return l.y < -12.0F * scale;
    }

    /**
     * Sets a lantern up like the mockup's {@code mk}.
     *
     * @param anywhere true to start it anywhere on the screen, false to start it below the bottom edge
     */
    private void spawn(@NotNull Lantern l, int depth, boolean anywhere) {
        CreatureScene s = this.scene;
        PixelRandom random = s.random();
        l.depth = depth;
        l.x = random.range(-20.0F, s.width());
        l.y = anywhere ? random.range(-10.0F, s.height()) : s.height() + random.range(4.0F, 40.0F);
        l.speed = random.range(RISE_MIN[depth], RISE_MAX[depth]);
        l.phase = random.range(0.0F, PixelMath.TAU);
        l.frequency = random.range(0.5F, 1.1F);
        l.sway = SWAY[depth];
        l.flickerOffset = random.range(0.0F, 9.0F);
    }

    @Override
    public void drawBack(@NotNull PixelGraphics graphics) {
        this.drawDepth(graphics, FAR);
    }

    @Override
    public void drawFront(@NotNull PixelGraphics graphics) {
        for (int depth = FAR + 1; depth <= NEAR; depth++) this.drawDepth(graphics, depth);
    }

    private void drawDepth(@NotNull PixelGraphics graphics, int depth) {
        CreaturePool<Lantern> layer = this.layers.get(depth);
        for (int i = 0; i < layer.size(); i++) this.drawLantern(graphics, layer.get(i));
        for (int i = 0; i < this.released.size(); i++) {
            Lantern l = this.released.get(i);
            if (l.depth == depth) this.drawLantern(graphics, l);
        }
    }

    private void drawLantern(@NotNull PixelGraphics graphics, @NotNull Lantern l) {
        int n = this.scene.scale();
        int x = PixelMath.floor(l.x + (float)Math.sin(l.phase) * l.sway * n);
        int y = PixelMath.floor(l.y);
        boolean flicker = (PixelMath.floor(this.scene.time() * 7.0F + l.flickerOffset) % 5) == 0;
        if (l.depth == NEAR) this.sprite(graphics, HALO, HALO_PALETTE, x - 2 * n, y, false, n);
        PixelPalette palette = (l.depth == FAR) ? (flicker ? FAR_FLICKER : FAR_STEADY) : (flicker ? NEAR_FLICKER : NEAR_STEADY);
        this.sprite(graphics, LANTERNS[l.depth], palette, x, y, false, n);
    }

    int layerCount(int depth) {
        return this.layers.get(depth).size();
    }

    int releasedCount() {
        return this.released.size();
    }

    @NotNull
    Lantern releasedLantern(int index) {
        return this.released.get(index);
    }

    static final class Lantern {

        /** 0 far, 1 middle, 2 near */
        int depth;
        /** Position of the top left corner of the sprite, before the sway */
        float x;
        float y;
        float speed;
        float phase;
        float frequency;
        float sway;
        float flickerOffset;

    }

}
