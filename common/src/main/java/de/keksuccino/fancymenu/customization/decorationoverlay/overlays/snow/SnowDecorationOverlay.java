package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.OverlayColorTint;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.ParticleLayer;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Snow falling in four depth layers: one-pixel dust far behind the elements, 3 px and 5 px flakes in the middle and a few 7 px crystals up
 * front. Size, speed and sway grow with nearness, and flakes tumble between a "+" and an "×" frame. Snow settles as a rounded cap on
 * every element and along the bottom of the screen, landing flakes kick up a puff, caps glint now and then and icicles grow under elements.
 * <br><br>
 * Parts: the dust is {@code BACK}. What belongs to an element (its cap, its icicles, glints and puffs on its cap) is {@code ELEMENT}, so it stays
 * in front of the element and of background elements whatever the Layers setting. The falling flakes and the snow along the bottom of the
 * screen are {@code FRONT}. Everything is laid out for the mockup stage of 480x270 GUI pixels and scales its counts with the screen area.
 */
public class SnowDecorationOverlay extends PixelDecorationOverlay<SnowDecorationOverlay> {

    static final float MIN_SPEED = 0.01F;
    static final float MAX_SPEED = 5.0F;
    /** Screen area of the mockup stage (480x270), which the flake counts are made for */
    private static final float STAGE_AREA = 480.0F * 270.0F;
    private static final float MAX_AREA_FACTOR = 4.0F;
    private static final int LAYERS = 4;
    private static final int[] LAYER_COUNT = {70, 44, 15, 4};
    private static final float[] LAYER_SPEED_MIN = {9.0F, 15.0F, 22.0F, 28.0F};
    private static final float[] LAYER_SPEED_MAX = {15.0F, 23.0F, 31.0F, 36.0F};
    private static final float[] LAYER_SWAY_MIN = {1.0F, 2.0F, 3.0F, 4.0F};
    private static final float[] LAYER_SWAY_MAX = {3.0F, 5.0F, 7.0F, 9.0F};
    private static final float[] LAYER_WIND = {0.5F, 0.8F, 1.0F, 1.2F};
    private static final int[] LAYER_SPRITE_SIZE = {1, 3, 5, 7};
    /** Snow a landing flake adds to a cap, in art pixels. Dust never lands. */
    private static final float[] LAYER_DEPOSIT = {0.0F, 0.35F, 0.8F, 1.2F};
    private static final int MAX_PUFFS = 128;
    private static final int MAX_GLINTS = 16;
    private static final float PUFF_LIFE = 0.3F;
    private static final float GLINT_LIFE = 0.45F;

    public final DecorationOverlayOptions.Amount amount = new DecorationOverlayOptions.Amount(this);
    public final DecorationOverlayOptions.Size size = new DecorationOverlayOptions.Size(this);
    public final Property.FloatProperty snowSpeed = putProperty(Property.floatProperty("snow_speed", 1.0F, "fancymenu.decoration_overlays.snow.speed"));
    public final Property<Boolean> snowAccumulation = putProperty(Property.booleanProperty("snow_accumulation", true, "fancymenu.decoration_overlays.snow.accumulate_snow"));
    public final Property.ColorProperty snowColorHex = putProperty(Property.hexColorProperty("snow_color_hex", "#FFFFFF", true, "fancymenu.decoration_overlays.snow.color"));

    private final OverlayColorTint tint = new OverlayColorTint(SnowArt.REFERENCE_COLOR);
    private final SnowArt art = new SnowArt(this.tint);
    @SuppressWarnings("unchecked")
    private final List<Flake>[] flakes = new List[LAYERS];
    private final Puff[] puffs = new Puff[MAX_PUFFS];
    private int puffCount = 0;
    private final Glint[] glints = new Glint[MAX_GLINTS];
    private int glintCount = 0;
    private final PixelSurfaces<SnowPile> surfaces = this.createSurfaces(surface -> new SnowPile(surface, this.scale, this.random));
    /** Caps and icicles of elements, drawn as element part */
    private final PixelCanvas elementCapCanvas = this.createScreenCanvas("snow_element_caps");
    /** The snow along the bottom of the screen, drawn as front part */
    private final PixelCanvas floorCapCanvas = this.createScreenCanvas("snow_floor_cap");
    private final ParticleLayer backLayer = new ParticleLayer("snow_back");
    private final ParticleLayer frontLayer = new ParticleLayer("snow_front");
    private final ParticleLayer elementLayer = new ParticleLayer("snow_element");
    private int scale = 1;
    private float speed = 1.0F;
    private boolean accumulate = true;
    private int countedWidth = -1;
    private int countedHeight = -1;
    private float countedAmount = -1.0F;
    private int drawnSurfacesVersion = -1;
    /** Set when the whole cap canvas has to be drawn again (new size, new color, caps switched on) */
    private boolean capsStale = true;
    private float nextGlint = 0.3F;

    public SnowDecorationOverlay() {
        for (int l = 0; l < LAYERS; l++) this.flakes[l] = new ArrayList<>();
        for (int i = 0; i < MAX_PUFFS; i++) this.puffs[i] = new Puff();
        for (int i = 0; i < MAX_GLINTS; i++) this.glints[i] = new Glint();
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.snowAccumulation.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SNOWING)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.accumulate_snow.desc")));

        this.snowColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.color.desc")));

        this.amount.addEntry(menu, this);

        this.size.addEntry(menu, this);

        this.snowSpeed.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SPEED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.snow.speed.desc")));

    }

    @Override
    protected void onResize(int width, int height) {
        this.countedWidth = -1;
        this.capsStale = true;
    }

    @Override
    protected void update(float deltaSeconds) {
        this.readOptions();
        int width = this.getWidth();
        int height = this.getHeight();
        float amountFactor = this.amount.getFactor();
        if ((width != this.countedWidth) || (height != this.countedHeight) || (amountFactor != this.countedAmount)) {
            boolean refill = (width != this.countedWidth) || (height != this.countedHeight);
            this.fitFlakeCounts(width, height, amountFactor, refill);
        }
        float time = this.getTime();
        float wind = 6.0F * (float)Math.sin(time * 0.23F) + 4.0F * (float)Math.sin(time * 0.61F + 1.7F);
        List<PixelSurfaces.Surface<SnowPile>> all = this.surfaces.all();
        for (int l = 0; l < LAYERS; l++) {
            List<Flake> layer = this.flakes[l];
            for (int i = 0; i < layer.size(); i++) this.moveFlake(layer.get(i), deltaSeconds, wind, width, height, all);
        }
        if (this.accumulate) {
            for (int i = 0; i < all.size(); i++) {
                SnowPile pile = all.get(i).state();
                if (pile != null) pile.advance(deltaSeconds);
            }
            this.updatePuffs(deltaSeconds);
            this.updateGlints(deltaSeconds, all);
            this.drawCapsIntoCanvas(all);
        }
    }

    private void readOptions() {
        if (this.tint.update(this.snowColorHex)) {
            this.art.rebuild(this.tint);
            this.capsStale = true;
        }
        int newScale = this.size.get();
        if (newScale != this.scale) {
            this.scale = newScale;
            // Caps are built on a grid of art pixels, so a new size needs new caps
            List<PixelSurfaces.Surface<SnowPile>> all = this.surfaces.all();
            for (int i = 0; i < all.size(); i++) all.get(i).setState(new SnowPile(all.get(i), newScale, this.random));
            // New piles have to be fitted under the elements again
            this.drawnSurfacesVersion = -1;
            this.capsStale = true;
        }
        this.speed = PixelMath.clamp(this.snowSpeed.getFloat(), MIN_SPEED, MAX_SPEED);
        boolean newAccumulate = this.snowAccumulation.tryGetNonNullElse(true);
        if (newAccumulate != this.accumulate) {
            this.accumulate = newAccumulate;
            this.capsStale = true;
            this.puffCount = 0;
            this.glintCount = 0;
        }
    }

    /**
     * Grows or shrinks every layer to its wanted number of flakes. A new screen size spreads all flakes over the screen again.
     */
    private void fitFlakeCounts(int width, int height, float amountFactor, boolean refill) {
        this.countedWidth = width;
        this.countedHeight = height;
        this.countedAmount = amountFactor;
        float areaFactor = PixelMath.clamp((width * height) / STAGE_AREA, 0.1F, MAX_AREA_FACTOR);
        for (int l = 0; l < LAYERS; l++) {
            List<Flake> layer = this.flakes[l];
            int wanted = Math.round(LAYER_COUNT[l] * areaFactor * amountFactor);
            if (refill) {
                for (int i = 0; i < layer.size(); i++) this.resetFlake(layer.get(i), true, width, height);
            }
            while (layer.size() < wanted) {
                Flake flake = new Flake();
                flake.layer = l;
                this.resetFlake(flake, true, width, height);
                layer.add(flake);
            }
            while (layer.size() > wanted) layer.removeLast();
        }
    }

    /**
     * Gives a flake new random values. Flakes of a new screen start anywhere on it, all others start above the top edge.
     */
    private void resetFlake(@NotNull Flake flake, boolean anywhere, int width, int height) {
        int l = flake.layer;
        flake.x = this.random.range(-8.0F, width + 8.0F);
        flake.y = anywhere ? this.random.range(-8.0F, height) : -this.random.range(8.0F, 50.0F);
        flake.fallSpeed = this.random.range(LAYER_SPEED_MIN[l], LAYER_SPEED_MAX[l]);
        flake.swayAmplitude = this.random.range(LAYER_SWAY_MIN[l], LAYER_SWAY_MAX[l]);
        flake.swaySpeed = this.random.range(0.5F, 1.3F);
        flake.phase = this.random.range(0.0F, PixelMath.TAU);
        flake.tumbleRate = this.random.range(1.5F, 3.5F);
        flake.tumbleOffset = this.random.range(0.0F, 9.0F);
    }

    private void moveFlake(@NotNull Flake flake, float deltaSeconds, float wind, int width, int height, @NotNull List<PixelSurfaces.Surface<SnowPile>> all) {
        int l = flake.layer;
        int spriteSize = LAYER_SPRITE_SIZE[l] * this.scale;
        float previousBottom = flake.y + spriteSize;
        flake.y += flake.fallSpeed * this.speed * deltaSeconds;
        flake.x += wind * LAYER_WIND[l] * this.speed * deltaSeconds;
        flake.phase += flake.swaySpeed * deltaSeconds;
        if (this.accumulate && (l > 0) && this.tryLand(flake, previousBottom, spriteSize, all)) {
            this.resetFlake(flake, false, width, height);
            return;
        }
        if (flake.y > height + 8) this.resetFlake(flake, false, width, height);
        if (flake.x < -12.0F) {
            flake.x += width + 24;
        } else if (flake.x > width + 12.0F) {
            flake.x -= width + 24;
        }
    }

    /**
     * Lands the flake on the first cap whose top it passed in this frame: adds its snow there and kicks up a puff for the bigger flakes.
     */
    private boolean tryLand(@NotNull Flake flake, float previousBottom, int spriteSize, @NotNull List<PixelSurfaces.Surface<SnowPile>> all) {
        float centerX = this.swayedX(flake) + spriteSize / 2.0F;
        float bottom = flake.y + spriteSize;
        for (int i = 0; i < all.size(); i++) {
            SnowPile pile = all.get(i).state();
            if (pile == null) continue;
            int column = pile.columnAt(centerX);
            if ((column < 0) || (column >= pile.columns)) continue;
            int top = pile.topY(column);
            if ((previousBottom <= top) && (bottom > top)) {
                pile.deposit(column, LAYER_DEPOSIT[flake.layer]);
                if (flake.layer > 1) {
                    this.spawnPuff(centerX, top - this.scale, -1, pile.floor);
                    this.spawnPuff(centerX, top - this.scale, 1, pile.floor);
                }
                return true;
            }
        }
        return false;
    }

    private float swayedX(@NotNull Flake flake) {
        return flake.x + (float)Math.sin(flake.phase) * flake.swayAmplitude * this.scale;
    }

    private void spawnPuff(float x, float y, int direction, boolean onFloor) {
        if (this.puffCount >= MAX_PUFFS) return;
        Puff puff = this.puffs[this.puffCount++];
        puff.x = x;
        puff.y = y;
        puff.vx = direction * this.random.range(6.0F, 16.0F) * this.scale;
        puff.vy = -this.random.range(8.0F, 18.0F) * this.scale;
        puff.age = 0.0F;
        puff.onFloor = onFloor;
    }

    private void updatePuffs(float deltaSeconds) {
        int kept = 0;
        for (int i = 0; i < this.puffCount; i++) {
            Puff puff = this.puffs[i];
            puff.age += deltaSeconds;
            puff.vy += 70.0F * this.scale * deltaSeconds;
            puff.x += puff.vx * deltaSeconds;
            puff.y += puff.vy * deltaSeconds;
            if (puff.age > PUFF_LIFE) continue;
            // Moves the living puffs to the front and keeps the dead objects behind them for reuse
            this.puffs[i] = this.puffs[kept];
            this.puffs[kept++] = puff;
        }
        this.puffCount = kept;
    }

    private void updateGlints(float deltaSeconds, @NotNull List<PixelSurfaces.Surface<SnowPile>> all) {
        int kept = 0;
        for (int i = 0; i < this.glintCount; i++) {
            Glint glint = this.glints[i];
            glint.age += deltaSeconds;
            if (glint.age > GLINT_LIFE) continue;
            this.glints[i] = this.glints[kept];
            this.glints[kept++] = glint;
        }
        this.glintCount = kept;
        this.nextGlint -= deltaSeconds;
        if (this.nextGlint > 0.0F) return;
        this.nextGlint = this.random.range(0.25F, 0.6F);
        if (all.isEmpty() || (this.glintCount >= MAX_GLINTS)) return;
        SnowPile pile = all.get(this.random.index(all.size())).state();
        if ((pile == null) || (pile.columns < 3)) return;
        int column = this.random.rangeInt(1, pile.columns - 2);
        int h = Math.round(pile.heights[column]);
        if (h < 2) return;
        Glint glint = this.glints[this.glintCount++];
        glint.x = pile.x0 + column * this.scale;
        glint.y = pile.surfaceY - (h + 1) * this.scale;
        glint.age = 0.0F;
        glint.onFloor = pile.floor;
    }

    /**
     * Brings the cap canvas up to date. Only caps that changed get drawn again, together with the caps whose area overlaps theirs.
     * When the elements changed, all caps and icicles get fitted between the elements around them first.
     */
    private void drawCapsIntoCanvas(@NotNull List<PixelSurfaces.Surface<SnowPile>> all) {
        if (this.drawnSurfacesVersion != this.surfaces.version()) {
            this.drawnSurfacesVersion = this.surfaces.version();
            List<PixelSurfaces.Surface<SnowPile>> elements = this.surfaces.elements();
            for (int i = 0; i < all.size(); i++) {
                SnowPile pile = all.get(i).state();
                if (pile != null) pile.fitBetweenElements(all.get(i), elements);
            }
            this.capsStale = true;
        }
        if (this.capsStale) {
            this.capsStale = false;
            this.elementCapCanvas.clear();
            this.floorCapCanvas.clear();
            for (int i = 0; i < all.size(); i++) {
                SnowPile pile = all.get(i).state();
                if (pile != null) pile.draw(this.canvasOf(pile), this.art);
            }
            return;
        }
        boolean anyCleared = false;
        for (int i = 0; i < all.size(); i++) {
            SnowPile pile = all.get(i).state();
            if ((pile == null) || !pile.needsDraw) continue;
            pile.clearArea(this.canvasOf(pile));
            anyCleared = true;
            for (int j = 0; j < all.size(); j++) {
                SnowPile other = all.get(j).state();
                if ((other != null) && (other != pile) && (other.floor == pile.floor) && other.overlapsArea(pile)) other.needsDraw = true;
            }
        }
        if (!anyCleared) return;
        for (int i = 0; i < all.size(); i++) {
            SnowPile pile = all.get(i).state();
            if ((pile != null) && pile.needsDraw) pile.draw(this.canvasOf(pile), this.art);
        }
    }

    @Override
    protected void drawBack(@NotNull PixelGraphics graphics) {
        int n = this.scale;
        int color = this.art.dust;
        List<Flake> dust = this.flakes[0];
        ParticleLayer layer = this.backLayer;
        layer.begin(graphics, dust.size());
        for (int i = 0; i < dust.size(); i++) {
            Flake flake = dust.get(i);
            layer.rect(PixelMath.floor(this.swayedX(flake)), PixelMath.floor(flake.y), n, n, color);
        }
        layer.end();
    }

    @NotNull
    private PixelCanvas canvasOf(@NotNull SnowPile pile) {
        return pile.floor ? this.floorCapCanvas : this.elementCapCanvas;
    }

    @Override
    protected void drawElement(@NotNull PixelGraphics graphics) {
        if (!this.accumulate) return;
        graphics.canvas(this.elementCapCanvas, 0, 0);
        ParticleLayer layer = this.elementLayer;
        layer.begin(graphics, this.puffCount + this.glintCount);
        this.drawPuffsAndGlints(layer, false);
        layer.end();
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        int n = this.scale;
        if (this.accumulate) graphics.canvas(this.floorCapCanvas, 0, 0);
        ParticleLayer layer = this.frontLayer;
        layer.begin(graphics, this.flakes[1].size() + this.flakes[2].size() + this.flakes[3].size() + this.puffCount + this.glintCount);
        float time = this.getTime();
        for (int l = 1; l < LAYERS; l++) {
            PixelSprite[] frames = SnowArt.FLAKES[l];
            List<Flake> layerFlakes = this.flakes[l];
            for (int i = 0; i < layerFlakes.size(); i++) {
                Flake flake = layerFlakes.get(i);
                PixelSprite frame = frames[PixelMath.floor(time * flake.tumbleRate + flake.tumbleOffset) & 1];
                layer.sprite(frame, this.art.flakePalettes[l], PixelMath.floor(this.swayedX(flake)), PixelMath.floor(flake.y), n);
            }
        }
        if (this.accumulate) this.drawPuffsAndGlints(layer, true);
        layer.end();
    }

    /**
     * Draws the puffs and glints of either the floor or the elements: the ones on caps of elements belong to their element.
     */
    private void drawPuffsAndGlints(@NotNull ParticleLayer layer, boolean onFloor) {
        int n = this.scale;
        for (int i = 0; i < this.puffCount; i++) {
            Puff puff = this.puffs[i];
            if (puff.onFloor == onFloor) layer.rect(PixelMath.floor(puff.x), PixelMath.floor(puff.y), n, n, (puff.age < 0.18F) ? this.art.puff : this.art.puffFaded);
        }
        for (int i = 0; i < this.glintCount; i++) {
            Glint glint = this.glints[i];
            if (glint.onFloor != onFloor) continue;
            if ((glint.age > 0.12F) && (glint.age < 0.32F)) {
                layer.sprite(SnowArt.GLINT_CROSS, this.art.glintPalette, glint.x - n, glint.y - n, n);
            } else {
                layer.rect(glint.x, glint.y, n, n, this.art.glintCore);
            }
        }
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        this.backLayer.release();
        this.frontLayer.release();
        this.elementLayer.release();
    }

    private static final class Flake {

        int layer;
        float x;
        float y;
        float fallSpeed;
        float swayAmplitude;
        float swaySpeed;
        float phase;
        float tumbleRate;
        float tumbleOffset;

    }

    private static final class Puff {

        float x;
        float y;
        float vx;
        float vy;
        float age;
        boolean onFloor;

    }

    private static final class Glint {

        int x;
        int y;
        float age;
        boolean onFloor;

    }

}
