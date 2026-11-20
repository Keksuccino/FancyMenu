package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayLook;
import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Falling autumn leaves in a detailed look (shaded ovate and maple leaves that flip and spin, curling wind strokes) and a simple look
 * (flat two-tone leaves, dashed wind strokes). Leaves land on elements and the floor, then dissolve or get lifted by the next gust.<br>
 * The small far leaves are back content and leaves lying on elements element content. Everything else is front content,
 * leaves lying on the floor included.
 */
public class LeavesDecorationOverlay extends PixelDecorationOverlay<LeavesDecorationOverlay> {

    /** Leaves on a screen of the mockup's size (480 x 270), at a Leaf Density of 1 */
    private static final int NEAR_LEAVES = 26;
    private static final int FAR_LEAVES = 22;
    private static final int MAX_NEAR_LEAVES = 320;
    private static final int MAX_FAR_LEAVES = 280;
    private static final float REFERENCE_AREA = 480.0F * 270.0F;
    /** Leaves within this many pixels of the front of a gust get pushed */
    private static final float GUST_REACH = 26.0F;
    /** Seconds a landed leaf takes to dissolve */
    private static final float DISSOLVE_SECONDS = 0.6F;
    /** Reading the options parses placeholders, so they get read a few times per second instead of every frame */
    private static final float SETTINGS_REFRESH_SECONDS = 0.1F;

    public final DecorationOverlayOptions.Look look = new DecorationOverlayOptions.Look(this, DecorationOverlayLook.DETAILED);
    public final Property<LeavesColorMode> leavesColors = putProperty(Property.enumProperty("leaves_colors", LeavesColorMode.AUTUMN_MIX, "fancymenu.decoration_overlays.leaves.colors", LeavesColorMode::getLocalizationKey));
    public final Property.FloatProperty leavesDensity = putProperty(Property.floatProperty("leaves_density", 1.0F, "fancymenu.decoration_overlays.leaves.density"));
    public final Property.FloatProperty leavesWindIntensity = putProperty(Property.floatProperty("leaves_wind_intensity", 1.0F, "fancymenu.decoration_overlays.leaves.wind"));
    public final Property<Boolean> leavesWindBlows = putProperty(Property.booleanProperty("leaves_wind_blows", true, "fancymenu.decoration_overlays.leaves.wind_blows"));
    public final Property.FloatProperty leavesFallSpeed = putProperty(Property.floatProperty("leaves_fall_speed", 1.0F, "fancymenu.decoration_overlays.leaves.fall_speed"));
    public final Property.FloatProperty leavesScale = putProperty(Property.floatProperty("leaves_scale", 1.0F, "fancymenu.decoration_overlays.leaves.scale"));
    public final Property.ColorProperty leavesColorStartHex = putProperty(Property.hexColorProperty("leaves_color_start_hex", "#7BA84F", true, "fancymenu.decoration_overlays.leaves.color_start"));
    public final Property.ColorProperty leavesColorEndHex = putProperty(Property.hexColorProperty("leaves_color_end_hex", "#D58A3B", true, "fancymenu.decoration_overlays.leaves.color_end"));

    private final PixelSurfaces<Void> surfaces = this.createSurfaces(null);
    private final List<Leaf> near = new ArrayList<>();
    private final List<Leaf> far = new ArrayList<>();
    private final Gust gust = new Gust();
    private float nextGust = 3.5F;
    @NotNull
    private LeafArt art = LeafArt.AUTUMN;
    @NotNull
    private LeavesColorMode colorMode = LeavesColorMode.AUTUMN_MIX;
    private String lastStartHex = null;
    private String lastEndHex = null;
    private boolean settingsRead = false;
    private float settingsTimer = 0.0F;
    private boolean rebuild = true;
    private boolean simple = false;
    private float density = 1.0F;
    private float windIntensity = 1.0F;
    private boolean windBlows = true;
    private float fallSpeed = 1.0F;
    private int spriteScale = 1;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.look.addEntry(menu, this);

        this.leavesColors.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.GRADIENT)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.colors.desc")));

        this.leavesColorStartHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .addIsActiveSupplier((menu1, entry) -> this.isCustomRange())
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.color_start.desc")));

        this.leavesColorEndHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .addIsActiveSupplier((menu1, entry) -> this.isCustomRange())
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.color_end.desc")));

        menu.addSeparatorEntry("separator_after_leaves_colors");

        this.leavesDensity.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.DENSITY_MEDIUM)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.density.desc")));

        this.leavesWindIntensity.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.AIR)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.wind.desc")));

        this.leavesWindBlows.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.AIR)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.wind_blows.desc")));

        this.leavesFallSpeed.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.SPEED)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.fall_speed.desc")));

        this.leavesScale.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.STRAIGHTEN)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.leaves.scale.desc")));

    }

    private boolean isCustomRange() {
        return this.leavesColors.tryGetNonNullElse(LeavesColorMode.AUTUMN_MIX) == LeavesColorMode.CUSTOM_RANGE;
    }

    @Override
    protected void onResize(int width, int height) {
        this.rebuild = true;
    }

    @Override
    protected void update(float deltaSeconds) {
        this.settingsTimer -= deltaSeconds;
        if (!this.settingsRead || (this.settingsTimer <= 0.0F)) {
            this.refreshSettings();
            this.settingsTimer = SETTINGS_REFRESH_SECONDS;
        }
        int width = this.getWidth();
        int height = this.getHeight();
        if (this.rebuild) {
            this.near.clear();
            this.far.clear();
            this.rebuild = false;
        }
        boolean fresh = this.near.isEmpty() && this.far.isEmpty();
        float areaFactor = (width * height) / REFERENCE_AREA;
        this.fitCount(this.near, PixelMath.clamp(Math.round(NEAR_LEAVES * areaFactor * this.density), 0, MAX_NEAR_LEAVES), false, fresh, width, height);
        this.fitCount(this.far, PixelMath.clamp(Math.round(FAR_LEAVES * areaFactor * this.density), 0, MAX_FAR_LEAVES), true, fresh, width, height);
        this.updateGust(deltaSeconds, width, height);
        float time = this.getTime();
        float wind = (5.0F * (float)Math.sin(time * 0.21F) + 3.0F * (float)Math.sin(time * 0.57F + 1.1F)) * this.windIntensity;
        for (int i = 0; i < this.near.size(); i++) this.updateLeaf(this.near.get(i), deltaSeconds, wind, width, height);
        for (int i = 0; i < this.far.size(); i++) this.updateLeaf(this.far.get(i), deltaSeconds, wind, width, height);
    }

    private void fitCount(@NotNull List<Leaf> leaves, int wanted, boolean far, boolean anywhere, int width, int height) {
        while (leaves.size() < wanted) {
            Leaf leaf = new Leaf();
            leaf.respawn(anywhere, far, width, height, this.random);
            leaves.add(leaf);
        }
        while (leaves.size() > wanted) leaves.remove(leaves.size() - 1);
    }

    private void updateGust(float deltaSeconds, int width, int height) {
        if (this.gust.isActive()) {
            this.gust.advance(deltaSeconds, width, 130.0F);
            if (this.gust.isActive()) {
                for (int i = 0; i < this.near.size(); i++) this.pushByGust(this.near.get(i));
                for (int i = 0; i < this.far.size(); i++) this.pushByGust(this.far.get(i));
            }
            return;
        }
        if (!this.windBlows) return;
        this.nextGust -= deltaSeconds;
        if (this.nextGust > 0.0F) return;
        int direction = this.random.sign();
        this.gust.start(direction, (direction > 0) ? -40.0F : (width + 40.0F), height, this.random);
        // Stronger wind blows more often
        this.nextGust = this.random.range(7.0F, 12.0F) / PixelMath.clamp(this.windIntensity, 0.5F, 3.0F);
    }

    /**
     * A gust lifts most landed leaves it reaches back into the air and kicks most falling ones sideways.
     */
    private void pushByGust(@NotNull Leaf leaf) {
        if (leaf.lastGust == this.gust.id()) return;
        float leafX = leaf.landed ? leaf.landX : leaf.glideX();
        if (Math.abs(leafX - this.gust.x()) >= GUST_REACH) return;
        leaf.lastGust = this.gust.id();
        float strength = PixelMath.clamp(this.windIntensity, 0.25F, 3.0F);
        if (leaf.landed) {
            if (!this.random.chance(0.75F)) return;
            leaf.x = leaf.landX;
            leaf.y = leaf.landY - 3 * this.spriteScale;
            leaf.phase = 0.0F;
            leaf.landed = false;
            leaf.lift = this.random.range(30.0F, 60.0F);
            leaf.kick = this.gust.direction() * this.random.range(70.0F, 120.0F) * strength;
        } else if (this.random.chance(0.7F)) {
            leaf.kick = this.gust.direction() * this.random.range(60.0F, 110.0F) * (leaf.far ? 0.5F : 1.0F) * strength;
            leaf.lift = Math.max(leaf.lift, this.random.range(0.0F, 16.0F));
        }
    }

    private void updateLeaf(@NotNull Leaf leaf, float deltaSeconds, float wind, int width, int height) {
        int s = this.spriteScale;
        if (leaf.landed) {
            leaf.landTime += deltaSeconds;
            if (leaf.landTime > leaf.landDuration + DISSOLVE_SECONDS) leaf.respawn(false, false, width, height, this.random);
            return;
        }
        float previousCos = (float)Math.cos(leaf.phase);
        leaf.phase += leaf.swing * deltaSeconds;
        float cos = (float)Math.cos(leaf.phase);
        // The leaf flips to its other side at every turning point of the glide
        if ((previousCos > 0.0F) != (cos > 0.0F)) {
            leaf.back = !leaf.back;
            leaf.edgeTime = 0.09F;
        }
        leaf.edgeTime = Math.max(0.0F, leaf.edgeTime - deltaSeconds);
        float previousY = leaf.y;
        leaf.y += (leaf.fallSpeed * this.fallSpeed * (0.35F + 0.65F * Math.abs(cos)) - leaf.lift) * deltaSeconds;
        leaf.x += (wind + leaf.kick) * deltaSeconds;
        leaf.kick *= (float)Math.exp(-1.6F * deltaSeconds);
        leaf.lift *= (float)Math.exp(-2.5F * deltaSeconds);
        if (!leaf.far && !leaf.big && (leaf.lift < 4.0F)) this.tryLanding(leaf, previousY);
        if (leaf.landed) return;
        if (leaf.y > height + 10 * s) {
            leaf.respawn(false, leaf.far, width, height, this.random);
            return;
        }
        if (leaf.x < -40.0F) {
            leaf.x += width + 60;
        } else if (leaf.x > width + 40) {
            leaf.x -= width + 60;
        }
    }

    /**
     * Lands the leaf on the first surface its bottom crossed since the last frame. It lies there flat.
     */
    private void tryLanding(@NotNull Leaf leaf, float previousY) {
        int s = this.spriteScale;
        float glideX = leaf.glideX();
        float centerX = glideX + 3.5F * s;
        float bottom = 7.0F * s;
        List<PixelSurfaces.Surface<Void>> all = this.surfaces.all();
        for (int i = 0; i < all.size(); i++) {
            PixelSurfaces.Surface<Void> surface = all.get(i);
            int top = surface.restY() + 1;
            if (!surface.spans(centerX) || (previousY + bottom > top) || (leaf.y + bottom <= top)) continue;
            this.land(leaf, surface, glideX);
            return;
        }
    }

    /**
     * Lays the leaf flat on top of the surface, at its glide position but fully on the surface.
     */
    void land(@NotNull Leaf leaf, @NotNull PixelSurfaces.Surface<?> surface, float glideX) {
        int s = this.spriteScale;
        leaf.landed = true;
        leaf.landedOnElement = !surface.isFloor();
        leaf.landX = PixelMath.floor(PixelMath.clamp(glideX, surface.x(), surface.right() - 7.0F * s));
        leaf.landY = surface.restY() + 1 - 3 * s;
        leaf.landTime = 0.0F;
        leaf.landDuration = this.random.range(3.0F, 7.0F);
        leaf.landFlip = this.random.chance(0.5F);
    }

    @Override
    protected void drawBack(@NotNull PixelGraphics graphics) {
        int s = this.spriteScale;
        for (int i = 0; i < this.far.size(); i++) {
            Leaf leaf = this.far.get(i);
            boolean flip = Math.cos(leaf.phase) < 0.0;
            int x = PixelMath.floor(leaf.glideX());
            int y = PixelMath.floor(leaf.y);
            if (this.simple) {
                graphics.sprite(LeafArt.SIMPLE_TINY, this.art.simple[leaf.ramp], x, y, flip, false, s);
            } else {
                graphics.sprite(LeafArt.TINY, leaf.back ? this.art.back[leaf.ramp] : this.art.face[leaf.ramp], x, y, flip, false, s);
            }
        }
    }

    /**
     * Leaves lying on elements belong to the elements, so they stay in front of them (and of background elements) with any Layers setting.
     */
    @Override
    protected void drawElement(@NotNull PixelGraphics graphics) {
        for (int i = 0; i < this.near.size(); i++) {
            Leaf leaf = this.near.get(i);
            if (leaf.landed && leaf.landedOnElement) this.drawLanded(graphics, leaf);
        }
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        float time = this.getTime();
        for (int i = 0; i < this.near.size(); i++) {
            Leaf leaf = this.near.get(i);
            if (leaf.landed) {
                if (!leaf.landedOnElement) this.drawLanded(graphics, leaf);
            } else if (this.simple) {
                this.drawSimple(graphics, leaf);
            } else {
                this.drawDetailed(graphics, leaf, time);
            }
        }
        this.gust.drawStrokes(graphics, this.simple, WindStrokes.LEAVES_RGB);
    }

    private void drawLanded(@NotNull PixelGraphics graphics, @NotNull Leaf leaf) {
        int s = this.spriteScale;
        float dissolve = leaf.landTime - leaf.landDuration;
        if (this.simple) {
            graphics.sprite((dissolve <= 0.0F) ? LeafArt.SIMPLE_FLAT : ((dissolve > 0.3F) ? LeafArt.SIMPLE_FLAT_DISSOLVED : LeafArt.SIMPLE_FLAT_DISSOLVING), this.art.simple[leaf.ramp], leaf.landX + s, leaf.landY, leaf.landFlip, false, s);
        } else {
            graphics.sprite((dissolve <= 0.0F) ? LeafArt.FLAT : ((dissolve > 0.3F) ? LeafArt.FLAT_DISSOLVED : LeafArt.FLAT_DISSOLVING), this.art.face[leaf.ramp], leaf.landX, leaf.landY, leaf.landFlip, false, s);
        }
    }

    private void drawSimple(@NotNull PixelGraphics graphics, @NotNull Leaf leaf) {
        int s = this.spriteScale;
        float cos = (float)Math.cos(leaf.phase);
        int x = PixelMath.floor(leaf.glideX());
        int y = PixelMath.floor(leaf.y);
        PixelPalette palette = this.art.simple[leaf.ramp];
        if (cos > 0.35F) {
            graphics.sprite(LeafArt.SIMPLE_LEAF, palette, x + s, y + s, false, true, s);
        } else if (cos < -0.35F) {
            graphics.sprite(LeafArt.SIMPLE_LEAF, palette, x + s, y + s, true, true, s);
        } else {
            graphics.sprite(LeafArt.SIMPLE_FLAT, palette, x + s, y + 2 * s, Math.sin(leaf.phase) < 0.0, false, s);
        }
    }

    /**
     * The sprite always points into the glide: diagonal while it glides, flat while it stalls, edge-on right after a flip.
     */
    private void drawDetailed(@NotNull PixelGraphics graphics, @NotNull Leaf leaf, float time) {
        int s = this.spriteScale;
        float cos = (float)Math.cos(leaf.phase);
        int x = PixelMath.floor(leaf.glideX());
        int y = PixelMath.floor(leaf.y);
        PixelPalette palette = leaf.back ? this.art.back[leaf.ramp] : this.art.face[leaf.ramp];
        if (leaf.big) {
            int turn = (PixelMath.floor(time * leaf.spinRate * 0.5F) + leaf.spinOffset) & 3;
            graphics.sprite(LeafArt.BIG_MAPLE_TURNS[turn], palette, x - 2 * s, y - 2 * s, leaf.back, false, s);
        } else if (leaf.edgeTime > 0.0F) {
            graphics.sprite(LeafArt.EDGE, palette, x + s, y + s, cos < 0.0F, false, s);
        } else if (leaf.type == Leaf.MAPLE) {
            int turn = (PixelMath.floor(time * leaf.spinRate) + leaf.spinOffset) & 3;
            graphics.sprite(LeafArt.MAPLE_TURNS[turn], palette, x, y, false, false, s);
        } else if (cos > 0.35F) {
            graphics.sprite(LeafArt.OVATE, palette, x, y, false, true, s);
        } else if (cos < -0.35F) {
            graphics.sprite(LeafArt.OVATE, palette, x, y, true, true, s);
        } else {
            graphics.sprite(LeafArt.FLAT, palette, x, y + 2 * s, Math.sin(leaf.phase) < 0.0, false, s);
        }
    }

    /**
     * Reads all options and builds the leaf palettes again when the colors changed.
     */
    void refreshSettings() {
        this.settingsRead = true;
        this.simple = this.look.isSimple();
        this.density = PixelMath.clamp(finiteOr(this.leavesDensity.getFloat(), 1.0F), 0.0F, 2.0F);
        this.windIntensity = PixelMath.clamp(finiteOr(this.leavesWindIntensity.getFloat(), 1.0F), 0.0F, 5.0F);
        this.windBlows = this.leavesWindBlows.tryGetNonNullElse(true);
        this.fallSpeed = PixelMath.clamp(finiteOr(this.leavesFallSpeed.getFloat(), 1.0F), 0.01F, 5.0F);
        this.spriteScale = snapScale(this.leavesScale.getFloat());
        LeavesColorMode mode = this.leavesColors.tryGetNonNullElse(LeavesColorMode.AUTUMN_MIX);
        if (mode == LeavesColorMode.AUTUMN_MIX) {
            this.art = LeafArt.AUTUMN;
        } else {
            String startHex = this.leavesColorStartHex.getHex();
            String endHex = this.leavesColorEndHex.getHex();
            if ((this.colorMode != mode) || !Objects.equals(startHex, this.lastStartHex) || !Objects.equals(endHex, this.lastEndHex)) {
                this.lastStartHex = startHex;
                this.lastEndHex = endHex;
                this.art = LeafArt.ofRange(this.leavesColorStartHex.getDrawable().getColorInt(), this.leavesColorEndHex.getDrawable().getColorInt());
            }
        }
        this.colorMode = mode;
    }

    /**
     * The Leaf Overlay Scale snapped to a whole number from 1 to 5, so every art pixel stays a clean square.
     */
    static int snapScale(float scale) {
        return PixelMath.clamp(Math.round(finiteOr(scale, 1.0F)), 1, 5);
    }

    @NotNull
    LeafArt getArt() {
        return this.art;
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

}
