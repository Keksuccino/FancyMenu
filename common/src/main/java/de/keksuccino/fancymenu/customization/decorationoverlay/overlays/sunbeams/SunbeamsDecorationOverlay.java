package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams;

import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * A fan of light rays from a sun just outside of the screen, in front of the elements. The rays swell, fade and come back at new angles,
 * dust drifts in them and the pointer stirs it. The side and the angle place the sun.<br>
 * With Hit Elements on, the rays stop at the elements: the edges that face the sun light up in gold, now and then with a glint
 * (element content), and every element casts a pointed shade into the haze behind it. No light falls on the faces of the elements,
 * so labels stay clean. With it off, the rays pass through in front of the elements.<br><br>
 *
 * The light only depends on the direction and the distance from the sun, so the {@link SunbeamsRig} stores both for every pixel once,
 * and a frame only refreshes the small direction tables of {@link SunbeamsRays}. The light canvas gets repainted from them in a band of rows
 * that moves down the screen, so every row gets repainted about 20 times a second while a frame only paints a part of the screen.
 * Rigs get built off the render thread (when the screen size, the sun or the elements change), and the old rig stays in use until the new one is ready.
 */
public class SunbeamsDecorationOverlay extends PixelDecorationOverlay<SunbeamsDecorationOverlay> {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final float STAGE_DIAGONAL = (float)Math.hypot(480.0, 270.0);
    /** How far outside of the screen the sun sits, as part of the screen diagonal. In the mockup it is just outside the top left corner. */
    private static final float SUN_DISTANCE = 0.343F;
    /** How far the sun glow reaches on the mockup stage. */
    private static final float STAGE_GLOW_RADIUS = 330.0F;
    private static final float GLOW_STRENGTH = 0.75F;
    /**
     * Times per second every row of the light gets repainted on the mockup stage or smaller screens. Bigger screens (small GUI scales)
     * repaint less often by the square root of their extra area, down to {@link #MIN_REPAINT_RATE}, so the work per frame grows slower than the screen.
     */
    private static final float REPAINT_RATE = 20.0F;
    private static final float MIN_REPAINT_RATE = 10.0F;
    /** The shade gets rebuilt at most this often while the elements keep moving. */
    private static final float SHADE_REBUILD_INTERVAL = 0.25F;
    private static final float GLINT_LIFE = 0.55F;
    private static final int MAX_GLINTS = 4;
    public static final int MIN_ANGLE = -80;
    public static final int MAX_ANGLE = 80;

    public final Property<SunbeamsSide> sunSide = putProperty(Property.enumProperty("sun_side", SunbeamsSide.TOP, "fancymenu.decoration_overlays.sunbeams.sun_side", SunbeamsSide::getLocalizationKey));
    public final Property.IntegerProperty sunAngle = putProperty(Property.integerProperty("sun_angle", -50, "fancymenu.decoration_overlays.sunbeams.sun_angle", Property.NumericInputBehavior.<Integer>builder().rangeInput(MIN_ANGLE, MAX_ANGLE).step(5).build()));
    public final Property.ColorProperty sunColor = putProperty(Property.hexColorProperty("sun_color", "#FFE9A6", true, "fancymenu.decoration_overlays.sunbeams.sun_color"));
    public final Property.IntegerProperty sunIntensity = putProperty(Property.integerProperty("sun_intensity", 100, "fancymenu.decoration_overlays.sunbeams.sun_intensity", Property.NumericInputBehavior.<Integer>builder().rangeInput(10, 200).step(10).build()));
    public final Property<Boolean> sunGlow = putProperty(Property.booleanProperty("sun_glow", true, "fancymenu.decoration_overlays.sunbeams.sun_glow"));
    public final Property<Boolean> dust = putProperty(Property.booleanProperty("dust", true, "fancymenu.decoration_overlays.sunbeams.dust"));
    public final Property<Boolean> hitElements = putProperty(Property.booleanProperty("hit_elements", true, "fancymenu.decoration_overlays.sunbeams.hit_elements"));

    private final PixelCanvas lightCanvas = this.createScreenCanvas("sunbeams");
    /** The lit element edges, only as big as the area they cover */
    private final PixelCanvas rimCanvas = new PixelCanvas("sunbeams_rim");
    private final SunbeamsRays rays = new SunbeamsRays();
    private final SunbeamsDust motes = new SunbeamsDust();
    private final float[] sunPosition = new float[2];
    /** The rig in use, NULL until the first one is ready */
    @Nullable
    private SunbeamsRig rig = null;
    /** The rig that is being built, NULL while none is */
    @Nullable
    private CompletableFuture<SunbeamsRig> building = null;
    /** True after a build failed, so it doesn't get tried again for this screen size */
    private boolean buildFailed = false;
    /** Light step of every screen pixel, for the dust */
    @NotNull
    private byte[] levels = new byte[0];
    /** Light step every lit edge pixel got painted with, -1 to paint it again */
    @NotNull
    private byte[] rimLevels = new byte[0];
    @NotNull
    private SunbeamsPalette palette = SunbeamsPalette.NATURAL;
    @Nullable
    private String lastColorHex = null;
    private boolean lastHitElements = true;
    private boolean lastGlow = true;
    private int lastIntensity = 100;
    private float shadeStartedAt = -Float.MAX_VALUE;
    private boolean repaintAll = true;
    private int repaintRow = 0;
    private float repaintCarry = 0.0F;
    private final float[] glintX = new float[MAX_GLINTS];
    private final float[] glintY = new float[MAX_GLINTS];
    private final float[] glintAge = new float[MAX_GLINTS];
    private int glintCount = 0;
    private float nextGlint = 1.0F;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.sunSide.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.WB_SUNNY)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.sunbeams.sun_side.desc")));

        this.sunAngle.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.ROTATE_RIGHT)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.sunbeams.sun_angle.desc")));

        this.sunColor.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.sunbeams.sun_color.desc")));

        this.sunIntensity.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.BRIGHTNESS_HIGH)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.sunbeams.sun_intensity.desc")));

        menu.addSeparatorEntry("separator_before_sunbeams_toggles");

        this.sunGlow.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.FLARE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.sunbeams.sun_glow.desc")));

        this.dust.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.GRAIN)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.sunbeams.dust.desc")));

        this.hitElements.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SHADOW)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.sunbeams.hit_elements.desc")));

    }

    @Override
    protected void onResize(int width, int height) {
        this.motes.setup(width, height, this.random);
        this.levels = new byte[width * height];
        // A new rig for the new size gets built with the next update
        this.rig = null;
        this.buildFailed = false;
    }

    @Override
    protected void update(float deltaSeconds) {
        int width = this.getWidth();
        int height = this.getHeight();
        if ((width <= 0) || (height <= 0)) return;
        float diagonal = (float)Math.hypot(width, height);
        float scale = diagonal / STAGE_DIAGONAL;
        boolean hit = this.hitElements.tryGetNonNullElse(true);
        boolean glow = this.sunGlow.tryGetNonNullElse(true);
        int intensityPercent = Math.max(10, Math.min(200, this.sunIntensity.getInteger()));
        float intensity = intensityPercent / 100.0F;
        this.syncPalette();
        if ((hit != this.lastHitElements) || (glow != this.lastGlow) || (intensityPercent != this.lastIntensity)) this.repaintAll = true;
        this.lastHitElements = hit;
        this.lastGlow = glow;
        this.lastIntensity = intensityPercent;

        int angle = Math.max(MIN_ANGLE, Math.min(MAX_ANGLE, this.sunAngle.getInteger()));
        float[] sun = this.sunPosition;
        SunbeamsRig.placeSun(this.sunSide.tryGetNonNullElse(SunbeamsSide.TOP), angle, width, height, SUN_DISTANCE * diagonal, sun);
        this.syncRig(sun[0], sun[1], width, height, hit, scale);
        SunbeamsRig current = this.rig;
        if (current == null) return;

        this.rays.update(deltaSeconds, this.getTime(), current, scale, intensity, this.random);
        current.setGlow(glow ? (GLOW_STRENGTH * intensity) : 0.0F, STAGE_GLOW_RADIUS * scale);
        this.repaintLight(current, deltaSeconds, hit);
        if (hit) {
            this.repaintRim(current);
            this.updateGlints(current, deltaSeconds);
        } else {
            this.glintCount = 0;
        }
        if (this.dust.tryGetNonNullElse(true)) this.motes.update(deltaSeconds, this.pointer, width, height);
    }

    private void syncPalette() {
        String hex = this.sunColor.getHex();
        if (Objects.equals(hex, this.lastColorHex)) return;
        this.lastColorHex = hex;
        this.palette = SunbeamsPalette.of(this.sunColor.getDrawable().getColorInt());
        this.repaintAll = true;
        Arrays.fill(this.rimLevels, (byte)-1);
    }

    /**
     * Takes a finished rig and starts building a new one when the sun or the screen changed, or when the elements moved
     * (at most every {@link #SHADE_REBUILD_INTERVAL} seconds). Only one rig gets built at a time.
     */
    private void syncRig(float sunX, float sunY, int width, int height, boolean hit, float scale) {
        CompletableFuture<SunbeamsRig> pending = this.building;
        if (pending != null) {
            if (!pending.isDone()) return;
            this.building = null;
            try {
                SunbeamsRig built = pending.join();
                // A rig for an older screen size doesn't fit the pixels anymore
                if ((built.width == width) && (built.height == height)) this.takeRig(built, scale);
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to build the light of the Sunbeams overlay!", ex);
                this.buildFailed = true;
            }
        }
        if (this.buildFailed) return;
        SunbeamsRig current = this.rig;
        boolean newSun = (current == null) || !current.matches(sunX, sunY, width, height);
        boolean newShade = !newSun && hit && !current.hasShadeFor(this.getElementBoxes()) && (this.getTime() - this.shadeStartedAt >= SHADE_REBUILD_INTERVAL);
        if (!newSun && !newShade) return;
        List<CollisionAreaBounds> boxes = hit ? List.copyOf(this.getElementBoxes()) : List.of();
        if (newSun) {
            this.building = CompletableFuture.supplyAsync(() -> {
                SunbeamsRig built = new SunbeamsRig(sunX, sunY, width, height);
                if (hit) built.buildShade(boxes);
                return built;
            });
        } else {
            this.building = CompletableFuture.supplyAsync(() -> {
                SunbeamsRig built = new SunbeamsRig(current);
                built.buildShade(boxes);
                return built;
            });
        }
        this.shadeStartedAt = this.getTime();
    }

    private void takeRig(@NotNull SunbeamsRig built, float scale) {
        SunbeamsRig old = this.rig;
        this.rig = built;
        // New rays only for a new fan, a new shade keeps them
        if ((old == null) || (old.bins != built.bins)) this.rays.setup(built.fanWidth, scale, this.random);
        this.rimCanvas.resize(built.rimRight - built.rimLeft, built.rimBottom - built.rimTop);
        this.rimLevels = new byte[built.rimCount];
        Arrays.fill(this.rimLevels, (byte)-1);
        this.glintCount = 0;
        this.repaintAll = true;
    }

    /**
     * Repaints the next band of rows of the light, or all of it after something changed.
     */
    private void repaintLight(@NotNull SunbeamsRig current, float deltaSeconds, boolean shaded) {
        int width = current.width;
        int height = current.height;
        int[] pixels = this.lightCanvas.pixels();
        if (pixels.length != width * height) return;
        int[] colors = this.palette.light;
        if (this.repaintAll) {
            this.repaintAll = false;
            this.repaintRow = 0;
            this.repaintCarry = 0.0F;
            this.rays.paint(current, 0, width * height, shaded, this.levels, pixels, colors);
            this.lightCanvas.markAllDirty();
            return;
        }
        float area = width * height / SunbeamsDust.STAGE_AREA;
        float rate = (area <= 1.0F) ? REPAINT_RATE : Math.max(MIN_REPAINT_RATE, REPAINT_RATE / (float)Math.sqrt(area));
        this.repaintCarry += deltaSeconds * height * rate;
        int rows = Math.min(height, (int)this.repaintCarry);
        this.repaintCarry -= rows;
        while (rows > 0) {
            int band = Math.min(rows, height - this.repaintRow);
            this.rays.paint(current, this.repaintRow * width, (this.repaintRow + band) * width, shaded, this.levels, pixels, colors);
            this.lightCanvas.markDirty(0, this.repaintRow, width, band);
            this.repaintRow = (this.repaintRow + band) % height;
            rows -= band;
        }
    }

    /**
     * Repaints the lit edges when the light on any of them changed its step. They take the light without shade, they are where it lands.
     * The whole canvas gets painted again (a few hundred pixels), but only the area around the changed edges gets uploaded.
     */
    private void repaintRim(@NotNull SunbeamsRig current) {
        int count = current.rimCount;
        if ((count == 0) || (this.rimLevels.length != count)) return;
        int left = current.rimLeft;
        int top = current.rimTop;
        int changedLeft = Integer.MAX_VALUE;
        int changedTop = Integer.MAX_VALUE;
        int changedRight = Integer.MIN_VALUE;
        int changedBottom = Integer.MIN_VALUE;
        for (int k = 0; k < count; k++) {
            byte level = (byte)SunbeamsRays.step(this.rays.lightAt(current, current.rimIndex[k]));
            if (level == this.rimLevels[k]) continue;
            this.rimLevels[k] = level;
            changedLeft = Math.min(changedLeft, Math.min(current.rimX[k], current.rimBevelX[k]));
            changedTop = Math.min(changedTop, Math.min(current.rimY[k], current.rimBevelY[k]));
            changedRight = Math.max(changedRight, Math.max(current.rimX[k], current.rimBevelX[k]));
            changedBottom = Math.max(changedBottom, Math.max(current.rimY[k], current.rimBevelY[k]));
        }
        if (changedRight < changedLeft) return;
        PixelCanvas canvas = this.rimCanvas;
        int[] pixels = canvas.pixels();
        int stride = canvas.width();
        if (pixels.length != stride * canvas.height()) return;
        // Edges overlap at the corners of an element, where a bevel lies under the next edge pixel, so the pixels get blended in order
        Arrays.fill(pixels, 0);
        for (int k = 0; k < count; k++) {
            int level = this.rimLevels[k];
            if (level <= 0) continue;
            int i = (current.rimY[k] - top) * stride + (current.rimX[k] - left);
            pixels[i] = PixelMath.blendOver(pixels[i], this.palette.rim[level]);
            int b = (current.rimBevelY[k] - top) * stride + (current.rimBevelX[k] - left);
            pixels[b] = PixelMath.blendOver(pixels[b], this.palette.rimBevel[level]);
        }
        canvas.markDirty(changedLeft - left, changedTop - top, changedRight - changedLeft + 1, changedBottom - changedTop + 1);
    }

    /**
     * Now and then a lit edge flashes a glint.
     */
    private void updateGlints(@NotNull SunbeamsRig current, float deltaSeconds) {
        int kept = 0;
        for (int i = 0; i < this.glintCount; i++) {
            this.glintAge[i] += deltaSeconds;
            if (this.glintAge[i] >= GLINT_LIFE) continue;
            this.glintX[kept] = this.glintX[i];
            this.glintY[kept] = this.glintY[i];
            this.glintAge[kept] = this.glintAge[i];
            kept++;
        }
        this.glintCount = kept;
        if ((this.nextGlint -= deltaSeconds) > 0.0F) return;
        this.nextGlint = this.random.range(0.5F, 1.5F);
        if ((current.rimCount == 0) || (this.rimLevels.length != current.rimCount) || (this.glintCount >= MAX_GLINTS)) return;
        for (int attempt = 0; attempt < 8; attempt++) {
            int k = this.random.index(current.rimCount);
            if (this.rimLevels[k] >= 2) {
                this.glintX[this.glintCount] = current.rimX[k];
                this.glintY[this.glintCount] = current.rimY[k];
                this.glintAge[this.glintCount] = 0.0F;
                this.glintCount++;
                break;
            }
        }
    }

    @Override
    protected void drawElement(@NotNull PixelGraphics graphics) {
        SunbeamsRig current = this.rig;
        if ((current == null) || !this.hitElements.tryGetNonNullElse(true) || (current.rimCount == 0)) return;
        graphics.canvas(this.rimCanvas, current.rimLeft, current.rimTop);
        for (int i = 0; i < this.glintCount; i++) {
            float u = this.glintAge[i] / GLINT_LIFE;
            // Grows from the small to the big frame and back
            PixelSprite glint = SunbeamsDust.GLINTS[((u < 0.2F) || (u >= 0.8F)) ? 0 : (((u < 0.4F) || (u >= 0.6F)) ? 1 : 2)];
            graphics.sprite(glint, this.palette.glint, (int)this.glintX[i] - (glint.width() >> 1), (int)this.glintY[i] - (glint.height() >> 1));
        }
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        if (this.rig == null) return;
        graphics.canvas(this.lightCanvas, 0, 0);
        if (this.dust.tryGetNonNullElse(true)) this.motes.draw(graphics, this.levels, this.getWidth(), this.palette);
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        this.rimCanvas.releaseTexture();
    }

}
