package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.bubbles;

import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreaturePool;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureScene;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures.CreatureSimulation;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * Bubbles in three sizes that wobble upward, bigger ones faster, many of them in streams. A bubble that hits the underside of an
 * element either pops or slides along it and escapes at its edge. Clicks pop the bubbles near the pointer and set a few small ones
 * free. Tiny far bubbles rise behind the elements. The port of the mockup's {@code bubbleNew}.
 */
final class BubbleStreams extends CreatureSimulation {

    static final PixelSprite[] BUBBLES = {
            PixelSprite.of(
                    "ho.",
                    "o.o",
                    ".o."),
            PixelSprite.of(
                    ".ooo.",
                    "oh..o",
                    "o...o",
                    "o...o",
                    ".ooo."),
            PixelSprite.of(
                    "..ooo..",
                    ".oh..o.",
                    "oh....o",
                    "o.....o",
                    "o.....o",
                    ".o...o.",
                    "..ooo..")
    };
    static final int[] SIZES = {3, 5, 7};
    static final PixelPalette PALETTE = PixelPalette.builder().rgb('o', 0xCFEFFA, 215).rgb('h', 0xFFFFFF).build();
    private static final int SPECK_COLOR = PixelMath.withAlpha(0x8FCFE6, 150);
    /** Pop rings by radius (2 to 6): eight sparks around the middle of the bubble */
    private static final PopRing[] POP_RINGS = {null, null, new PopRing(2), new PopRing(3), new PopRing(4), new PopRing(5), new PopRing(6)};
    private static final PixelPalette POP_EARLY = PixelPalette.builder().rgb('p', 0xE6F8FF, 230).build();
    private static final PixelPalette POP_LATE = PixelPalette.builder().rgb('p', 0xE6F8FF, 140).build();
    /** Base wobble amplitude and rising speed per size */
    private static final float[] WOBBLE = {2.0F, 3.0F, 4.0F};
    private static final float[] RISE_SPEED = {18.0F, 28.0F, 40.0F};
    /** Stream positions on the mockup stage, as fractions of its width. Wider screens repeat them. */
    private static final float[] STREAM_PATTERN = {60.0F / 480.0F, 150.0F / 480.0F, 330.0F / 480.0F, 420.0F / 480.0F};
    static final int MAX_STREAMS = 16;
    /** Bubbles that are always around, far specks and the most bubbles the streams fill up to, all on the mockup stage */
    static final float BUBBLE_COUNT = 34.0F;
    static final float SPECK_COUNT = 40.0F;
    static final float STREAM_LIMIT = 90.0F;
    /** Clicks can add bubbles beyond the stream limit, up to this many */
    static final float CLICK_LIMIT = 130.0F;
    static final float POP_TIME = 0.2F;
    static final float SLIDE_SPEED = 12.0F;
    static final float CLICK_RADIUS = 28.0F;
    static final int CLICK_BUBBLES = 10;
    /** Chance that a bubble pops when it hits an element */
    static final float POP_CHANCE = 0.3F;

    private final CreaturePool<Bubble> bubbles = new CreaturePool<>(Bubble::new);
    private final CreaturePool<Speck> specks = new CreaturePool<>(Speck::new);
    private final Stream[] streams = new Stream[MAX_STREAMS];
    private int streamCount = 0;
    private int lastStreamWidth = -1;
    /** Bubbles that respawn at the bottom when they are gone, the others (from streams and clicks) just go */
    private int permanent = 0;

    BubbleStreams(@NotNull CreatureScene scene) {
        super(scene);
        for (int i = 0; i < this.streams.length; i++) this.streams[i] = new Stream();
    }

    int wantedPermanent() {
        return Math.max(1, Math.round(BUBBLE_COUNT * this.scene.density()));
    }

    int wantedSpecks() {
        return Math.round(SPECK_COUNT * this.scene.density());
    }

    int streamLimit() {
        return Math.round(STREAM_LIMIT * this.scene.density());
    }

    int clickLimit() {
        return Math.round(CLICK_LIMIT * this.scene.density());
    }

    @Override
    public void update(float dt) {
        CreatureScene s = this.scene;
        PixelRandom random = s.random();
        float w = s.width();
        float h = s.height();
        if (s.width() != this.lastStreamWidth) this.placeStreams();
        this.fitPermanent();
        while (this.specks.size() < this.wantedSpecks()) this.spawnSpeck(this.specks.add());
        this.specks.truncate(this.wantedSpecks());

        for (int i = 0; i < this.specks.size(); i++) {
            Speck f = this.specks.get(i);
            f.phase += dt * 2.0F;
            f.y -= f.speed * dt;
            f.x += (float)Math.sin(f.phase) * 4.0F * dt;
            if (f.y < -2.0F) {
                f.y = h + 2.0F;
                f.x = random.range(0.0F, w);
            }
        }

        for (int i = 0; i < this.streamCount; i++) {
            Stream e = this.streams[i];
            if ((e.timer -= dt) > 0.0F) continue;
            e.timer = random.range(0.3F, 0.9F);
            if (this.bubbles.size() >= this.streamLimit()) continue;
            Bubble u = this.bubbles.add();
            this.spawn(u, false, random.chance(0.8F) ? 0 : 1, e.x + random.range(-3.0F, 3.0F));
            u.temporary = true;
        }

        boolean surfacesChanged = s.surfacesChanged();
        List<? extends PixelSurfaces.Surface<?>> elements = s.elements();
        for (int i = this.bubbles.size() - 1; i >= 0; i--) {
            Bubble u = this.bubbles.get(i);
            int size = SIZES[u.kind];
            if (u.pop >= 0.0F) {
                if ((u.pop += dt) > POP_TIME) this.finish(i);
                continue;
            }
            PixelSurfaces.Surface<?> slide = u.slide;
            if ((slide != null) && surfacesChanged && !s.isSurfaceLive(slide)) {
                this.release(u);
                slide = null;
            }
            if (slide != null) {
                u.x += u.slideDir * SLIDE_SPEED * dt;
                // Past the edge of the element it rises again
                if ((u.x + size <= slide.x()) || (u.x >= slide.right())) this.release(u);
                continue;
            }
            float previousY = u.y;
            u.phase += u.frequency * dt;
            u.y -= u.speed * dt;
            float wobbledX = u.x + (float)Math.sin(u.phase) * u.amplitude;
            float centerX = wobbledX + size / 2.0F;
            for (int j = 0; j < elements.size(); j++) {
                PixelSurfaces.Surface<?> element = elements.get(j);
                int underside = element.bottom();
                if ((centerX >= element.x()) && (centerX < element.right()) && (previousY >= underside) && (u.y < underside)) {
                    u.onElement = true;
                    if (random.chance(POP_CHANCE)) {
                        u.pop = 0.0F;
                    } else {
                        u.y = underside;
                        u.x = wobbledX;
                        u.amplitude = 0.0F;
                        u.slide = element;
                        // It slides towards the nearer end of the element
                        u.slideDir = (centerX < element.x() + element.width() / 2.0F) ? -1 : 1;
                    }
                    break;
                }
            }
            if (u.y < -size - 2.0F) this.finish(i);
        }
    }

    /**
     * Pops the bubbles near a click and sets a few small ones free there.
     */
    @Override
    public void onClick(float x, float y, boolean primaryButton) {
        if (!primaryButton) return;
        PixelRandom random = this.scene.random();
        for (int i = 0; i < this.bubbles.size(); i++) {
            Bubble u = this.bubbles.get(i);
            if ((u.pop < 0.0F) && (Math.hypot(u.x - x, u.y - y) < CLICK_RADIUS)) u.pop = 0.0F;
        }
        for (int i = 0; (i < CLICK_BUBBLES) && (this.bubbles.size() < this.clickLimit()); i++) {
            Bubble u = this.bubbles.add();
            this.spawn(u, false, 0, x + random.range(-6.0F, 6.0F));
            u.y = y + random.range(-4.0F, 4.0F);
            u.temporary = true;
        }
    }

    /**
     * Spreads the streams over the screen: the mockup's four streams per 480 pixels of width, at its positions.
     */
    private void placeStreams() {
        CreatureScene s = this.scene;
        int width = s.width();
        this.lastStreamWidth = width;
        int tiles = Math.max(1, Math.round(width / (float)CreatureScene.STAGE_WIDTH));
        float tileWidth = width / (float)tiles;
        this.streamCount = Math.min(MAX_STREAMS, tiles * STREAM_PATTERN.length);
        for (int i = 0; i < this.streamCount; i++) {
            Stream e = this.streams[i];
            e.x = (i / STREAM_PATTERN.length + STREAM_PATTERN[i % STREAM_PATTERN.length]) * tileWidth + s.random().range(-10.0F, 10.0F);
            e.timer = s.random().range(0.0F, 1.0F);
        }
    }

    /**
     * Keeps the number of bubbles that are always around in line with the screen size. The first ones start all over the screen.
     */
    private void fitPermanent() {
        int wanted = this.wantedPermanent();
        while (this.permanent < wanted) {
            Bubble u = this.bubbles.add();
            this.spawn(u, true, -1, Float.NaN);
            this.permanent++;
        }
        for (int i = this.bubbles.size() - 1; (i >= 0) && (this.permanent > wanted); i--) {
            if (this.bubbles.get(i).temporary) continue;
            this.bubbles.remove(i);
            this.permanent--;
        }
    }

    /**
     * Sets a bubble up like the mockup's {@code mk}.
     *
     * @param anywhere true to start it anywhere on the screen, false to start it below the bottom edge
     * @param kind the size (0 to 2), -1 for a random one, small ones most often
     * @param x the column, NaN for a random one
     */
    private void spawn(@NotNull Bubble u, boolean anywhere, int kind, float x) {
        CreatureScene s = this.scene;
        PixelRandom random = s.random();
        if (kind < 0) kind = random.chance(0.55F) ? 0 : (random.chance(0.75F) ? 1 : 2);
        u.kind = kind;
        u.amplitude = WOBBLE[kind] * random.range(0.6F, 1.2F);
        u.baseAmplitude = u.amplitude;
        u.x = Float.isNaN(x) ? s.range(4.0F, s.width() - 8.0F) : x;
        u.y = anywhere ? random.range(0.0F, s.height()) : s.height() + random.range(2.0F, 20.0F);
        u.speed = RISE_SPEED[kind] * random.range(0.8F, 1.2F);
        u.phase = random.range(0.0F, PixelMath.TAU);
        u.frequency = random.range(1.5F, 3.2F);
        u.slide = null;
        u.slideDir = 0;
        u.onElement = false;
        u.pop = -1.0F;
        u.temporary = false;
    }

    private void spawnSpeck(@NotNull Speck f) {
        PixelRandom random = this.scene.random();
        f.x = random.range(0.0F, this.scene.width());
        f.y = random.range(0.0F, this.scene.height());
        f.speed = random.range(6.0F, 14.0F);
        f.phase = random.range(0.0F, PixelMath.TAU);
    }

    /**
     * Ends the slide of a bubble, so it rises and wobbles again.
     */
    private static void release(@NotNull Bubble u) {
        u.slide = null;
        u.onElement = false;
        u.amplitude = u.baseAmplitude;
        u.phase = 0.0F;
    }

    /**
     * A bubble popped or rose off the screen: stream and click bubbles are gone, the others start again at the bottom.
     */
    private void finish(int index) {
        Bubble u = this.bubbles.get(index);
        if (u.temporary) this.bubbles.remove(index);
        else this.spawn(u, false, -1, Float.NaN);
    }

    @Override
    public void drawBack(@NotNull PixelGraphics graphics) {
        for (int i = 0; i < this.specks.size(); i++) {
            Speck f = this.specks.get(i);
            graphics.pixel(PixelMath.floor(f.x), PixelMath.floor(f.y), SPECK_COLOR);
        }
    }

    /**
     * Draws the bubbles caught under an element and their pops. They belong to the element, so they stay in front of it whatever
     * the Layers setting is, and nothing drawn in the background pass (like a full-screen background element) can cover them.
     */
    @Override
    public void drawElement(@NotNull PixelGraphics graphics) {
        this.drawBubbles(graphics, true);
    }

    @Override
    public void drawFront(@NotNull PixelGraphics graphics) {
        this.drawBubbles(graphics, false);
    }

    private void drawBubbles(@NotNull PixelGraphics graphics, boolean onElement) {
        for (int i = 0; i < this.bubbles.size(); i++) {
            Bubble u = this.bubbles.get(i);
            if (u.onElement != onElement) continue;
            int size = SIZES[u.kind];
            int x = PixelMath.floor(u.x + (float)Math.sin(u.phase) * u.amplitude);
            int y = PixelMath.floor(u.y);
            if (u.pop >= 0.0F) {
                boolean late = u.pop > POP_TIME / 2.0F;
                PopRing ring = POP_RINGS[(size >> 1) + 1 + (late ? 2 : 0)];
                this.sprite(graphics, ring.sprite, late ? POP_LATE : POP_EARLY, x + (size >> 1) + ring.left, y + (size >> 1) + ring.top, false, 1);
            } else {
                this.sprite(graphics, BUBBLES[u.kind], PALETTE, x, y, false, 1);
            }
        }
    }

    int bubbleCount() {
        return this.bubbles.size();
    }

    @NotNull
    Bubble bubble(int index) {
        return this.bubbles.get(index);
    }

    int speckCount() {
        return this.specks.size();
    }

    int streamCount() {
        return this.streamCount;
    }

    float streamX(int index) {
        return this.streams[index].x;
    }

    static final class Bubble {

        int kind;
        /** Position of the top left corner of the sprite, before the wobble */
        float x;
        float y;
        float speed;
        float phase;
        float frequency;
        float amplitude;
        float baseAmplitude;
        /** The element it slides along, NULL while it rises */
        @Nullable
        PixelSurfaces.Surface<?> slide;
        int slideDir;
        /** Whether it belongs to an element: while it slides along one, and while it pops after hitting one */
        boolean onElement;
        /** Seconds since it popped, negative while it is whole */
        float pop;
        /** Stream and click bubbles are gone once they pop or leave the screen */
        boolean temporary;

    }

    /**
     * A tiny far bubble behind the elements.
     */
    static final class Speck {

        float x;
        float y;
        float speed;
        float phase;

    }

    static final class Stream {

        float x;
        float timer;

    }

    /**
     * The sparks of a popping bubble as one sprite, the mockup's {@code popDraw}: eight points at a radius around the middle, on the
     * diagonals and (a bit further out) on the axes, rounded down like the mockup's pixel buffer does.
     */
    static final class PopRing {

        private static final double[][] DIRECTIONS = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {0, -1.4}, {0, 1.4}, {-1.4, 0}, {1.4, 0}};

        final PixelSprite sprite;
        /** Offset of the sprite's top left corner from the middle of the bubble */
        final int left;
        final int top;

        PopRing(int radius) {
            int[] xs = new int[DIRECTIONS.length];
            int[] ys = new int[DIRECTIONS.length];
            int minX = 0;
            int minY = 0;
            int maxX = 0;
            int maxY = 0;
            for (int i = 0; i < DIRECTIONS.length; i++) {
                xs[i] = (int)Math.floor(DIRECTIONS[i][0] * radius);
                ys[i] = (int)Math.floor(DIRECTIONS[i][1] * radius);
                minX = Math.min(minX, xs[i]);
                minY = Math.min(minY, ys[i]);
                maxX = Math.max(maxX, xs[i]);
                maxY = Math.max(maxY, ys[i]);
            }
            int width = maxX - minX + 1;
            int height = maxY - minY + 1;
            char[] codes = new char[width * height];
            Arrays.fill(codes, '.');
            for (int i = 0; i < DIRECTIONS.length; i++) codes[(ys[i] - minY) * width + (xs[i] - minX)] = 'p';
            this.sprite = PixelSprite.fromCodes(width, height, codes);
            this.left = minX;
            this.top = minY;
        }

    }

}
