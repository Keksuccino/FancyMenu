package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayOptions;
import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.decorationoverlay.overlays.ParticleLayer;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.properties.Property;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import de.keksuccino.fancymenu.util.rendering.ui.icon.MaterialIcons;
import de.keksuccino.fancymenu.util.rendering.ui.tooltip.UITooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fireworks: rockets climb with a spark trail that cools from white to dark red and burst in the upper half of the screen.
 * A burst opens with a two-frame flash, then every star draws a streak that steps down a four-color ramp as it burns out.
 * Glitter shells end in blinking sparkles and willow shells droop on long gold trails. See {@link FireworksShow} for the simulation.<br><br>
 *
 * The Size option draws the whole show on a grid of bigger art pixels, like the mockup zoomed in, so shells grow with it.
 * All of it is {@code FRONT} content.
 */
public class FireworksDecorationOverlay extends PixelDecorationOverlay<FireworksDecorationOverlay> {

    static final float MIN_EXPLOSION_SIZE = 0.01F;
    static final float MAX_EXPLOSION_SIZE = 5.0F;
    private static final float MIN_WIDTH_FACTOR = 0.5F;
    private static final float MAX_WIDTH_FACTOR = 3.0F;
    /** A star streak is drawn this far back along its flight, the hot inner part is the front 45 percent of it */
    private static final float INNER_TRAIL = 0.45F;

    public final DecorationOverlayOptions.Amount amount = new DecorationOverlayOptions.Amount(this);
    public final DecorationOverlayOptions.Size size = new DecorationOverlayOptions.Size(this);
    public final Property.FloatProperty fireworksExplosionSize = putProperty(Property.floatProperty("fireworks_explosion_size", 1.0F, "fancymenu.decoration_overlays.fireworks.explosion_size"));
    public final Property<Boolean> fireworksShowRockets = putProperty(Property.booleanProperty("fireworks_show_rockets", true, "fancymenu.decoration_overlays.fireworks.show_rockets"));

    final FireworksShow show = new FireworksShow();
    private final ParticleLayer layer = new ParticleLayer("fireworks");
    private int scale = 1;
    private boolean showRockets = true;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {

        this.fireworksShowRockets.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.ROCKET_LAUNCH)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.show_rockets.desc")));

        this.amount.addEntry(menu, this);

        this.size.addEntry(menu, this);

        this.fireworksExplosionSize.buildContextMenuEntryAndAddTo(menu, this)
                .setIcon(MaterialIcons.EXPLOSION)
                .setTooltipSupplier((menu1, entry) -> UITooltip.of(Component.translatable("fancymenu.decoration_overlays.fireworks.explosion_size.desc")));

    }

    @Override
    protected void onResize(int width, int height) {
        this.show.clear();
    }

    @Override
    protected void update(float deltaSeconds) {
        int newScale = this.size.get();
        if (newScale != this.scale) {
            // The show runs in art pixels, so a new size starts it over on the new grid
            this.scale = newScale;
            this.show.clear();
        }
        this.showRockets = this.fireworksShowRockets.tryGetNonNullElse(true);
        int artWidth = Math.max(1, this.getWidth() / this.scale);
        int artHeight = Math.max(1, this.getHeight() / this.scale);
        float widthFactor = PixelMath.clamp(artWidth / FireworksShow.STAGE_WIDTH, MIN_WIDTH_FACTOR, MAX_WIDTH_FACTOR);
        float launchRate = FireworksShow.LAUNCHES_PER_SECOND * this.amount.getFactor() * widthFactor;
        float burstScale = PixelMath.clamp(this.fireworksExplosionSize.getFloat(), MIN_EXPLOSION_SIZE, MAX_EXPLOSION_SIZE);
        this.show.update(deltaSeconds, this.random, artWidth, artHeight, launchRate, burstScale, this.showRockets);
    }

    @Override
    protected void drawFront(@NotNull PixelGraphics graphics) {
        int n = this.scale;
        FireworksShow show = this.show;
        ParticleLayer layer = this.layer;
        // A burning star draws up to two short streaks and a head, a few quads each
        layer.begin(graphics, show.sparkCount + show.rocketCount + 6 * show.starCount + show.flashCount);
        if (this.showRockets) {
            for (int i = 0; i < show.sparkCount; i++) {
                FireworksShow.Spark spark = show.sparks[i];
                int heat = Math.min(3, PixelMath.floor(spark.life / spark.maxLife * 4.0F));
                FireworksArt.dot(layer, PixelMath.floor(spark.x), PixelMath.floor(spark.y), FireworksArt.HEAT_ARGB[heat], n);
            }
            for (int i = 0; i < show.rocketCount; i++) {
                FireworksShow.Rocket rocket = show.rockets[i];
                layer.sprite(FireworksArt.ROCKET, FireworksArt.ROCKET_PALETTE, PixelMath.floor(rocket.x) * n, PixelMath.floor(rocket.y) * n, n);
            }
        }
        float time = this.getTime();
        for (int i = 0; i < show.starCount; i++) this.drawStar(layer, show.stars[i], time, n);
        for (int i = 0; i < show.flashCount; i++) {
            FireworksShow.Flash flash = show.flashes[i];
            int x = PixelMath.floor(flash.x);
            int y = PixelMath.floor(flash.y);
            if (flash.age < FireworksShow.FLASH_DISC_TIME) {
                layer.sprite(FireworksArt.FLASH_DISC, FireworksArt.FLASH_DISC_PALETTE, (x - 3) * n, (y - 3) * n, n);
            } else {
                layer.sprite(FireworksArt.FLASH_RING, FireworksArt.FLASH_RING_PALETTES[flash.ramp], (x - 7) * n, (y - 7) * n, n);
            }
        }
        layer.end();
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        this.layer.release();
    }

    /**
     * Draws a star by how far it burned: a streak back along its flight with a hotter inner part and a head pixel, three times stepping
     * down the ramp, then a lone ember that blinks, or a sparkle for glitter stars.
     */
    private void drawStar(@NotNull ParticleLayer layer, @NotNull FireworksShow.Star star, float time, int n) {
        int[] ramp = FireworksArt.RAMPS_ARGB[star.ramp];
        float burned = star.life / star.maxLife;
        int headX = PixelMath.floor(star.x);
        int headY = PixelMath.floor(star.y);
        boolean blink = (PixelMath.floor((time + star.phase) * 12.0F) & 1) == 1;
        if (burned >= 0.8F) {
            if (star.glitter && blink) {
                layer.sprite(FireworksArt.SPARKLE, FireworksArt.SPARKLE_PALETTES[star.ramp], (headX - 1) * n, (headY - 1) * n, n);
            } else {
                FireworksArt.dot(layer, headX, headY, (star.glitter || blink) ? ramp[2] : ramp[3], n);
            }
            return;
        }
        int stage = (burned < 0.15F) ? 0 : ((burned < 0.55F) ? 1 : 2);
        int lineEndX = Math.round(star.x);
        int lineEndY = Math.round(star.y);
        int tailX = Math.round(star.x - star.vx * star.trail);
        int tailY = Math.round(star.y - star.vy * star.trail);
        int innerX = Math.round(star.x - star.vx * star.trail * INNER_TRAIL);
        int innerY = Math.round(star.y - star.vy * star.trail * INNER_TRAIL);
        FireworksArt.line(layer, tailX, tailY, lineEndX, lineEndY, ramp[stage + 1], n);
        FireworksArt.line(layer, innerX, innerY, lineEndX, lineEndY, ramp[stage], n);
        FireworksArt.dot(layer, headX, headY, (stage == 0) ? FireworksArt.WHITE : ramp[stage - 1], n);
    }

}
