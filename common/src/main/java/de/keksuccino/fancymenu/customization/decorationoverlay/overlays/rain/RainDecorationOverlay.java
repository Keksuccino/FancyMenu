package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.OverlayColorTint;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.ParticleLayer;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
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
 * Rain in two layers: short dim streaks behind the elements and long bright ones in front, all along the same wind angle.
 * Near streaks splash on every surface with two or three droplets and send a ripple along a thin wet film. Drips bead up under elements,
 * let go and splash on whatever is below. Thunder draws a branching bolt behind the elements with a restrike, a dithered afterimage and a
 * soft flash over the whole screen.<br><br>
 *
 * Parts: far streaks and the bolt are {@code BACK}. The wet film on an element with its ripples and the drips beading up under an element are
 * {@code ELEMENT}, so they stay in front of the element and of background elements whatever the Layers setting. The flash, the film along the
 * bottom of the screen, near streaks, falling drips and splashes are {@code FRONT}.
 * Everything is laid out for the mockup stage of 480x270 GUI pixels and scales its counts with the screen area.
 */
public class RainDecorationOverlay extends PixelDecorationOverlay<RainDecorationOverlay> {

    static final float MIN_THUNDER_BRIGHTNESS = 0.1F;
    static final float MAX_THUNDER_BRIGHTNESS = 2.0F;
    /** Screen size of the mockup stage (480x270), which the counts and spawn margins are made for */
    private static final float STAGE_HEIGHT = 270.0F;
    private static final float STAGE_AREA = 480.0F * 270.0F;
    private static final float MAX_AREA_FACTOR = 4.0F;
    private static final int FAR_COUNT = 150;
    private static final int NEAR_COUNT = 84;
    private static final int MAX_DROPS = 2048;
    private static final int MAX_RIPPLES = 1024;
    private static final int MAX_HITS = 512;
    private static final float RIPPLE_LIFE = 0.36F;
    private static final float HIT_LIFE = 0.06F;
    private static final int FLASH_COLOR = 0xCFE0FF;
    private static final int FLASH_STRIKE_ALPHA = 38;
    private static final int FLASH_RESTRIKE_ALPHA = 20;

    public final DecorationOverlayOptions.Amount amount = new DecorationOverlayOptions.Amount(this);
    public final DecorationOverlayOptions.Size size = new DecorationOverlayOptions.Size(this);
    public final Property.FloatProperty rainThunderBrightness = putProperty(Property.floatProperty("rain_thunder_brightness", 1.0F, "fancymenu.decoration_overlays.rain.thunder_brightness"));
    public final Property<Boolean> rainPuddles = putProperty(Property.booleanProperty("rain_puddles", true, "fancymenu.decoration_overlays.rain.puddles"));
    public final Property<Boolean> rainDrips = putProperty(Property.booleanProperty("rain_drips", true, "fancymenu.decoration_overlays.rain.drips"));
    public final Property<Boolean> rainThunder = putProperty(Property.booleanProperty("rain_thunder", false, "fancymenu.decoration_overlays.rain.thunder"));
    public final Property.ColorProperty rainColorHex = putProperty(Property.hexColorProperty("rain_color_hex", "#CFE7FF", true, "fancymenu.decoration_overlays.rain.color"));

    private final OverlayColorTint tint = new OverlayColorTint(RainArt.REFERENCE_COLOR);
    private final RainArt art = new RainArt(this.tint);
    private final List<Streak> far = new ArrayList<>();
    private final List<Streak> near = new ArrayList<>();
    private final Drop[] drops = new Drop[MAX_DROPS];
    private int dropCount = 0;
    private final Ripple[] ripples = new Ripple[MAX_RIPPLES];
    private int rippleCount = 0;
    private final Hit[] hits = new Hit[MAX_HITS];
    private int hitCount = 0;
    private final PixelSurfaces<RainSurface> surfaces = this.createSurfaces(surface -> new RainSurface(surface, this.random));
    private final PixelCanvas boltCanvas = this.createScreenCanvas("rain_bolt");
    private final RainBolt bolt = new RainBolt();
    private final ParticleLayer backLayer = new ParticleLayer("rain_back");
    private final ParticleLayer frontLayer = new ParticleLayer("rain_front");
    private final ParticleLayer elementLayer = new ParticleLayer("rain_element");
    private int scale = 1;
    private boolean puddles = true;
    private boolean dripsEnabled = true;
    private boolean thunder = false;
    private float thunderBrightness = 1.0F;
    private int countedWidth = -1;
    private int countedHeight = -1;
    private float countedAmount = -1.0F;
    /** Sideways pixels per row of every streak: the wind angle */
    private float slope = 0.22F;
    private float nextBolt = 3.0F;
    /** The surfaces version and size the drips got fitted between the elements for */
    private int fittedSurfacesVersion = -1;
    private int fittedScale = -1;
    private boolean boltVisible = false;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.rainPuddles.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.WATER)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.puddles.desc")));

        this.rainDrips.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.WATER_DROP)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.drips.desc")));

        this.rainThunder.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.THUNDERSTORM)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.thunder.desc")));

        this.rainThunderBrightness.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.BRIGHTNESS_6)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.thunder_brightness.desc")));

        this.rainColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.rain.color.desc")));

        this.amount.addEntry(menu, this);

        this.size.addEntry(menu, this);

    }

    @Override
    protected void onResize(int width, int height) {
        this.countedWidth = -1;
        // The canvas got cleared with the resize
        this.bolt.reset();
        this.dropCount = 0;
        this.rippleCount = 0;
        this.hitCount = 0;
    }

    @Override
    protected void update(float deltaSeconds) {
        this.readOptions();
        int width = this.getWidth();
        int height = this.getHeight();
        float amountFactor = this.amount.getFactor();
        if ((width != this.countedWidth) || (height != this.countedHeight) || (amountFactor != this.countedAmount)) {
            boolean refill = (width != this.countedWidth) || (height != this.countedHeight);
            this.fitStreakCounts(width, height, amountFactor, refill);
        }
        float time = this.getTime();
        this.slope = 0.3F + 0.1F * (float)Math.sin(time * 0.35F) + 0.05F * (float)Math.sin(time * 0.9F + 2.0F);
        int n = this.scale;
        for (int i = 0; i < this.far.size(); i++) {
            Streak streak = this.far.get(i);
            streak.y += streak.speed * deltaSeconds;
            streak.x += this.slope * streak.speed * deltaSeconds;
            if (streak.y - streak.length * n > height) this.resetFar(streak, false, width, height);
        }
        List<PixelSurfaces.Surface<RainSurface>> all = this.surfaces.all();
        for (int i = 0; i < this.near.size(); i++) this.moveNear(this.near.get(i), deltaSeconds, width, height, all);
        this.updateSplashes(deltaSeconds);
        if (this.dripsEnabled) {
            if ((this.fittedSurfacesVersion != this.surfaces.version()) || (this.fittedScale != n)) {
                this.fittedSurfacesVersion = this.surfaces.version();
                this.fittedScale = n;
                List<PixelSurfaces.Surface<RainSurface>> elements = this.surfaces.elements();
                for (int i = 0; i < all.size(); i++) {
                    RainSurface surface = all.get(i).state();
                    if (surface != null) surface.fitDrips(all.get(i), elements, n);
                }
            }
            this.updateDrips(deltaSeconds, height, all);
        }
        if (this.bolt.isActive()) {
            this.bolt.advance(deltaSeconds);
        } else if (this.thunder && ((this.nextBolt -= deltaSeconds) <= 0.0F)) {
            this.bolt.strike(this.random, Math.max(1, width / n), Math.max(1, height / n), n);
            this.nextBolt = this.random.range(5.0F, 8.5F);
        }
        this.boltVisible = this.bolt.drawInto(this.boltCanvas);
    }

    private void readOptions() {
        if (this.tint.update(this.rainColorHex)) this.art.rebuild(this.tint);
        this.scale = this.size.get();
        this.puddles = this.rainPuddles.tryGetNonNullElse(true);
        if (!this.puddles) this.rippleCount = 0;
        this.dripsEnabled = this.rainDrips.tryGetNonNullElse(true);
        boolean newThunder = this.rainThunder.tryGetNonNullElse(false);
        if (!newThunder && this.thunder) this.bolt.stop();
        if (newThunder && !this.thunder) this.nextBolt = 3.0F;
        this.thunder = newThunder;
        this.thunderBrightness = PixelMath.clamp(this.rainThunderBrightness.getFloat(), MIN_THUNDER_BRIGHTNESS, MAX_THUNDER_BRIGHTNESS);
    }

    /**
     * Grows or shrinks both layers to their wanted number of streaks. A new screen size spreads all streaks over the screen again.
     */
    private void fitStreakCounts(int width, int height, float amountFactor, boolean refill) {
        this.countedWidth = width;
        this.countedHeight = height;
        this.countedAmount = amountFactor;
        float areaFactor = PixelMath.clamp((width * height) / STAGE_AREA, 0.1F, MAX_AREA_FACTOR);
        int farWanted = Math.round(FAR_COUNT * areaFactor * amountFactor);
        int nearWanted = Math.round(NEAR_COUNT * areaFactor * amountFactor);
        if (refill) {
            for (int i = 0; i < this.far.size(); i++) this.resetFar(this.far.get(i), true, width, height);
            for (int i = 0; i < this.near.size(); i++) this.resetNear(this.near.get(i), true, width, height);
        }
        while (this.far.size() < farWanted) {
            Streak streak = new Streak();
            this.resetFar(streak, true, width, height);
            this.far.add(streak);
        }
        while (this.far.size() > farWanted) this.far.remove(this.far.size() - 1);
        while (this.near.size() < nearWanted) {
            Streak streak = new Streak();
            this.resetNear(streak, true, width, height);
            this.near.add(streak);
        }
        while (this.near.size() > nearWanted) this.near.remove(this.near.size() - 1);
    }

    /**
     * How far left of the screen streaks start, so the wind fills the left side too. The mockup stage uses 60 (far) and 80 (near) pixels.
     */
    private static float spawnMargin(float stageMargin, int height) {
        return stageMargin * (height / STAGE_HEIGHT);
    }

    private void resetFar(@NotNull Streak streak, boolean anywhere, int width, int height) {
        streak.x = this.random.range(-spawnMargin(60.0F, height), width + 10.0F);
        streak.y = anywhere ? this.random.range(0.0F, height) : -this.random.range(6.0F, 60.0F);
        streak.length = this.random.rangeInt(RainArt.FAR_MIN_LENGTH, RainArt.FAR_MAX_LENGTH);
        streak.speed = this.random.range(300.0F, 380.0F);
    }

    private void resetNear(@NotNull Streak streak, boolean anywhere, int width, int height) {
        streak.x = this.random.range(-spawnMargin(80.0F, height), width + 10.0F);
        streak.y = anywhere ? this.random.range(0.0F, height) : -this.random.range(6.0F, 80.0F);
        streak.length = this.random.rangeInt(RainArt.NEAR_MIN_LENGTH, RainArt.NEAR_MAX_LENGTH);
        streak.speed = this.random.range(470.0F, 620.0F);
    }

    /**
     * The row a falling thing lands on when it reaches the surface: the art pixel row right above it.
     */
    private int landingY(@NotNull RainSurface surface) {
        return surface.y - this.scale;
    }

    private void moveNear(@NotNull Streak streak, float deltaSeconds, int width, int height, @NotNull List<PixelSurfaces.Surface<RainSurface>> all) {
        float previousY = streak.y;
        streak.y += streak.speed * deltaSeconds;
        streak.x += this.slope * streak.speed * deltaSeconds;
        for (int i = 0; i < all.size(); i++) {
            RainSurface surface = all.get(i).state();
            if ((surface == null) || !surface.spans(streak.x)) continue;
            int landing = this.landingY(surface);
            if ((previousY < landing) && (streak.y >= landing)) {
                this.splash(PixelMath.floor(streak.x), landing, surface, this.random.rangeInt(2, 3));
                this.resetNear(streak, false, width, height);
                return;
            }
        }
        if (streak.y - streak.length * this.scale > height) this.resetNear(streak, false, width, height);
    }

    /**
     * An impact: a short flash of two pixels, droplets thrown up to both sides and, with the wet film on, a ripple along the surface.
     */
    private void splash(int x, int y, @NotNull RainSurface surface, int droplets) {
        int n = this.scale;
        if (this.hitCount < MAX_HITS) {
            Hit hit = this.hits[this.hitCount];
            if (hit == null) this.hits[this.hitCount] = hit = new Hit();
            this.hitCount++;
            hit.x = x;
            hit.y = y - n;
            hit.age = 0.0F;
        }
        for (int k = 0; (k < droplets) && (this.dropCount < MAX_DROPS); k++) {
            Drop drop = this.drops[this.dropCount];
            if (drop == null) this.drops[this.dropCount] = drop = new Drop();
            this.dropCount++;
            drop.x = x;
            drop.y = y - n;
            drop.vx = this.random.range(12.0F, 34.0F) * this.random.sign() * n;
            drop.vy = -this.random.range(30.0F, 70.0F) * n;
            drop.age = 0.0F;
            drop.life = this.random.range(0.2F, 0.34F);
        }
        if (this.puddles && (this.rippleCount < MAX_RIPPLES)) {
            Ripple ripple = this.ripples[this.rippleCount];
            if (ripple == null) this.ripples[this.rippleCount] = ripple = new Ripple();
            this.rippleCount++;
            ripple.x = x;
            ripple.y = y;
            ripple.minX = surface.x;
            ripple.maxX = surface.x + surface.width;
            ripple.age = 0.0F;
            ripple.onFloor = surface.floor;
        }
    }

    private void updateSplashes(float deltaSeconds) {
        float gravity = 320.0F * this.scale;
        int kept = 0;
        for (int i = 0; i < this.dropCount; i++) {
            Drop drop = this.drops[i];
            drop.age += deltaSeconds;
            drop.vy += gravity * deltaSeconds;
            drop.x += drop.vx * deltaSeconds;
            drop.y += drop.vy * deltaSeconds;
            if (drop.age > drop.life) continue;
            // Moves the living ones to the front and keeps the dead objects behind them for reuse
            this.drops[i] = this.drops[kept];
            this.drops[kept++] = drop;
        }
        this.dropCount = kept;
        kept = 0;
        for (int i = 0; i < this.rippleCount; i++) {
            Ripple ripple = this.ripples[i];
            ripple.age += deltaSeconds;
            if (ripple.age > RIPPLE_LIFE) continue;
            this.ripples[i] = this.ripples[kept];
            this.ripples[kept++] = ripple;
        }
        this.rippleCount = kept;
        kept = 0;
        for (int i = 0; i < this.hitCount; i++) {
            Hit hit = this.hits[i];
            hit.age += deltaSeconds;
            if (hit.age > HIT_LIFE) continue;
            this.hits[i] = this.hits[kept];
            this.hits[kept++] = hit;
        }
        this.hitCount = kept;
    }

    private void updateDrips(float deltaSeconds, int height, @NotNull List<PixelSurfaces.Surface<RainSurface>> all) {
        int n = this.scale;
        for (int i = 0; i < all.size(); i++) {
            RainSurface owner = all.get(i).state();
            if (owner == null) continue;
            for (RainSurface.Drip drip : owner.drips) {
                if (!drip.falling) {
                    if (drip.maxStage(n) <= 0) continue;
                    drip.progress += deltaSeconds / drip.period;
                    if (drip.progress >= 1.0F) {
                        drip.falling = true;
                        drip.y = owner.bottom + drip.fallStartOffset(n);
                        drip.vy = 20.0F * n;
                    }
                    continue;
                }
                float previousY = drip.y;
                drip.vy += 520.0F * n * deltaSeconds;
                drip.y += drip.vy * deltaSeconds;
                int x = owner.dripX(drip, n);
                RainSurface landedOn = null;
                for (int j = 0; j < all.size(); j++) {
                    RainSurface surface = all.get(j).state();
                    if ((surface == null) || (surface == owner) || !surface.spans(x)) continue;
                    int landing = this.landingY(surface);
                    if ((previousY < landing) && (drip.y >= landing)) {
                        landedOn = surface;
                        break;
                    }
                }
                if (landedOn != null) {
                    this.splash(x, this.landingY(landedOn), landedOn, 2);
                } else if (drip.y <= height + 10 * n) {
                    continue;
                }
                drip.falling = false;
                drip.progress = -this.random.range(0.0F, 1.2F);
                drip.period = this.random.range(0.9F, 2.4F);
            }
        }
    }

    @Override
    protected void drawBack(@NotNull PixelGraphics graphics) {
        int n = this.scale;
        int slopeIndex = RainArt.slopeIndex(this.slope);
        ParticleLayer layer = this.backLayer;
        layer.begin(graphics, this.far.size());
        for (int i = 0; i < this.far.size(); i++) {
            Streak streak = this.far.get(i);
            int offset = RainArt.tailOffset(streak.length, slopeIndex);
            layer.sprite(RainArt.farStreak(streak.length, slopeIndex), this.art.farPalette, PixelMath.floor(streak.x) - offset * n, PixelMath.floor(streak.y) - (streak.length - 1) * n, n);
        }
        layer.end();
        if (this.boltVisible) graphics.canvas(this.boltCanvas, 0, 0);
    }

    @Override
    protected void drawElement(@NotNull PixelGraphics graphics) {
        List<PixelSurfaces.Surface<RainSurface>> all = this.surfaces.all();
        ParticleLayer layer = this.elementLayer;
        layer.begin(graphics, (this.puddles ? (5 * all.size() + 2 * this.rippleCount) : 0) + (this.dripsEnabled ? 3 * all.size() : 0));
        if (this.puddles) {
            this.drawFilms(layer, all, false);
            this.drawRipples(layer, false);
        }
        if (this.dripsEnabled) {
            int n = this.scale;
            for (int i = 0; i < all.size(); i++) {
                RainSurface owner = all.get(i).state();
                if (owner == null) continue;
                for (RainSurface.Drip drip : owner.drips) {
                    int stage = drip.falling ? 0 : drip.stage(n);
                    if (stage > 0) layer.sprite(RainArt.HANGING_DRIP[stage - 1], this.art.hangingDripPalette, owner.dripX(drip, n), owner.bottom, n);
                }
            }
        }
        layer.end();
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        int n = this.scale;
        int stage = this.bolt.stage();
        if ((stage == RainBolt.STAGE_STRIKE) || (stage == RainBolt.STAGE_RESTRIKE)) {
            int alpha = Math.round(((stage == RainBolt.STAGE_STRIKE) ? FLASH_STRIKE_ALPHA : FLASH_RESTRIKE_ALPHA) * this.thunderBrightness);
            graphics.rect(0, 0, this.getWidth(), this.getHeight(), PixelMath.withAlpha(FLASH_COLOR, alpha));
        }
        List<PixelSurfaces.Surface<RainSurface>> all = this.surfaces.all();
        ParticleLayer layer = this.frontLayer;
        layer.begin(graphics, this.near.size() + this.dropCount + 2 * (this.rippleCount + this.hitCount) + (this.puddles ? 16 : 0));
        if (this.puddles) this.drawFilms(layer, all, true);
        int slopeIndex = RainArt.slopeIndex(this.slope);
        for (int i = 0; i < this.near.size(); i++) {
            Streak streak = this.near.get(i);
            int offset = RainArt.tailOffset(streak.length, slopeIndex);
            layer.sprite(RainArt.nearStreak(streak.length, slopeIndex), this.art.nearPalette, PixelMath.floor(streak.x) - offset * n, PixelMath.floor(streak.y) - (streak.length - 1) * n, n);
        }
        if (this.dripsEnabled) {
            for (int i = 0; i < all.size(); i++) {
                RainSurface owner = all.get(i).state();
                if (owner == null) continue;
                for (RainSurface.Drip drip : owner.drips) {
                    if (drip.falling) layer.sprite(RainArt.FALLING_DRIP, this.art.fallingDripPalette, owner.dripX(drip, n), PixelMath.floor(drip.y) - n, n);
                }
            }
        }
        if (this.puddles) this.drawRipples(layer, true);
        for (int i = 0; i < this.dropCount; i++) {
            Drop drop = this.drops[i];
            layer.rect(PixelMath.floor(drop.x), PixelMath.floor(drop.y), n, n, this.art.drop);
        }
        for (int i = 0; i < this.hitCount; i++) {
            Hit hit = this.hits[i];
            layer.rect(hit.x - n, hit.y, n, n, this.art.hit);
            layer.rect(hit.x + n, hit.y, n, n, this.art.hit);
        }
        layer.end();
    }

    /**
     * Draws the thin wet film with its glints on the floor or on all elements.
     */
    private void drawFilms(@NotNull ParticleLayer layer, @NotNull List<PixelSurfaces.Surface<RainSurface>> all, boolean floor) {
        int n = this.scale;
        float time = this.getTime();
        for (int i = 0; i < all.size(); i++) {
            RainSurface surface = all.get(i).state();
            if ((surface == null) || (surface.floor != floor)) continue;
            int y = this.landingY(surface);
            layer.rect(surface.x, y, surface.width, n, this.art.film);
            for (int g = 0; g < surface.glintX.length; g++) {
                if (Math.sin(time * surface.glintSpeed[g] + surface.glintPhase[g]) > 0.55) layer.rect(surface.glintX[g], y, n, n, this.art.filmGlint);
            }
        }
    }

    /**
     * Draws the ripples running along the film of the floor or of the elements.
     */
    private void drawRipples(@NotNull ParticleLayer layer, boolean floor) {
        int n = this.scale;
        for (int i = 0; i < this.rippleCount; i++) {
            Ripple ripple = this.ripples[i];
            if (ripple.onFloor != floor) continue;
            int spread = (1 + PixelMath.floor(ripple.age * 18.0F)) * n;
            int color = this.art.ripple[(ripple.age < 0.12F) ? 0 : ((ripple.age < 0.24F) ? 1 : 2)];
            int left = ripple.x - spread;
            int right = ripple.x + spread;
            if ((left >= ripple.minX) && (left < ripple.maxX)) layer.rect(left, ripple.y, n, n, color);
            if ((right >= ripple.minX) && (right < ripple.maxX)) layer.rect(right, ripple.y, n, n, color);
        }
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        this.backLayer.release();
        this.frontLayer.release();
        this.elementLayer.release();
    }

    private static final class Streak {

        float x;
        float y;
        int length;
        float speed;

    }

    private static final class Drop {

        float x;
        float y;
        float vx;
        float vy;
        float age;
        float life;

    }

    private static final class Ripple {

        int x;
        int y;
        int minX;
        int maxX;
        float age;
        boolean onFloor;

    }

    private static final class Hit {

        int x;
        int y;
        float age;

    }

}
