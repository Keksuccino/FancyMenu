package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayLook;
import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
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
 * Groups of fireflies that flash in loose waves, follow the pointer and rest on elements, with a dim far layer behind the elements.
 * The simple look (the default) draws small squares with a soft halo, the detailed look plus-shaped lights with a dithered halo and a trail.<br>
 * Far fireflies are back content and fireflies resting on elements element content. All others are front content,
 * fireflies resting on the floor included.
 */
public class FireflyDecorationOverlay extends PixelDecorationOverlay<FireflyDecorationOverlay> {

    /** Screen area (GUI pixels) per group at a Group Amount of 1, with at least {@value #MIN_BASE_GROUPS} groups */
    private static final int AREA_PER_GROUP = 180000;
    private static final int MIN_BASE_GROUPS = 2;
    private static final int MAX_BASE_GROUPS = 24;
    private static final int FIREFLIES_PER_GROUP = 11;
    /** Far fireflies on a screen of the mockup's size (480 x 270) */
    private static final int FAR_FIREFLIES = 18;
    private static final int MAX_FAR_FIREFLIES = 240;
    private static final float REFERENCE_AREA = 480.0F * 270.0F;
    private static final float MIN_MULTIPLIER = 0.1F;
    private static final float MAX_MULTIPLIER = 4.0F;
    /** Reading the options parses placeholders, so they get read a few times per second instead of every frame */
    private static final float SETTINGS_REFRESH_SECONDS = 0.1F;

    public final DecorationOverlayOptions.Look look = new DecorationOverlayOptions.Look(this, DecorationOverlayLook.SIMPLE);
    public final Property.BooleanProperty fireflyUnlitBodies = putProperty(Property.booleanProperty("firefly_unlit_bodies", false, "fancymenu.decoration_overlays.fireflies.unlit_bodies"));
    public final Property.FloatProperty fireflyGroupDensity = putProperty(Property.floatProperty("firefly_group_density", 1.0F, "fancymenu.decoration_overlays.fireflies.intensity"));
    public final Property.FloatProperty fireflyGroupAmount = putProperty(Property.floatProperty("firefly_group_amount", 1.0F, "fancymenu.decoration_overlays.fireflies.group_amount"));
    public final Property.FloatProperty fireflyGroupSize = putProperty(Property.floatProperty("firefly_group_size", 1.0F, "fancymenu.decoration_overlays.fireflies.group_size"));
    public final Property.FloatProperty fireflyScale = putProperty(Property.floatProperty("firefly_scale", 1.0F, "fancymenu.decoration_overlays.fireflies.scale"));
    public final Property<Boolean> fireflyFollowMouse = putProperty(Property.booleanProperty("firefly_follow_mouse", true, "fancymenu.decoration_overlays.fireflies.follow_mouse"));
    public final Property<Boolean> fireflyLanding = putProperty(Property.booleanProperty("firefly_landing", true, "fancymenu.decoration_overlays.fireflies.landing"));
    public final Property.ColorProperty fireflyColorHex = putProperty(Property.hexColorProperty("firefly_color_hex", "#FFE08A", true, "fancymenu.decoration_overlays.fireflies.color"));

    private final PixelSurfaces<Void> surfaces = this.createSurfaces(null);
    private final List<FireflyGroup> groups = new ArrayList<>();
    private final List<Firefly> far = new ArrayList<>();
    @NotNull
    private FireflyArt art = FireflyArt.DEFAULT;
    private String lastColorHex = null;
    private boolean settingsRead = false;
    private float settingsTimer = 0.0F;
    private boolean rebuild = true;
    private boolean simple = true;
    private boolean unlitBodies = false;
    private boolean followPointer = true;
    private boolean landing = true;
    private float groupAmount = 1.0F;
    private float groupDensity = 1.0F;
    private float groupSize = 1.0F;
    private int spriteScale = 1;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.look.addEntry(menu, this);

        this.fireflyUnlitBodies.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.DARK_MODE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.unlit_bodies.desc")));

        menu.addSeparatorEntry("separator_after_firefly_look");

        this.fireflyFollowMouse.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.MOUSE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.follow_mouse.desc")));

        this.fireflyLanding.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.FLIGHT_LAND)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.landing.desc")));

        this.fireflyColorHex.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.PALETTE)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.color.desc")));

        this.fireflyGroupAmount.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.GROUPS)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.group_amount.desc")));

        this.fireflyGroupDensity.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.DENSITY_MEDIUM)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.intensity.desc")));

        this.fireflyGroupSize.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.STRAIGHTEN)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.group_size.desc")));

        this.fireflyScale.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.ZOOM_OUT_MAP)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireflies.scale.desc")));

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
            this.groups.clear();
            this.far.clear();
            this.rebuild = false;
        }
        this.fitCounts(width, height);
        float time = this.getTime();
        for (int i = 0; i < this.far.size(); i++) {
            Firefly firefly = this.far.get(i);
            firefly.advanceFlash(deltaSeconds, this.random, false);
            firefly.heading += this.random.range(-1.0F, 1.0F) * 1.5F * deltaSeconds;
            firefly.x += (float)Math.cos(firefly.heading) * 4.0F * deltaSeconds;
            firefly.y += (float)Math.sin(firefly.heading) * 3.0F * deltaSeconds;
            if (firefly.x < -4.0F) {
                firefly.x += width + 8;
            } else if (firefly.x > width + 4) {
                firefly.x -= width + 8;
            }
            firefly.y = PixelMath.clamp(firefly.y, 20.0F, Math.max(20.0F, height - 12.0F));
        }
        for (int g = 0; g < this.groups.size(); g++) {
            FireflyGroup group = this.groups.get(g);
            group.radius = group.baseRadius * this.groupSize;
            group.move(deltaSeconds, time, this.pointer, this.followPointer, width, height, this.random);
            List<Firefly> members = group.members;
            for (int i = 0; i < members.size(); i++) {
                Firefly firefly = members.get(i);
                if (firefly.advanceFlash(deltaSeconds, this.random, true)) {
                    for (int j = 0; j < members.size(); j++) {
                        if (j != i) members.get(j).nudge();
                    }
                }
                this.updateFlight(firefly, group, deltaSeconds, time, height);
            }
        }
    }

    private void updateFlight(@NotNull Firefly firefly, @NotNull FireflyGroup group, float deltaSeconds, float time, int height) {
        firefly.landingCooldown = Math.max(0.0F, firefly.landingCooldown - deltaSeconds);
        if (firefly.state == Firefly.FLYING) {
            firefly.fly(deltaSeconds, time, group.x, group.y, group.radius, this.random);
            if (this.landing && (firefly.landingCooldown <= 0.0F) && this.random.chance(0.03F * deltaSeconds)) {
                this.pickLandingSpot(firefly, height);
                firefly.state = Firefly.LANDING;
                firefly.landingSpeed = this.random.range(20.0F, 40.0F);
            }
        } else if (firefly.state == Firefly.LANDING) {
            if (firefly.approachTarget(deltaSeconds, time)) {
                firefly.state = Firefly.LANDED;
                firefly.restTime = 0.0F;
                firefly.restDuration = this.random.range(2.5F, 6.0F);
                firefly.trailCount = 0;
            }
        } else {
            firefly.restTime += deltaSeconds;
            if (firefly.restTime >= firefly.restDuration) {
                firefly.state = Firefly.FLYING;
                firefly.landingCooldown = this.random.range(3.0F, 8.0F);
                firefly.heading = (float)(-Math.PI / 2.0) + this.random.range(-0.8F, 0.8F);
            }
        }
        if (firefly.state != Firefly.LANDED) firefly.recordTrail(deltaSeconds);
    }

    /**
     * Picks where a firefly lands: the floor, or any of the four edges of an element, wider surfaces more often.
     */
    private void pickLandingSpot(@NotNull Firefly firefly, int height) {
        this.aimAt(firefly, this.surfaces.pick(this.random), height);
    }

    /**
     * Picks a landing spot on the surface: anywhere on the floor, or on any of the four edges of an element.
     */
    void aimAt(@NotNull Firefly firefly, @NotNull PixelSurfaces.Surface<?> surface, int height) {
        firefly.targetOnElement = !surface.isFloor();
        if (surface.isFloor()) {
            firefly.targetX = surface.x() + this.random.next() * surface.width();
            firefly.targetY = height - 1;
            return;
        }
        int edge = this.random.rangeInt(0, 3);
        firefly.targetX = (edge < 2) ? (surface.x() + this.random.next() * surface.width()) : ((edge == 2) ? surface.x() : (surface.right() - 1));
        firefly.targetY = (edge == 0) ? surface.restY() : ((edge == 1) ? surface.bottom() : (surface.y() + this.random.next() * surface.height()));
    }

    /**
     * Grows or shrinks the groups, their members and the far layer to the wanted counts.
     */
    private void fitCounts(int width, int height) {
        int baseGroups = PixelMath.clamp((width * height) / AREA_PER_GROUP, MIN_BASE_GROUPS, MAX_BASE_GROUPS);
        int wantedGroups = PixelMath.clamp(PixelMath.floor(baseGroups * this.groupAmount), 1, (int)(MAX_BASE_GROUPS * MAX_MULTIPLIER));
        boolean fresh = this.groups.isEmpty();
        while (this.groups.size() < wantedGroups) {
            int index = this.groups.size();
            // A fresh screen spreads its groups over the width, groups added later go anywhere
            float slot = fresh ? (index + this.random.range(0.1F, 0.9F)) / wantedGroups : this.random.next();
            float x = (width > 140) ? PixelMath.lerp(70.0F, width - 70.0F, slot) : width * 0.5F;
            float y = (height > 120) ? this.random.range(60.0F, height - 60.0F) : height * 0.5F;
            this.groups.add(new FireflyGroup(x, y, this.random.range(22.0F, 56.0F)));
        }
        while (this.groups.size() > wantedGroups) this.groups.remove(this.groups.size() - 1);
        int wantedMembers = PixelMath.clamp(PixelMath.floor(FIREFLIES_PER_GROUP * this.groupDensity), 1, (int)(FIREFLIES_PER_GROUP * MAX_MULTIPLIER));
        for (int g = 0; g < this.groups.size(); g++) {
            FireflyGroup group = this.groups.get(g);
            group.radius = group.baseRadius * this.groupSize;
            while (group.members.size() < wantedMembers) group.addMember(this.random);
            while (group.members.size() > wantedMembers) group.members.remove(group.members.size() - 1);
        }
        float areaFactor = (width * height) / REFERENCE_AREA;
        int wantedFar = PixelMath.clamp(Math.round(FAR_FIREFLIES * areaFactor * this.groupAmount), 0, MAX_FAR_FIREFLIES);
        while (this.far.size() < wantedFar) {
            Firefly firefly = new Firefly();
            firefly.x = this.random.range(0.0F, width);
            firefly.y = this.random.range(30.0F, Math.max(30.0F, height - 20.0F));
            firefly.heading = this.random.range(0.0F, PixelMath.TAU);
            firefly.nextFlash = this.random.range(0.0F, 6.0F);
            this.far.add(firefly);
        }
        while (this.far.size() > wantedFar) this.far.remove(this.far.size() - 1);
    }

    @Override
    protected void drawBack(@NotNull PixelGraphics graphics) {
        for (int i = 0; i < this.far.size(); i++) {
            Firefly firefly = this.far.get(i);
            int level = firefly.level();
            if (level == 0) continue;
            graphics.pixel(PixelMath.floor(firefly.x), PixelMath.floor(firefly.y), (level >= 2) ? this.art.farBright : this.art.farLit);
        }
    }

    @Override
    protected void drawElement(@NotNull PixelGraphics graphics) {
        this.drawGroups(graphics, true);
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        this.drawGroups(graphics, false);
    }

    /**
     * @param onElements true to draw the fireflies resting on elements, false for all others
     */
    private void drawGroups(@NotNull PixelGraphics graphics, boolean onElements) {
        float time = this.getTime();
        for (int g = 0; g < this.groups.size(); g++) {
            List<Firefly> members = this.groups.get(g).members;
            for (int i = 0; i < members.size(); i++) {
                Firefly firefly = members.get(i);
                if (isRestingOnElement(firefly) != onElements) continue;
                if (this.simple) {
                    this.drawSimple(graphics, firefly);
                } else {
                    this.drawDetailed(graphics, firefly, time);
                }
            }
        }
    }

    /**
     * Fireflies resting on elements belong to the elements, so they stay in front of them (and of background elements) with any Layers setting.
     */
    static boolean isRestingOnElement(@NotNull Firefly firefly) {
        return (firefly.state == Firefly.LANDED) && firefly.targetOnElement;
    }

    private void drawSimple(@NotNull PixelGraphics graphics, @NotNull Firefly firefly) {
        int s = this.spriteScale;
        int level = firefly.level();
        int x = PixelMath.floor(firefly.x);
        int y = PixelMath.floor(firefly.y);
        if (level == 0) {
            if (this.unlitBodies) graphics.rect(x, y, s, s, FireflyArt.UNLIT_BODY);
            return;
        }
        PixelSprite sprite = FireflyArt.SIMPLE[firefly.big ? 1 : 0][Math.min(level, 3)];
        int offset = (sprite.width() - (firefly.big ? 2 : 1)) / 2 * s;
        graphics.sprite(sprite, this.art.simple[Math.min(level, 3)], x - offset, y - offset, false, false, s, this.art.tint);
    }

    private void drawDetailed(@NotNull PixelGraphics graphics, @NotNull Firefly firefly, float time) {
        int s = this.spriteScale;
        int level = firefly.level();
        int x = PixelMath.floor(firefly.x);
        int y = PixelMath.floor(firefly.y);
        if ((level >= 2) && (firefly.state != Firefly.LANDED)) {
            for (int i = 0; i < firefly.trailCount; i++) {
                int alpha = Math.round(FireflyArt.TRAIL_ALPHAS[i] * this.tintAlpha());
                graphics.rect(PixelMath.floor(firefly.trailX[i]), PixelMath.floor(firefly.trailY[i]), s, s, PixelMath.withAlpha(this.art.trailRgb, alpha));
            }
        }
        if (level == 0) {
            if (!this.unlitBodies) return;
            graphics.rect(x, y, s, s, FireflyArt.UNLIT_BODY);
            if ((firefly.state != Firefly.LANDED) && ((PixelMath.floor(time * 16.0F + firefly.wobblePhase) & 1) != 0)) {
                graphics.rect(x - s, y - s, s, s, FireflyArt.UNLIT_WINGS);
                graphics.rect(x + s, y - s, s, s, FireflyArt.UNLIT_WINGS);
            }
            return;
        }
        PixelSprite halo = FireflyArt.HALOS[Math.min(level, 3)];
        int offset = ((halo.width() - 1) / 2) * s;
        graphics.sprite(halo, this.art.detailed[Math.min(level, 3)], x - offset, y - offset, false, false, s, this.art.tint);
    }

    private float tintAlpha() {
        return (this.art.tint == -1) ? 1.0F : (this.art.tint >>> 24) / 255.0F;
    }

    /**
     * Reads all options.
     */
    void refreshSettings() {
        this.settingsRead = true;
        this.simple = this.look.isSimple();
        this.unlitBodies = this.fireflyUnlitBodies.getBoolean();
        this.followPointer = this.fireflyFollowMouse.tryGetNonNullElse(true);
        this.landing = this.fireflyLanding.tryGetNonNullElse(true);
        this.groupAmount = PixelMath.clamp(finiteOr(this.fireflyGroupAmount.getFloat(), 1.0F), MIN_MULTIPLIER, MAX_MULTIPLIER);
        this.groupDensity = PixelMath.clamp(finiteOr(this.fireflyGroupDensity.getFloat(), 1.0F), MIN_MULTIPLIER, MAX_MULTIPLIER);
        this.groupSize = PixelMath.clamp(finiteOr(this.fireflyGroupSize.getFloat(), 1.0F), 0.5F, 2.0F);
        this.spriteScale = snapScale(this.fireflyScale.getFloat());
        String colorHex = this.fireflyColorHex.getHex();
        if (!Objects.equals(colorHex, this.lastColorHex)) {
            this.lastColorHex = colorHex;
            this.art = FireflyArt.of(this.fireflyColorHex.getDrawable().getColorInt());
        }
    }

    /**
     * The Firefly Scale snapped to a whole number from 1 to 4, so every art pixel stays a clean square.
     */
    static int snapScale(float scale) {
        return PixelMath.clamp(Math.round(finiteOr(scale, 1.0F)), 1, 4);
    }

    @NotNull
    FireflyArt getArt() {
        return this.art;
    }

    boolean isSimple() {
        return this.simple;
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

}
