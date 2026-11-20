package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.clouds;

import de.keksuccino.fancymenu.customization.decorationoverlay.PixelDecorationOverlay;
import de.keksuccino.fancymenu.customization.layout.editor.LayoutEditorScreen;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelMath;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import de.keksuccino.fancymenu.util.rendering.ui.contextmenu.v2.ContextMenu;
import org.jetbrains.annotations.NotNull;

/**
 * Cumulus clouds in three depth layers that drift at three speeds behind the elements, with thin wisps high above them.
 * Far clouds are smaller, slower and paler. Everything is back content.<br><br>
 *
 * Every cloud is one sprite that gets built once from a fixed seed, so the sky looks the same every time and drawing it is one quad per cloud.
 */
public class CloudsDecorationOverlay extends PixelDecorationOverlay<CloudsDecorationOverlay> {

    private static final float STAGE_WIDTH = 480.0F;
    private static final float STAGE_HEIGHT = 270.0F;
    /** The clouds look the same every time, so they come from a fixed seed. */
    private static final int CLOUD_SEED = 77;
    /** Pixels per second of the wisps and of the far, middle and near clouds. */
    private static final float[] LAYER_SPEEDS = {1.4F, 2.6F, 5.0F, 9.0F};
    /**
     * Per layer, from the wisps to the near clouds: clouds on the stage, smallest and largest width, height per width (0 for wisps),
     * tones, highest and lowest stage row.
     */
    private static final float[][] LAYERS = {
            {5, 36, 72, 0.0F, 0, 8, 74},
            {6, 24, 40, 0.3F, 3, 74, 114},
            {4, 52, 84, 0.34F, 4, 42, 92},
            {3, 104, 150, 0.35F, 5, -10, 30}
    };
    /** Five tones from the sunlit top to the shaded base. Farther layers drop the extremes and lean toward the sky color. */
    private static final int[] CLOUD_TONES = {0xFFFFFF, 0xEDF3FB, 0xD2E1F3, 0xB3C8E7, 0x97B0DA};
    private static final int CLOUD_SKY = 0x8DB9E6;
    private static final PixelPalette[] LAYER_PALETTES = {
            PixelPalette.builder().rgb('a', 0xFFFFFF, 150).rgb('b', 0xFFFFFF, 84).build(),
            tonePalette(new int[]{1, 2, 3}, 0.4F),
            tonePalette(new int[]{0, 1, 2, 3}, 0.17F),
            tonePalette(new int[]{0, 1, 2, 3, 4}, 0.0F)
    };

    private int cloudCount = 0;
    private int[] cloudLayer = new int[0];
    private PixelSprite[] cloudSprite = new PixelSprite[0];
    private float[] cloudX = new float[0];
    private int[] cloudY = new int[0];

    @Override
    protected void initConfigMenu(@NotNull ContextMenu menu, @NotNull LayoutEditorScreen editor) {
    }

    @NotNull
    private static PixelPalette tonePalette(@NotNull int[] tones, float towardSky) {
        PixelPalette.Builder builder = PixelPalette.builder();
        for (int i = 0; i < tones.length; i++) {
            builder.argb((char)('a' + i), PixelMath.mix(PixelMath.opaque(CLOUD_TONES[tones[i]]), PixelMath.opaque(CLOUD_SKY), towardSky));
        }
        return builder.build();
    }

    /**
     * Builds the clouds for the screen size: wider screens get more clouds, and the layers keep their share of the screen height.
     */
    @Override
    protected void onResize(int width, int height) {
        PixelRandom shapes = new PixelRandom(CLOUD_SEED);
        float scaleX = width / STAGE_WIDTH;
        float scaleY = height / STAGE_HEIGHT;
        int total = 0;
        for (float[] layer : LAYERS) total += layerCount(layer, scaleX);
        this.cloudLayer = new int[total];
        this.cloudSprite = new PixelSprite[total];
        this.cloudX = new float[total];
        this.cloudY = new int[total];
        int n = 0;
        for (int l = 0; l < LAYERS.length; l++) {
            float[] layer = LAYERS[l];
            int count = layerCount(layer, scaleX);
            for (int i = 0; i < count; i++) {
                int w = Math.round(layer[1] + shapes.next() * (layer[2] - layer[1]));
                PixelSprite sprite;
                if (l == 0) {
                    sprite = CloudShapes.wisp(shapes, w);
                } else {
                    int h = Math.round(w * (layer[3] + shapes.next() * 0.08F));
                    sprite = CloudShapes.cumulus(shapes, w, h, (int)layer[4]);
                }
                this.cloudLayer[n] = l;
                this.cloudSprite[n] = sprite;
                // Spread evenly over the width, with a little jitter, so the sky never starts with a gap
                this.cloudX[n] = (i + shapes.next() * 0.8F) / count * (width + w) - w;
                this.cloudY[n] = Math.round((layer[5] + shapes.next() * (layer[6] - layer[5])) * scaleY);
                n++;
            }
        }
        this.cloudCount = total;
    }

    private static int layerCount(@NotNull float[] layer, float scaleX) {
        return Math.max(1, Math.round(layer[0] * scaleX));
    }

    @Override
    protected void update(float deltaSeconds) {
        int width = this.getWidth();
        for (int i = 0; i < this.cloudCount; i++) {
            this.cloudX[i] += LAYER_SPEEDS[this.cloudLayer[i]] * deltaSeconds;
            if (this.cloudX[i] > width + 4) this.cloudX[i] = -this.cloudSprite[i].width() - this.random.range(4.0F, 70.0F);
        }
    }

    @Override
    protected void drawBack(@NotNull PixelGraphics graphics) {
        for (int i = 0; i < this.cloudCount; i++) {
            graphics.sprite(this.cloudSprite[i], LAYER_PALETTES[this.cloudLayer[i]], PixelMath.floor(this.cloudX[i]), this.cloudY[i]);
        }
    }

}
