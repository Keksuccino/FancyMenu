package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.shootingstars;

import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A night sky behind the elements: stars that twinkle in steps from a dim dot up to a 7 px sparkle in four soft tints, a meteor every
 * few seconds and now and then a shower of them. Everything is back content.<br><br>
 *
 * The stars only change six times a second, so they live in one canvas that gets redrawn at those steps. Meteors move every frame
 * and are a few short runs of pixels each.
 */
public class ShootingStarsDecorationOverlay extends PixelDecorationOverlay<ShootingStarsDecorationOverlay> {

    /** The mockup stage the star density and the star band are made for. */
    private static final float STAGE_WIDTH = 480.0F;
    private static final float STAGE_HEIGHT = 270.0F;
    private static final int STAGE_STAR_COUNT = 90;
    /** Lowest row stars sit on, on the stage. */
    private static final float STAGE_STAR_BAND = 172.0F;
    /** Stars reach this far out of their center pixel (the 7 px sparkle). */
    private static final int STAR_REACH = 3;
    private static final int TWINKLE_STEPS_PER_SECOND = 6;

    private static final PixelSprite STAR_PLUS = PixelSprite.of(
            ".1.",
            "121",
            ".1.");
    private static final PixelSprite STAR_SPARKLE = PixelSprite.of(
            "...1...",
            "...1...",
            "...2...",
            "1123211",
            "...2...",
            "...1...",
            "...1...");
    /** Bright and dim color of the four star tints: white, warm, cold and rose. */
    private static final int[][] STAR_TINTS = {{0xFFFFFF, 0xAEC2F0}, {0xFFF6D8, 0xD8C48A}, {0xD8ECFF, 0x8FB0E0}, {0xFFE0F0, 0xD0A0C0}};
    private static final PixelPalette[] STAR_PALETTES = new PixelPalette[STAR_TINTS.length];
    private static final PixelPalette[] STAR_PALETTES_BRIGHT = new PixelPalette[STAR_TINTS.length];
    private static final int DIM_STAR_ALPHA = 150;

    /** The meteor trail cools from a white head over pale and deep blue into a broken-up tail. */
    private static final int TRAIL_HEAD = 0xFFFFFFFF;
    private static final int TRAIL_PALE = 0xFFCFE2FF;
    private static final int TRAIL_BLUE = PixelMath.withAlpha(0x7FA6E8, 200);
    private static final int TRAIL_TAIL = PixelMath.withAlpha(0x4868B8, 130);
    private static final int SPARK_WHITE = PixelMath.withAlpha(0xFFFFFF, 200);
    private static final int SPARK_BLUE = PixelMath.withAlpha(0xCFE2FF, 200);

    static {
        for (int k = 0; k < STAR_TINTS.length; k++) {
            int[] tint = STAR_TINTS[k];
            STAR_PALETTES[k] = PixelPalette.builder().rgb('1', tint[1]).rgb('2', tint[0]).rgb('3', 0xFFFFFF).build();
            STAR_PALETTES_BRIGHT[k] = PixelPalette.builder().rgb('1', tint[0]).rgb('2', 0xFFFFFF).build();
        }
    }

    private final PixelCanvas starCanvas = new PixelCanvas("shooting_stars");
    private final MeteorShower meteorShower = new MeteorShower();
    private int starCount = 0;
    private int[] starX = new int[0];
    private int[] starY = new int[0];
    private int[] starTint = new int[0];
    /** 0 to 2: how much brighter than the rest a star twinkles */
    private int[] starBoost = new int[0];
    private float[] starPhase = new float[0];
    private float[] starSpeed = new float[0];
    private int twinkleStep = -1;

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {
    }

    @Override
    protected void onResize(int width, int height) {
        float area = (width / STAGE_WIDTH) * (height / STAGE_HEIGHT);
        int count = Math.max(1, Math.round(STAGE_STAR_COUNT * area));
        int band = Math.max(2, Math.round(STAGE_STAR_BAND * height / STAGE_HEIGHT));
        this.starCount = count;
        this.starX = new int[count];
        this.starY = new int[count];
        this.starTint = new int[count];
        this.starBoost = new int[count];
        this.starPhase = new float[count];
        this.starSpeed = new float[count];
        for (int i = 0; i < count; i++) {
            this.starX[i] = this.random.rangeInt(2, Math.max(2, width - 3));
            this.starY[i] = this.random.rangeInt(2, band);
            this.starTint[i] = this.random.rangeInt(0, STAR_TINTS.length - 1);
            this.starBoost[i] = this.random.chance(0.12F) ? 2 : (this.random.chance(0.4F) ? 1 : 0);
            this.starPhase[i] = this.random.range(0.0F, PixelMath.TAU);
            this.starSpeed[i] = this.random.range(0.6F, 2.2F);
        }
        this.starCanvas.resize(width, Math.min(height, band + STAR_REACH + 1));
        this.twinkleStep = -1;
    }

    @Override
    protected void update(float deltaSeconds) {
        this.meteorShower.update(deltaSeconds, this.random, this.getWidth(), this.getHeight());
        int step = (int)(this.getTime() * TWINKLE_STEPS_PER_SECOND);
        if (step != this.twinkleStep) {
            this.twinkleStep = step;
            this.paintStars(step / (float)TWINKLE_STEPS_PER_SECOND);
        }
    }

    /**
     * Repaints all stars at the twinkle step that starts at {@code stepTime}.
     */
    private void paintStars(float stepTime) {
        PixelCanvas canvas = this.starCanvas;
        canvas.fillRect(0, 0, canvas.width(), canvas.height(), 0);
        for (int i = 0; i < this.starCount; i++) {
            float wave = (float)Math.sin(stepTime * this.starSpeed[i] + this.starPhase[i]);
            int base = (wave > 0.85F) ? 3 : ((wave > 0.4F) ? 2 : ((wave > -0.3F) ? 1 : 0));
            paintStar(canvas, this.starX[i], this.starY[i], this.starTint[i], base + this.starBoost[i] - 1);
        }
    }

    /**
     * Paints one star at a brightness level: up to 0 a dim dot, 1 a bright dot, 2 a small plus, 3 a bright plus, 4 the 7 px sparkle.
     */
    private static void paintStar(@NotNull PixelCanvas canvas, int x, int y, int tint, int level) {
        if (level <= 0) {
            canvas.blend(x, y, PixelMath.withAlpha(STAR_TINTS[tint][1], DIM_STAR_ALPHA));
        } else if (level == 1) {
            canvas.blend(x, y, PixelMath.opaque(STAR_TINTS[tint][0]));
        } else if (level == 2) {
            canvas.stamp(STAR_PLUS, STAR_PALETTES[tint], x - 1, y - 1, false, false, 1);
        } else if (level == 3) {
            canvas.stamp(STAR_PLUS, STAR_PALETTES_BRIGHT[tint], x - 1, y - 1, false, false, 1);
        } else {
            canvas.stamp(STAR_SPARKLE, STAR_PALETTES[tint], x - STAR_REACH, y - STAR_REACH, false, false, 1);
        }
    }

    @Override
    protected void drawBack(@NotNull PixelGraphics graphics) {
        graphics.canvas(this.starCanvas, 0, 0);
        for (int i = 0; i < this.meteorShower.count; i++) {
            this.drawMeteor(graphics, this.meteorShower.meteors[i]);
        }
    }

    /**
     * Draws the trail from the head backwards. Neighboring pixels of the same color in a row become one quad,
     * and the tail beyond 16 pixels keeps only every second pixel, so it looks like it breaks up.
     */
    private void drawMeteor(@NotNull PixelGraphics graphics, @NotNull MeteorShower.Meteor meteor) {
        int length = meteor.currentLength();
        int height = this.getHeight();
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        int runY = Integer.MIN_VALUE;
        int runLeft = 0;
        int runRight = 0;
        int runColor = 0;
        for (int i = 0; i < length; i++) {
            if ((i >= 16) && ((i & 1) == 1)) continue;
            int x = PixelMath.floor(meteor.x - meteor.dirX * i);
            int y = PixelMath.floor(meteor.y - meteor.dirY * i);
            if (((x == lastX) && (y == lastY)) || (y < 0) || (y >= height)) continue;
            lastX = x;
            lastY = y;
            int color = trailColor(i);
            if ((y == runY) && (color == runColor) && ((x == runLeft - 1) || (x == runRight + 1))) {
                if (x < runLeft) runLeft = x;
                else runRight = x;
                continue;
            }
            if (runY != Integer.MIN_VALUE) graphics.rect(runLeft, runY, runRight - runLeft + 1, 1, runColor);
            runY = y;
            runLeft = x;
            runRight = x;
            runColor = color;
        }
        if (runY != Integer.MIN_VALUE) graphics.rect(runLeft, runY, runRight - runLeft + 1, 1, runColor);
        if (meteor.isBreakingUp()) {
            graphics.pixel(PixelMath.floor(meteor.x + 2.0F), PixelMath.floor(meteor.y - 1.0F), SPARK_WHITE);
            graphics.pixel(PixelMath.floor(meteor.x - 1.0F), PixelMath.floor(meteor.y + 2.0F), SPARK_BLUE);
        }
    }

    private static int trailColor(int index) {
        if (index < 3) return TRAIL_HEAD;
        if (index < 9) return TRAIL_PALE;
        if (index < 16) return TRAIL_BLUE;
        return TRAIL_TAIL;
    }

    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        super.onCloseScreen(closedScreen, newScreen);
        this.starCanvas.releaseTexture();
    }

}
