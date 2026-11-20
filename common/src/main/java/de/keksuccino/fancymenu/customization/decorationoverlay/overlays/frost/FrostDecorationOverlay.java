package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.frost;

import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.TiledCanvas;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcon;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Frost creeps over the menu: a glazed band with icicles along the screen edges, cracked ice plates with crystals in the corners,
 * and ice crusts with a rime ridge and icicles on the elements. A click works like a hammer blow: the ice at that spot breaks out,
 * chips fly off, cracks run into the ice around the hole, and the hole freezes over again from its rim.<br><br>
 *
 * Parts: the ice of the edges and corners is drawn across the screen ({@code FRONT}), the ice on elements and everything a blow on it
 * throws off belongs to the elements ({@code ELEMENT}). Every part freezes on its own clock, so a part that gets switched back on
 * freezes over again.<br>
 * The ice lives in two canvases that only change while ice grows, breaks or refreezes. A frozen, untouched screen costs nothing per frame
 * besides the glints.
 */
public class FrostDecorationOverlay extends PixelDecorationOverlay<FrostDecorationOverlay> {

    private static final String KEY = "fancymenu.decoration_overlays.frost.";
    static final int EDGES = 0;
    static final int CORNERS = 1;
    static final int ELEMENTS = 2;
    private static final int PART_COUNT = 3;
    /** The ice is designed, not random: these seeds give it its shape */
    private static final int SCREEN_SEED = 4242;
    private static final int ELEMENT_SEED = 4243;
    private static final int BLOW_RADIUS = 15;
    private static final int MAX_CHIPS = 260;
    private static final int MAX_CRACKS = 36;
    private static final int CRACKS_PER_BLOW = 6;
    private static final int MAX_CRACK_LENGTH = 32;
    private static final float CRACK_LIFE = 0.9F;
    private static final float FLASH_LIFE = 0.14F;
    private static final float GLINT_LIFE = 0.4F;

    /** Chips are single pixels, slivers that tumble, and a few small chunks. */
    private static final PixelSprite CHIP_DOT = PixelSprite.of("1");
    private static final PixelSprite CHIP_FLAT = PixelSprite.of("11");
    private static final PixelSprite CHIP_TALL = PixelSprite.of("1", "1");
    private static final PixelSprite CHIP_CHUNK = PixelSprite.of("11", "23");
    private static final PixelPalette[] CHIP_PALETTES = new PixelPalette[FrostArt.TONE.length];
    /** The white flash of a blow: a small cross first, then a burst of rays further out */
    private static final PixelSprite FLASH_NEAR = PixelSprite.of(
            "..#..",
            "..#..",
            "#####",
            "..#..",
            "..#..");
    private static final PixelSprite FLASH_FAR = PixelSprite.of(
            "#....#....#",
            ".#...#...#.",
            "..#..#..#..",
            "...........",
            "...........",
            "###.....###",
            "...........",
            "...........",
            "..#..#..#..",
            ".#...#...#.",
            "#....#....#");
    private static final PixelPalette FLASH_PALETTE = PixelPalette.builder().rgb('#', 0xFFFFFF).build();
    private static final PixelSprite GLINT_STAR = PixelSprite.of(
            ".2.",
            "212",
            ".2.");
    private static final PixelSprite GLINT_DOT = PixelSprite.of("1");
    private static final PixelPalette GLINT_PALETTE = PixelPalette.builder().rgb('1', 0xFFFFFF).rgb('2', FrostArt.GLINT_ARM).build();

    static {
        for (int tone = 0; tone < FrostArt.TONE.length; tone++) {
            CHIP_PALETTES[tone] = PixelPalette.builder().rgb('1', FrostArt.TONE[tone]).rgb('2', FrostArt.TONE[2]).rgb('3', FrostArt.TONE[3]).build();
        }
    }

    public final Property<Boolean> screenEdges = putProperty(Property.booleanProperty("screen_edges", true, KEY + "screen_edges"));
    public final Property<Boolean> corners = putProperty(Property.booleanProperty("corners", true, KEY + "corners"));
    public final Property<Boolean> onElements = putProperty(Property.booleanProperty("on_elements", true, KEY + "on_elements"));

    private final TiledCanvas screenCanvas = new TiledCanvas("frost_screen");
    private final TiledCanvas elementCanvas = new TiledCanvas("frost_elements");
    private final PixelSurfaces<Void> surfaces = this.createSurfaces(null);
    private final IceLayer[] layers = new IceLayer[PART_COUNT];
    private final float[] clocks = new float[PART_COUNT];
    private final boolean[] partOn = new boolean[PART_COUNT];
    private final boolean[] partWanted = new boolean[PART_COUNT];
    /** Bounds of the elements the element ice got shaped for, as x, y, width, height */
    private final List<int[]> frozenElements = new ArrayList<>();
    private int surfacesVersion = -1;
    private boolean resized = false;
    private final FrostArt.Walker walker = new FrostArt.Walker(0, 0);
    /** Paint a changed pixel of a part into its canvas */
    private final IceLayer.PixelListener[] repainters = {(x, y) -> this.repaintPixel(EDGES, x, y), (x, y) -> this.repaintPixel(CORNERS, x, y), (x, y) -> this.repaintPixel(ELEMENTS, x, y)};

    private final float[] chipX = new float[MAX_CHIPS];
    private final float[] chipY = new float[MAX_CHIPS];
    private final float[] chipVelocityX = new float[MAX_CHIPS];
    private final float[] chipVelocityY = new float[MAX_CHIPS];
    private final float[] chipAge = new float[MAX_CHIPS];
    private final float[] chipLife = new float[MAX_CHIPS];
    private final float[] chipTumble = new float[MAX_CHIPS];
    private final byte[] chipTone = new byte[MAX_CHIPS];
    /** 0 a single pixel, 1 a tumbling sliver, 2 a chunk */
    private final byte[] chipSize = new byte[MAX_CHIPS];
    private final boolean[] chipOnElement = new boolean[MAX_CHIPS];
    private int chipCount = 0;

    private final int[][] crackPoints = new int[MAX_CRACKS][MAX_CRACK_LENGTH * 2];
    private final int[] crackLength = new int[MAX_CRACKS];
    private final float[] crackAge = new float[MAX_CRACKS];
    private final boolean[] crackOnElement = new boolean[MAX_CRACKS];
    private int crackCount = 0;

    private boolean flashShown = false;
    private int flashX;
    private int flashY;
    private float flashAge;
    private boolean flashOnElement;

    private boolean glintShown = false;
    private int glintX;
    private int glintY;
    private int glintPart;
    private float glintAge;
    private float nextGlint = 0.5F;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {
        this.addToggle(menu, this.screenEdges, MaterialIcons.BORDER_OUTER, "screen_edges");
        this.addToggle(menu, this.corners, MaterialIcons.ROUNDED_CORNER, "corners");
        this.addToggle(menu, this.onElements, MaterialIcons.WIDGETS, "on_elements");
    }

    private void addToggle(@NotNull ContextMenu menu, @NotNull Property<Boolean> property, @NotNull MaterialIcon icon, @NotNull String name) {
        property.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(icon)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(KEY + name + ".desc")));
    }

    @Override
    protected void onResize(int width, int height) {
        this.screenCanvas.resizeToScreen(width, height);
        this.elementCanvas.resizeToScreen(width, height);
        PixelRandom random = new PixelRandom(SCREEN_SEED);
        this.layers[EDGES] = new IceLayer(width, height);
        FrostShaper.edges(this.layers[EDGES], random, width, height);
        this.layers[CORNERS] = new IceLayer(width, height);
        FrostShaper.corners(this.layers[CORNERS], random, width, height);
        this.layers[ELEMENTS] = new IceLayer(width, height);
        this.frozenElements.clear();
        this.surfacesVersion = -1;
        this.chipCount = 0;
        this.crackCount = 0;
        this.flashShown = false;
        this.glintShown = false;
        // The ice shows at once as far as its clock got, it just has to get into the new canvases
        for (int part = 0; part < PART_COUNT; part++) {
            if (this.partOn[part]) this.markAllPending(this.layers[part]);
        }
        this.resized = true;
    }

    @Override
    protected void update(float deltaSeconds) {
        boolean[] wanted = this.partWanted;
        wanted[EDGES] = this.screenEdges.tryGetNonNullElse(true);
        wanted[CORNERS] = this.corners.tryGetNonNullElse(true);
        wanted[ELEMENTS] = this.onElements.tryGetNonNullElse(true);
        for (int part = 0; part < PART_COUNT; part++) {
            IceLayer layer = this.layers[part];
            if (wanted[part] && !this.partOn[part]) {
                // Switched back on: it freezes over again, holes included
                this.clocks[part] = 0.0F;
                Arrays.fill(layer.glaze.knocked, 0.0F);
                Arrays.fill(layer.crystals.knocked, 0.0F);
                this.markAllPending(layer);
            } else if (!wanted[part] && this.partOn[part]) {
                this.partOn[part] = false;
                this.hideAll(part);
            }
            this.partOn[part] = wanted[part];
            if (wanted[part]) this.clocks[part] += deltaSeconds;
        }
        this.syncElements();
        for (int part = 0; part < PART_COUNT; part++) {
            if (this.partOn[part]) this.layers[part].settle(this.clocks[part], this.repainters[part]);
        }
        this.updateEffects(deltaSeconds);
        this.resized = false;
    }

    private void markAllPending(@NotNull IceLayer layer) {
        layer.glaze.markAllPending();
        layer.crystals.markAllPending();
    }

    /**
     * Shapes the element ice again when the elements changed. Elements that are there from the start, after a resize or that kept
     * their place follow the clock of the part, ones that show up later freeze over from scratch.
     */
    private void syncElements() {
        if (this.surfaces.version() == this.surfacesVersion) return;
        this.surfacesVersion = this.surfaces.version();
        List<int[]> boxes = new ArrayList<>();
        for (PixelSurfaces.Surface<Void> surface : this.surfaces.elements()) {
            if ((surface.width() > 0) && (surface.height() > 0)) boxes.add(new int[]{surface.x(), surface.y(), surface.width(), surface.height()});
        }
        boxes.sort(Comparator.comparingInt((int[] box) -> box[1]).thenComparingInt(box -> box[0]));
        int width = this.getWidth();
        int height = this.getHeight();
        IceLayer layer = new IceLayer(width, height);
        PixelRandom random = new PixelRandom(ELEMENT_SEED);
        for (int[] box : boxes) {
            // The first frame always follows a resize, so the elements of a fresh screen count as known
            boolean known = this.resized || containsBox(this.frozenElements, box);
            layer.setBirthDelay(known ? 0.0 : this.clocks[ELEMENTS]);
            int up = freeRowsAbove(boxes, box);
            int down = freeRowsBelow(boxes, box, height);
            FrostShaper.element(layer, random, box[0], box[1], box[2], box[3], up, down);
        }
        layer.setBirthDelay(0.0);
        this.frozenElements.clear();
        this.frozenElements.addAll(boxes);
        this.layers[ELEMENTS] = layer;
        this.elementCanvas.clearAll();
        if (this.partOn[ELEMENTS]) this.markAllPending(layer);
    }

    private static boolean containsBox(@NotNull List<int[]> boxes, @NotNull int[] box) {
        for (int[] other : boxes) {
            if ((other[0] == box[0]) && (other[1] == box[1]) && (other[2] == box[2]) && (other[3] == box[3])) return true;
        }
        return false;
    }

    /**
     * Rows the ice may use above an element: half the gap to the element above it, or 5 below the top of the screen.
     */
    private static int freeRowsAbove(@NotNull List<int[]> boxes, @NotNull int[] box) {
        int gap = -1;
        for (int[] other : boxes) {
            if ((other == box) || !overlapsHorizontally(other, box) || (other[1] + other[3] > box[1])) continue;
            int g = box[1] - (other[1] + other[3]);
            if ((gap < 0) || (g < gap)) gap = g;
        }
        return (gap < 0) ? 5 : (int)Math.ceil(gap / 2.0);
    }

    /**
     * Rows the ice may use below an element: half the gap to the element below it plus one, or 14.
     */
    private static int freeRowsBelow(@NotNull List<int[]> boxes, @NotNull int[] box, int screenHeight) {
        int gap = -1;
        int bottom = box[1] + box[3];
        for (int[] other : boxes) {
            if ((other == box) || !overlapsHorizontally(other, box) || (other[1] < bottom)) continue;
            int g = other[1] - bottom;
            if ((gap < 0) || (g < gap)) gap = g;
        }
        return (gap < 0) ? 14 : (gap / 2 + 1);
    }

    private static boolean overlapsHorizontally(@NotNull int[] a, @NotNull int[] b) {
        return (a[0] < b[0] + b[2]) && (b[0] < a[0] + a[2]);
    }

    private void hideAll(int part) {
        this.hideAll(part, this.layers[part].glaze);
        this.hideAll(part, this.layers[part].crystals);
    }

    private void hideAll(int part, @NotNull IceLayer.Pixels pixels) {
        pixels.clearPending();
        for (int i = 0; i < pixels.count; i++) {
            if (pixels.shown[i] == IceLayer.HIDDEN) continue;
            pixels.shown[i] = IceLayer.HIDDEN;
            this.repaintPixel(part, pixels.x[i], pixels.y[i]);
        }
    }

    /**
     * Paints one pixel of the canvas of a part again from all ice there: the glaze of the parts that share the canvas first,
     * then their crystals, like the mockup draws all glaze before all crystals.
     */
    private void repaintPixel(int part, int x, int y) {
        boolean element = part == ELEMENTS;
        int firstPart = element ? ELEMENTS : EDGES;
        int lastPart = element ? ELEMENTS : CORNERS;
        int key = y * this.getWidth() + x;
        int color = 0;
        for (int l = firstPart; l <= lastPart; l++) {
            IceLayer layer = this.layers[l];
            int i = layer.glazeAt(key);
            if ((i < 0) || (layer.glaze.shown[i] == IceLayer.HIDDEN)) continue;
            int alpha = FrostArt.GLAZE_ALPHA[layer.glaze.value[i]];
            if (layer.glaze.shown[i] == IceLayer.FRESH) alpha >>= 1;
            color = PixelMath.blendOver(color, PixelMath.withAlpha(FrostArt.GLAZE, alpha));
        }
        for (int l = firstPart; l <= lastPart; l++) {
            IceLayer layer = this.layers[l];
            int i = layer.crystalAt(key);
            if ((i < 0) || (layer.crystals.shown[i] == IceLayer.HIDDEN)) continue;
            int tone = layer.crystals.value[i];
            // New ice shows white for a moment before it settles
            int rgb = (layer.crystals.shown[i] == IceLayer.FRESH) ? 0xFFFFFF : FrostArt.TONE[tone];
            color = PixelMath.blendOver(color, PixelMath.withAlpha(rgb, (tone == 3) ? FrostArt.SHADED_TONE_ALPHA : 255));
        }
        (element ? this.elementCanvas : this.screenCanvas).set(x, y, color);
    }

    /**
     * What a switched-on part shows at a pixel right now: 2 for a crystal, 1 for glaze only, 0 for no ice.
     */
    private int iceAt(int x, int y) {
        if ((x < 0) || (y < 0) || (x >= this.getWidth()) || (y >= this.getHeight())) return 0;
        int key = y * this.getWidth() + x;
        int found = 0;
        for (int part = 0; part < PART_COUNT; part++) {
            if (!this.partOn[part]) continue;
            IceLayer layer = this.layers[part];
            int i = layer.crystalAt(key);
            if ((i >= 0) && (layer.crystals.age(i, this.clocks[part]) >= 0.0F)) return 2;
            i = layer.glazeAt(key);
            if ((i >= 0) && (layer.glaze.age(i, this.clocks[part]) >= 0.0F)) found = 1;
        }
        return found;
    }

    /**
     * A click works like a hammer blow, only at the spot it hits: the ice inside a jagged patch around it breaks out, chips fly off,
     * cracks run into the ice around the hole, and after a while the hole freezes over again from its rim.
     */
    @Override
    protected void onClick(float x, float y, boolean primaryButton) {
        if (!primaryButton || (this.layers[EDGES] == null)) return;
        int mx = (int)Math.round(x);
        int my = (int)Math.round(y);
        float phase = this.random.next() * 9.0F;
        int hits = 0;
        boolean onElement = false;
        int reach = BLOW_RADIUS + 4;
        for (int yy = Math.max(0, my - reach); yy <= Math.min(this.getHeight() - 1, my + reach); yy++) {
            for (int xx = Math.max(0, mx - reach); xx <= Math.min(this.getWidth() - 1, mx + reach); xx++) {
                int dx = xx - mx;
                int dy = yy - my;
                double distance = Math.hypot(dx, dy);
                double angle = Math.atan2(dy, dx);
                double limit = BLOW_RADIUS * (0.72 + 0.4 * PixelMath.valueNoise((float)(angle * 2.2 + phase), phase) + 0.14 * PixelMath.tri((float)(angle * 9.0 + phase)));
                if (distance >= limit) continue;
                int key = yy * this.getWidth() + xx;
                for (int part = 0; part < PART_COUNT; part++) {
                    if (!this.partOn[part]) continue;
                    IceLayer layer = this.layers[part];
                    float clock = this.clocks[part];
                    if (this.knock(layer.crystals, layer.crystalAt(key), clock, distance, limit, dx, dy, xx, yy, 0.45F, part)) {
                        hits++;
                        if (part == ELEMENTS) onElement = true;
                    }
                    if (this.knock(layer.glaze, layer.glazeAt(key), clock, distance, limit, dx, dy, xx, yy, 0.07F, part)) {
                        hits++;
                        if (part == ELEMENTS) onElement = true;
                    }
                }
            }
        }
        if (hits == 0) return;
        this.flashShown = true;
        this.flashX = mx;
        this.flashY = my;
        this.flashAge = 0.0F;
        this.flashOnElement = onElement;
        int firstDirection = this.random.rangeInt(0, 15);
        for (int k = 0; k < CRACKS_PER_BLOW; k++) {
            // The oldest cracks make room for new ones
            if (this.crackCount == MAX_CRACKS) this.removeCrack(0);
            int c = this.crackCount++;
            int direction = firstDirection + k * 3 + this.random.rangeInt(-1, 1);
            int length = Math.min(MAX_CRACK_LENGTH, (int)Math.round(BLOW_RADIUS * (1.25 + this.random.next() * 0.7)));
            this.walker.reset(mx, my);
            int[] points = this.crackPoints[c];
            for (int i = 0; i < length; i++) {
                FrostArt.step(this.walker, direction);
                points[i * 2] = this.walker.x;
                points[i * 2 + 1] = this.walker.y;
            }
            this.crackLength[c] = length;
            this.crackAge[c] = 0.0F;
            this.crackOnElement[c] = onElement;
        }
    }

    /**
     * Knocks one ice pixel out of a blow's patch. Some pixels fly off as chips.
     *
     * @return whether there was ice to knock out
     */
    private boolean knock(@NotNull IceLayer.Pixels pixels, int index, float clock, double distance, double limit, int dx, int dy, int x, int y, float chipChance, int part) {
        if (!pixels.knock(index, clock, distance, limit)) return false;
        if ((this.chipCount < MAX_CHIPS) && (this.random.next() < chipChance)) {
            int c = this.chipCount++;
            float speed = this.random.range(25.0F, 80.0F);
            float unit = (distance > 0.0) ? (float)distance : 1.0F;
            this.chipX[c] = x;
            this.chipY[c] = y;
            this.chipVelocityX[c] = dx / unit * speed + this.random.range(-14.0F, 14.0F);
            this.chipVelocityY[c] = dy / unit * speed - this.random.range(15.0F, 55.0F);
            this.chipAge[c] = 0.0F;
            this.chipLife[c] = this.random.range(0.5F, 1.05F);
            this.chipTone[c] = (byte)((pixels == this.layers[part].crystals) ? pixels.value[index] : 2);
            this.chipSize[c] = (byte)((this.random.next() < 0.1F) ? 2 : ((this.random.next() < 0.3F) ? 1 : 0));
            this.chipTumble[c] = this.random.range(0.0F, 9.0F);
            this.chipOnElement[c] = part == ELEMENTS;
        }
        return true;
    }

    private void updateEffects(float deltaSeconds) {
        for (int c = this.chipCount - 1; c >= 0; c--) {
            this.chipAge[c] += deltaSeconds;
            this.chipVelocityY[c] += 190.0F * deltaSeconds;
            this.chipX[c] += this.chipVelocityX[c] * deltaSeconds;
            this.chipY[c] += this.chipVelocityY[c] * deltaSeconds;
            if ((this.chipAge[c] > this.chipLife[c]) || (this.chipY[c] > this.getHeight() + 2)) this.removeChip(c);
        }
        for (int c = this.crackCount - 1; c >= 0; c--) {
            this.crackAge[c] += deltaSeconds;
            if (this.crackAge[c] > CRACK_LIFE) this.removeCrack(c);
        }
        if (this.flashShown && ((this.flashAge += deltaSeconds) > FLASH_LIFE)) this.flashShown = false;
        if (this.glintShown && ((this.glintAge += deltaSeconds) > GLINT_LIFE)) this.glintShown = false;
        if ((this.nextGlint -= deltaSeconds) <= 0.0F) {
            this.nextGlint = this.random.range(0.25F, 0.7F);
            this.pickGlint();
        }
    }

    /**
     * Crystals glint now and then: a random crystal of a part that is on, if it is lit and has been frozen for a while.
     */
    private void pickGlint() {
        int total = 0;
        for (IceLayer layer : this.layers) total += layer.crystals.count;
        if (total == 0) return;
        int pick = this.random.index(total);
        for (int part = 0; part < PART_COUNT; part++) {
            IceLayer.Pixels crystals = this.layers[part].crystals;
            if (pick >= crystals.count) {
                pick -= crystals.count;
                continue;
            }
            if (this.partOn[part] && (crystals.value[pick] < 2) && (crystals.age(pick, this.clocks[part]) > 1.0F)) {
                this.glintShown = true;
                this.glintX = crystals.x[pick];
                this.glintY = crystals.y[pick];
                this.glintPart = part;
                this.glintAge = 0.0F;
            }
            return;
        }
    }

    private void removeChip(int c) {
        int last = --this.chipCount;
        this.chipX[c] = this.chipX[last];
        this.chipY[c] = this.chipY[last];
        this.chipVelocityX[c] = this.chipVelocityX[last];
        this.chipVelocityY[c] = this.chipVelocityY[last];
        this.chipAge[c] = this.chipAge[last];
        this.chipLife[c] = this.chipLife[last];
        this.chipTumble[c] = this.chipTumble[last];
        this.chipTone[c] = this.chipTone[last];
        this.chipSize[c] = this.chipSize[last];
        this.chipOnElement[c] = this.chipOnElement[last];
    }

    private void removeCrack(int c) {
        int last = --this.crackCount;
        int[] points = this.crackPoints[c];
        this.crackPoints[c] = this.crackPoints[last];
        this.crackPoints[last] = points;
        this.crackLength[c] = this.crackLength[last];
        this.crackAge[c] = this.crackAge[last];
        this.crackOnElement[c] = this.crackOnElement[last];
    }

    @Override
    protected void drawElement(@NotNull PixelGraphics graphics) {
        this.elementCanvas.draw(graphics);
        this.drawEffects(graphics, true);
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        this.screenCanvas.draw(graphics);
        this.drawEffects(graphics, false);
    }

    /**
     * Draws the glint, cracks, chips and the flash that belong to the element ice or to the screen ice.
     */
    private void drawEffects(@NotNull PixelGraphics graphics, boolean element) {
        if (this.glintShown && ((this.glintPart == ELEMENTS) == element) && this.partOn[this.glintPart]) {
            boolean star = (this.glintAge > 0.1F) && (this.glintAge < 0.3F);
            if (star) {
                graphics.sprite(GLINT_STAR, GLINT_PALETTE, this.glintX - 1, this.glintY - 1);
            } else {
                graphics.sprite(GLINT_DOT, GLINT_PALETTE, this.glintX, this.glintY);
            }
        }
        // Cracks only show on ice that is still there, so they start at the rim of the hole: dark on crystals, white on glaze
        for (int c = 0; c < this.crackCount; c++) {
            if (this.crackOnElement[c] != element) continue;
            float age = this.crackAge[c];
            int alpha = (age < 0.15F) ? 255 : ((age < 0.5F) ? 190 : 100);
            int[] points = this.crackPoints[c];
            for (int i = 0; i < this.crackLength[c]; i++) {
                int x = points[i * 2];
                int y = points[i * 2 + 1];
                int ice = this.iceAt(x, y);
                if (ice != 0) graphics.pixel(x, y, PixelMath.withAlpha((ice > 1) ? FrostArt.CRACK_ON_CRYSTAL : 0xFFFFFF, alpha));
            }
        }
        for (int c = 0; c < this.chipCount; c++) {
            if (this.chipOnElement[c] != element) continue;
            int alpha = (this.chipAge[c] > this.chipLife[c] * 0.7F) ? 140 : 255;
            PixelSprite sprite = switch (this.chipSize[c]) {
                case 2 -> CHIP_CHUNK;
                case 1 -> ((PixelMath.floor(this.chipAge[c] * 12.0F + this.chipTumble[c]) & 1) != 0) ? CHIP_FLAT : CHIP_TALL;
                default -> CHIP_DOT;
            };
            graphics.sprite(sprite, CHIP_PALETTES[this.chipTone[c]], PixelMath.floor(this.chipX[c]), PixelMath.floor(this.chipY[c]), false, false, 1, PixelMath.withAlpha(0xFFFFFF, alpha));
        }
        if (this.flashShown && (this.flashOnElement == element)) {
            if (this.flashAge >= 0.06F) {
                graphics.sprite(FLASH_FAR, FLASH_PALETTE, this.flashX - 5, this.flashY - 5);
            } else {
                graphics.sprite(FLASH_NEAR, FLASH_PALETTE, this.flashX - 2, this.flashY - 2);
            }
        }
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        this.screenCanvas.releaseTextures();
        this.elementCanvas.releaseTextures();
    }

}
