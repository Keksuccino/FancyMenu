package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.embers;

import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.canvas.CanvasPainter;
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
import java.util.List;

/**
 * Embers rise from the bottom of the screen and cool from white through yellow and orange to dark red, the hottest ones flare as
 * small crosses, and every few seconds a crackle throws a quick burst upward. Ash flakes drift down and settle in layered drifts on
 * the elements and in a deeper bed along the bottom, where spots smolder, give off thin wisps of smoke and coals pulse slowly.<br><br>
 *
 * Parts: a dimmer layer of far sparks rises behind the elements ({@code BACK}). The ash drifts on elements with their smoldering spots,
 * the smoke rising from them and the puffs of flakes landing on them belong to the elements ({@code ELEMENT}). Everything else, the bed
 * on the floor included, is drawn across the screen in front ({@code FRONT}).
 * The drifts live in canvases that only change in the columns where ash landed or sank.
 */
public class EmbersDecorationOverlay extends PixelDecorationOverlay<EmbersDecorationOverlay> {

    private static final String KEY = "fancymenu.decoration_overlays.embers.";
    /** Colors an ember cools through, from white-hot to nearly out */
    static final int[] EMBER = {0xFFFFF6C8, 0xFFFFC857, 0xFFF07F2A, 0xFFC0402A, 0xFF6A2420};
    private static final int SMOKE = 0xA8A0A0;
    private static final int FAR_SPARKS = 60;
    private static final int NEAR_SPARKS = 95;
    private static final int MAX_BURST_SPARKS = 64;
    private static final int MAX_FLAKES = 170;
    private static final int START_FLAKES = 90;
    private static final int MAX_GLOWS = 48;
    private static final int MAX_SMOKE = 96;
    private static final int MAX_PUFFS = 48;
    private static final int COALS = 16;
    /** Seconds between two flakes of the light ashfall from above */
    private static final float ASHFALL_INTERVAL = 0.26F;

    /** A hot flare: a white core with arms in the next color of the ramp */
    private static final PixelSprite FLARE = PixelSprite.of(
            ".a.",
            "aca",
            ".a.");
    private static final PixelPalette[] FLARE_PALETTES = {
            PixelPalette.builder().rgb('c', 0xFFFFFF).argb('a', EMBER[1]).build(),
            PixelPalette.builder().rgb('c', 0xFFFFFF).argb('a', EMBER[2]).build()};
    /** A smoldering spot in the top of a drift, which tints the ash around it while it is hot */
    private static final PixelSprite GLOW_DEEP = PixelSprite.of(
            "scs",
            ".b.");
    private static final PixelSprite GLOW_FLAT = PixelSprite.of("scs");
    private static final PixelSprite GLOW_DOT = PixelSprite.of("c");
    private static final PixelPalette[] GLOW_PALETTES = new PixelPalette[EMBER.length];
    private static final PixelSprite COAL = PixelSprite.of("ab");
    private static final PixelPalette[] COAL_PALETTES = {
            PixelPalette.builder().argb('a', EMBER[1]).argb('b', EMBER[2]).build(),
            PixelPalette.builder().argb('a', EMBER[2]).argb('b', EMBER[3]).build(),
            PixelPalette.builder().argb('a', EMBER[3]).argb('b', EMBER[3]).build()};
    /** A tumbling ash sliver, lying flat or standing on edge */
    private static final PixelSprite SLIVER_FLAT = PixelSprite.of("12");
    private static final PixelSprite SLIVER_UPRIGHT = PixelSprite.of("2", "1");
    private static final PixelPalette[] SLIVER_PALETTES = new PixelPalette[3];

    static {
        for (int k = 0; k < EMBER.length; k++) {
            int sideAlpha = (k != 0) ? 70 : 120;
            GLOW_PALETTES[k] = PixelPalette.builder().argb('c', EMBER[k]).rgb('s', EMBER[2], sideAlpha).rgb('b', EMBER[3], 110).build();
        }
        for (int tone = 0; tone < 3; tone++) {
            SLIVER_PALETTES[tone] = PixelPalette.builder().argb('1', AshDrift.ASH[tone]).argb('2', AshDrift.ASH[tone + 1]).build();
        }
    }

    public final Property<Boolean> ashOnFloor = putProperty(Property.booleanProperty("ash_on_floor", true, KEY + "ash_on_floor"));
    public final Property<Boolean> ashOnElements = putProperty(Property.booleanProperty("ash_on_elements", true, KEY + "ash_on_elements"));

    private final PixelSurfaces<AshDrift> surfaces = this.createSurfaces(surface -> new AshDrift(this.random, surface.isFloor(), surface.x(), surface.y(), surface.width()));
    private final TiledCanvas elementCanvas = new TiledCanvas("embers_elements");
    private final TiledCanvas bedCanvas = new TiledCanvas("embers_bed");
    private final CanvasPainter painter = new CanvasPainter();
    private final List<AshDrift> drifts = new ArrayList<>();
    private final TiledCanvas.Content elementContent = this::paintElementDrifts;
    private final TiledCanvas.Content bedContent = this::paintBed;
    private int surfacesVersion = -1;
    private boolean resized = false;
    private boolean started = false;
    private boolean floorOn = true;
    private boolean elementsOn = true;
    private float nextCrackle = 2.0F;
    private float ashfall = 0.0F;
    private float nextSmolder = 0.4F;

    private final Spark[] sparks = new Spark[FAR_SPARKS + NEAR_SPARKS];
    private final Spark[] burst = new Spark[MAX_BURST_SPARKS];
    private int burstCount = 0;
    private final Flake[] flakes = new Flake[MAX_FLAKES];
    private int flakeCount = 0;
    private final Glow[] glows = new Glow[MAX_GLOWS];
    private int glowCount = 0;
    private final Mote[] smoke = new Mote[MAX_SMOKE];
    private int smokeCount = 0;
    private final Mote[] puffs = new Mote[MAX_PUFFS];
    private int puffCount = 0;
    private final int[] coalColumn = new int[COALS];
    private final float[] coalPhase = new float[COALS];
    private final float[] coalSpeed = new float[COALS];

    public EmbersDecorationOverlay() {
        for (int i = 0; i < this.sparks.length; i++) this.sparks[i] = new Spark();
        for (int i = 0; i < this.burst.length; i++) this.burst[i] = new Spark();
        for (int i = 0; i < this.flakes.length; i++) this.flakes[i] = new Flake();
        for (int i = 0; i < this.glows.length; i++) this.glows[i] = new Glow();
        for (int i = 0; i < this.smoke.length; i++) this.smoke[i] = new Mote();
        for (int i = 0; i < this.puffs.length; i++) this.puffs[i] = new Mote();
    }

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {
        this.addToggle(menu, this.ashOnFloor, MaterialIcons.FLOOR, "ash_on_floor");
        this.addToggle(menu, this.ashOnElements, MaterialIcons.WIDGETS, "ash_on_elements");
    }

    private void addToggle(@NotNull ContextMenu menu, @NotNull Property<Boolean> property, @NotNull MaterialIcon icon, @NotNull String name) {
        property.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(icon)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable(KEY + name + ".desc")));
    }

    @Override
    protected void onResize(int width, int height) {
        this.elementCanvas.resizeToScreen(width, height);
        this.bedCanvas.resize(0, height - AshDrift.MAX_ROWS, width, AshDrift.MAX_ROWS, width, AshDrift.MAX_ROWS);
        for (int c = 0; c < COALS; c++) {
            this.coalColumn[c] = this.random.rangeInt(2, width - 4);
            this.coalPhase[c] = this.random.range(0.0F, PixelMath.TAU);
            this.coalSpeed[c] = this.random.range(0.5F, 1.4F);
        }
        if (!this.started) {
            this.started = true;
            for (int i = 0; i < this.sparks.length; i++) this.sparks[i].reset(this.random, i < FAR_SPARKS, true, width, height);
            for (int i = 0; i < START_FLAKES; i++) this.addFlake(this.random.range(0.0F, width), this.random.range(0.0F, height));
        }
        this.surfacesVersion = -1;
        this.resized = true;
    }

    @Override
    protected void update(float deltaSeconds) {
        this.floorOn = this.ashOnFloor.tryGetNonNullElse(true);
        this.elementsOn = this.ashOnElements.tryGetNonNullElse(true);
        this.syncDrifts();
        int width = this.getWidth();
        int height = this.getHeight();
        AshDrift bed = this.surfaces.floor().state();

        // A crackle throws sparks up from the bed and leaves a few glowing spots where it burst
        if ((this.nextCrackle -= deltaSeconds) <= 0.0F) {
            this.nextCrackle = this.random.range(1.5F, 4.0F);
            float x = this.random.range(20.0F, width - 20.0F);
            for (int k = this.random.rangeInt(5, 9); k > 0; k--) {
                if (this.burstCount == MAX_BURST_SPARKS) break;
                Spark spark = this.burst[this.burstCount++];
                spark.reset(this.random, false, false, width, height);
                spark.x = x + this.random.range(-4.0F, 4.0F);
                spark.y = height + this.random.range(0.0F, 4.0F);
                spark.rise = this.random.range(50.0F, 95.0F);
                spark.life = this.random.range(1.2F, 2.4F);
                spark.kick = this.random.range(-22.0F, 22.0F);
            }
            for (int k = 0; k < 3; k++) {
                int column = (int)Math.round(x + this.random.range(-6.0F, 6.0F));
                if (bed != null) this.light(bed, column, this.random.range(1.5F, 3.0F));
            }
        }
        float riseDecay = (float)Math.exp(-0.12 * deltaSeconds);
        float kickDecay = (float)Math.exp(-1.5 * deltaSeconds);
        for (Spark spark : this.sparks) {
            if (spark.advance(deltaSeconds, riseDecay, kickDecay)) {
                this.burnOut(spark);
                spark.reset(this.random, spark.far, false, width, height);
            }
        }
        for (int i = this.burstCount - 1; i >= 0; i--) {
            Spark spark = this.burst[i];
            if (!spark.advance(deltaSeconds, riseDecay, kickDecay)) continue;
            this.burnOut(spark);
            this.burst[i] = this.burst[--this.burstCount];
            this.burst[this.burstCount] = spark;
        }

        // A light ashfall from above keeps the drifts fed
        for (this.ashfall -= deltaSeconds; this.ashfall <= 0.0F; this.ashfall += ASHFALL_INTERVAL) {
            this.addFlake(this.random.range(0.0F, width), -3.0F);
        }
        this.updateFlakes(deltaSeconds, height);
        for (AshDrift drift : this.drifts) drift.sink(deltaSeconds);

        if ((this.nextSmolder -= deltaSeconds) <= 0.0F) {
            this.nextSmolder = this.random.range(0.35F, 0.9F);
            List<PixelSurfaces.Surface<AshDrift>> all = this.surfaces.all();
            AshDrift drift = all.get(this.random.index(all.size())).state();
            if (drift != null) this.light(drift, this.random.rangeInt(1, drift.columns - 2), this.random.range(2.2F, 4.0F));
        }
        this.updateGlows(deltaSeconds);
        this.updateMotes(deltaSeconds);

        for (AshDrift drift : this.drifts) drift.sync(this.isOn(drift), drift.floor ? this.bedCanvas : this.elementCanvas);
        if (this.resized) {
            this.elementCanvas.invalidateAll();
            this.bedCanvas.invalidateAll();
        }
        this.elementCanvas.repaint(this.painter, this.elementContent);
        this.bedCanvas.repaint(this.painter, this.bedContent);
        this.resized = false;
    }

    private boolean isOn(@NotNull AshDrift drift) {
        return drift.floor ? this.floorOn : this.elementsOn;
    }

    /**
     * Keeps the drifts in step with the surfaces: drifts of elements that are gone get erased, together with their glowing spots.
     */
    private void syncDrifts() {
        if (this.surfaces.version() == this.surfacesVersion) return;
        this.surfacesVersion = this.surfaces.version();
        List<AshDrift> current = new ArrayList<>();
        for (PixelSurfaces.Surface<AshDrift> surface : this.surfaces.all()) {
            if (surface.state() != null) current.add(surface.state());
        }
        for (AshDrift old : this.drifts) {
            if (current.contains(old)) continue;
            if (!old.floor) old.invalidateAll(this.elementCanvas);
        }
        for (int i = this.glowCount - 1; i >= 0; i--) {
            if (!current.contains(this.glows[i].drift)) this.removeGlow(i);
        }
        this.drifts.clear();
        this.drifts.addAll(current);
    }

    private void light(@NotNull AshDrift drift, int column, float life) {
        if (!this.isOn(drift) || (column <= 0) || (column >= drift.columns - 1) || (drift.heights[column] < 1.0F) || (this.glowCount == MAX_GLOWS)) return;
        Glow glow = this.glows[this.glowCount++];
        glow.drift = drift;
        glow.column = column;
        glow.age = 0.0F;
        glow.life = life;
    }

    /**
     * Burnt-out embers turn into ash flakes.
     */
    private void burnOut(@NotNull Spark spark) {
        if (!spark.far && (spark.y > 10.0F) && (this.flakeCount < MAX_FLAKES) && (this.random.next() < 0.5F)) {
            this.addFlake(spark.drawX(), spark.y);
        }
    }

    private void addFlake(float x, float y) {
        if (this.flakeCount == MAX_FLAKES) return;
        Flake flake = this.flakes[this.flakeCount++];
        flake.x = x;
        flake.y = y;
        flake.fall = this.random.range(6.0F, 13.0F);
        flake.phase = this.random.range(0.0F, PixelMath.TAU);
        flake.wobble = this.random.range(1.2F, 2.6F);
        flake.big = this.random.next() < 0.35F;
        flake.tone = this.random.rangeInt(0, 2);
        flake.tumble = this.random.range(0.0F, 9.0F);
    }

    private void updateFlakes(float deltaSeconds, int height) {
        for (int i = this.flakeCount - 1; i >= 0; i--) {
            Flake flake = this.flakes[i];
            float lastY = flake.y;
            flake.phase += flake.wobble * deltaSeconds;
            flake.y += flake.fall * deltaSeconds;
            flake.x += (float)Math.sin(flake.phase) * 7.0F * deltaSeconds;
            boolean gone = flake.y > height + 2;
            if (!gone) gone = this.land(flake, lastY);
            if (!gone) continue;
            this.flakes[i] = this.flakes[--this.flakeCount];
            this.flakes[this.flakeCount] = flake;
        }
    }

    /**
     * Lets a flake settle on the first drift it reached, elements before the floor. A landing flake kicks up a small puff.
     *
     * @return whether it landed
     */
    private boolean land(@NotNull Flake flake, float lastY) {
        for (AshDrift drift : this.drifts) {
            if (!this.isOn(drift)) continue;
            int column = PixelMath.floor(flake.x - drift.x0);
            if ((column < 0) || (column >= drift.columns)) continue;
            int surface = drift.surfaceRow(column);
            if ((lastY > surface) || (flake.y <= surface)) continue;
            drift.deposit(column, flake.big ? 0.5F : 0.3F);
            if (flake.big) {
                for (int k = 0; k < 2; k++) {
                    if (this.puffCount == MAX_PUFFS) break;
                    Mote puff = this.puffs[this.puffCount++];
                    puff.x = flake.x;
                    puff.y = surface - 1;
                    puff.velocityX = ((k != 0) ? 1 : -1) * this.random.range(4.0F, 10.0F);
                    puff.velocityY = -this.random.range(5.0F, 11.0F);
                    puff.age = 0.0F;
                    puff.onElement = !drift.floor;
                }
            }
            return true;
        }
        return false;
    }

    private void updateGlows(float deltaSeconds) {
        for (int i = this.glowCount - 1; i >= 0; i--) {
            Glow glow = this.glows[i];
            AshDrift drift = glow.drift;
            glow.age += deltaSeconds;
            if ((drift == null) || (glow.age >= glow.life) || !this.isOn(drift)) {
                this.removeGlow(i);
                continue;
            }
            // A hot spot gives off a thin wisp of smoke now and then
            if ((glow.age < glow.life * 0.6F) && (this.random.next() < deltaSeconds * 1.5F) && (this.smokeCount < MAX_SMOKE)) {
                Mote wisp = this.smoke[this.smokeCount++];
                wisp.x = drift.x0 + glow.column;
                wisp.y = drift.surfaceRow(glow.column) - 1;
                wisp.age = 0.0F;
                wisp.phase = this.random.range(0.0F, PixelMath.TAU);
                wisp.onElement = !drift.floor;
            }
        }
    }

    private void removeGlow(int i) {
        Glow glow = this.glows[i];
        this.glows[i] = this.glows[--this.glowCount];
        this.glows[this.glowCount] = glow;
        glow.drift = null;
    }

    private void updateMotes(float deltaSeconds) {
        for (int i = this.smokeCount - 1; i >= 0; i--) {
            Mote wisp = this.smoke[i];
            wisp.age += deltaSeconds;
            wisp.y -= 9.0F * deltaSeconds;
            if (wisp.age <= 1.5F) continue;
            this.smoke[i] = this.smoke[--this.smokeCount];
            this.smoke[this.smokeCount] = wisp;
        }
        for (int i = this.puffCount - 1; i >= 0; i--) {
            Mote puff = this.puffs[i];
            puff.age += deltaSeconds;
            puff.velocityY += 50.0F * deltaSeconds;
            puff.x += puff.velocityX * deltaSeconds;
            puff.y += puff.velocityY * deltaSeconds;
            if (puff.age <= 0.3F) continue;
            this.puffs[i] = this.puffs[--this.puffCount];
            this.puffs[this.puffCount] = puff;
        }
    }

    private void paintElementDrifts(@NotNull CanvasPainter painter) {
        for (AshDrift drift : this.drifts) {
            if (!drift.floor) drift.paint(painter);
        }
    }

    private void paintBed(@NotNull CanvasPainter painter) {
        AshDrift bed = this.surfaces.floor().state();
        if (bed != null) bed.paint(painter);
    }

    @Override
    protected void drawBack(@NotNull PixelGraphics graphics) {
        // The dimmer far sparks rise behind the elements
        for (int i = 0; i < FAR_SPARKS; i++) {
            Spark spark = this.sparks[i];
            int k = spark.heat();
            int y = PixelMath.floor(spark.y);
            // New sparks start below the screen, they cost no quad until they rise into it
            if ((k < 4) && (y < this.getHeight())) graphics.pixel(PixelMath.floor(spark.drawX()), y, PixelMath.withAlpha(EMBER[k + 1], 200));
        }
    }

    @Override
    protected void drawElement(@NotNull PixelGraphics graphics) {
        this.elementCanvas.draw(graphics);
        this.drawGlows(graphics, false);
        this.drawSmoke(graphics, true);
        this.drawPuffs(graphics, true);
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        float time = this.getTime();
        this.bedCanvas.draw(graphics);
        AshDrift bed = this.surfaces.floor().state();
        if (this.floorOn && (bed != null)) {
            // Coals in the bed pulse slowly
            for (int c = 0; c < COALS; c++) {
                int column = this.coalColumn[c];
                if ((column < 0) || (column >= bed.columns)) continue;
                int h = bed.height(column);
                if (h < 2) continue;
                float v = 0.5F + 0.5F * (float)Math.sin(time * this.coalSpeed[c] + this.coalPhase[c]);
                int palette = (v > 0.8F) ? 0 : ((v > 0.45F) ? 1 : 2);
                graphics.sprite(COAL, COAL_PALETTES[palette], column, this.getHeight() - h + 1);
            }
        }
        this.drawGlows(graphics, true);
        this.drawSmoke(graphics, false);
        for (int i = 0; i < this.flakeCount; i++) {
            Flake flake = this.flakes[i];
            int x = PixelMath.floor(flake.x);
            int y = PixelMath.floor(flake.y);
            if (!flake.big) {
                graphics.pixel(x, y, PixelMath.withAlpha(AshDrift.ASH[flake.tone], 235));
            } else if ((PixelMath.floor(time * 2.5F + flake.tumble) & 1) != 0) {
                graphics.sprite(SLIVER_UPRIGHT, SLIVER_PALETTES[flake.tone], x, y - 1);
            } else {
                graphics.sprite(SLIVER_FLAT, SLIVER_PALETTES[flake.tone], x, y);
            }
        }
        this.drawPuffs(graphics, false);
        for (int i = FAR_SPARKS; i < this.sparks.length; i++) this.drawEmber(graphics, this.sparks[i], time);
        for (int i = 0; i < this.burstCount; i++) this.drawEmber(graphics, this.burst[i], time);
    }

    /**
     * Draws a near ember, which flickers: it skips one frame in nine.
     */
    private void drawEmber(@NotNull PixelGraphics graphics, @NotNull Spark spark, float time) {
        if (PixelMath.floor(time * 20.0F + spark.phase * 7.0F) % 9 == 0) return;
        int y = PixelMath.floor(spark.y);
        if (y > this.getHeight()) return;
        int k = spark.heat();
        int x = PixelMath.floor(spark.drawX());
        if (spark.big && (k < 2)) {
            graphics.sprite(FLARE, FLARE_PALETTES[k], x - 1, y - 1);
        } else {
            graphics.pixel(x, y, (k == 4) ? PixelMath.withAlpha(EMBER[4], 170) : EMBER[k]);
        }
    }

    /**
     * Draws the wisps of smoke rising from the drifts on elements or from the bed on the floor.
     */
    private void drawSmoke(@NotNull PixelGraphics graphics, boolean onElements) {
        for (int i = 0; i < this.smokeCount; i++) {
            Mote wisp = this.smoke[i];
            if (wisp.onElement != onElements) continue;
            int alpha = (wisp.age < 0.5F) ? 130 : ((wisp.age < 1.0F) ? 90 : 50);
            graphics.pixel(PixelMath.floor(wisp.x + (float)Math.sin(wisp.age * 4.0F + wisp.phase) * 1.5F), PixelMath.floor(wisp.y), PixelMath.withAlpha(SMOKE, alpha));
        }
    }

    /**
     * Draws the puffs of ash kicked up by flakes landing on the drifts on elements or on the bed on the floor.
     */
    private void drawPuffs(@NotNull PixelGraphics graphics, boolean onElements) {
        for (int i = 0; i < this.puffCount; i++) {
            Mote puff = this.puffs[i];
            if (puff.onElement != onElements) continue;
            graphics.pixel(PixelMath.floor(puff.x), PixelMath.floor(puff.y), PixelMath.withAlpha(AshDrift.ASH[1], (puff.age < 0.18F) ? 230 : 120));
        }
    }

    private void drawGlows(@NotNull PixelGraphics graphics, boolean floor) {
        for (int i = 0; i < this.glowCount; i++) {
            Glow glow = this.glows[i];
            AshDrift drift = glow.drift;
            if ((drift == null) || (drift.floor != floor) || !this.isOn(drift)) continue;
            int h = drift.height(glow.column);
            if (h < 1) continue;
            int k = Math.min(4, PixelMath.floor(glow.age / glow.life * 5.0F));
            int x = drift.x0 + glow.column;
            int y = drift.top - h;
            if (k >= 3) {
                graphics.sprite(GLOW_DOT, GLOW_PALETTES[k], x, y);
            } else {
                graphics.sprite((h > 1) ? GLOW_DEEP : GLOW_FLAT, GLOW_PALETTES[k], x - 1, y);
            }
        }
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        this.elementCanvas.releaseTextures();
        this.bedCanvas.releaseTextures();
    }

    /**
     * An ember rising on a wavering path.
     */
    private static final class Spark {

        boolean far;
        float x;
        float y;
        float rise;
        float phase;
        float wave;
        float amplitude;
        float age;
        float life;
        boolean big;
        /** Sideways push of a crackle, which fades quickly */
        float kick;

        /**
         * Starts the spark again, at the bottom or anywhere on the screen.
         */
        void reset(@NotNull PixelRandom random, boolean far, boolean anywhere, int width, int height) {
            this.far = far;
            this.x = random.range(0.0F, width);
            this.y = anywhere ? random.range(0.0F, height) : (height + random.range(2.0F, 30.0F));
            this.rise = far ? random.range(10.0F, 22.0F) : random.range(18.0F, 44.0F);
            this.phase = random.range(0.0F, PixelMath.TAU);
            this.wave = random.range(1.2F, 3.0F);
            this.amplitude = random.range(3.0F, 10.0F);
            this.age = anywhere ? random.range(0.0F, 4.0F) : 0.0F;
            this.life = random.range(4.5F, 9.0F);
            this.big = !far && (random.next() < 0.3F);
            this.kick = 0.0F;
        }

        /**
         * @return whether it burnt out or left the screen
         */
        boolean advance(float deltaSeconds, float riseDecay, float kickDecay) {
            this.age += deltaSeconds;
            this.phase += this.wave * deltaSeconds;
            this.y -= this.rise * deltaSeconds;
            this.rise *= riseDecay;
            this.x += this.kick * deltaSeconds;
            this.kick *= kickDecay;
            return (this.age >= this.life) || (this.y < -6.0F);
        }

        float drawX() {
            return this.x + (float)Math.sin(this.phase) * this.amplitude;
        }

        /**
         * How far it cooled, from 0 (white-hot) to 4 (nearly out).
         */
        int heat() {
            return Math.min(4, PixelMath.floor(this.age / this.life * 5.0F));
        }

    }

    /**
     * An ash flake: a speck, or a sliver that tumbles.
     */
    private static final class Flake {

        float x;
        float y;
        float fall;
        float phase;
        float wobble;
        boolean big;
        int tone;
        float tumble;

    }

    /**
     * A smoldering spot in a drift.
     */
    private static final class Glow {

        @Nullable
        AshDrift drift;
        int column;
        float age;
        float life;

    }

    /**
     * A wisp of smoke or a puff of ash.
     */
    private static final class Mote {

        float x;
        float y;
        float velocityX;
        float velocityY;
        float age;
        float phase;
        /** Whether it came from ash on an element, which makes it part of the element */
        boolean onElement;

    }

}
