package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/**
 * Draws the many small sprites and rects of one overlay part (falling flakes, rain streaks, confetti), either straight as quads or
 * rasterized into a canvas that gets drawn as a single quad.<br><br>
 *
 * The game's GUI renderer files every quad into a layer by checking it against the quads submitted before it, so a frame with thousands
 * of quads gets slow quickly (a rain overlay with 4000 quads dropped to about 20 fps). A layer therefore switches to its canvas while
 * the part has more than {@value #CANVAS_ABOVE} quads and back to plain quads below {@value #DIRECT_BELOW}. Both ways give the same
 * picture: translucent pixels blend in the canvas in the order they get drawn, and the canvas blends over the screen like the quads would.
 * The canvas gets created on first use and follows the screen size; release it with {@link #release()} when the screen closes.<br><br>
 *
 * Use it like {@link PixelGraphics}: {@link #begin}, then sprites and rects in drawing order, then {@link #end()}. Render thread only.
 */
public final class ParticleLayer {

    static final int CANVAS_ABOVE = 600;
    static final int DIRECT_BELOW = 450;

    private final PixelCanvas canvas;
    @Nullable
    private PixelGraphics graphics = null;
    private boolean canvasMode = false;

    /**
     * @param name a short name of the canvas for debugging
     */
    public ParticleLayer(@NotNull String name) {
        this(new PixelCanvas(name));
    }

    ParticleLayer(@NotNull PixelCanvas canvas) {
        this.canvas = canvas;
    }

    /**
     * Starts drawing the part of this frame.
     *
     * @param expectedQuads about how many quads the part draws in this frame
     */
    public void begin(@NotNull PixelGraphics graphics, int expectedQuads) {
        this.graphics = graphics;
        if (this.canvasMode ? (expectedQuads < DIRECT_BELOW) : (expectedQuads > CANVAS_ABOVE)) this.canvasMode = !this.canvasMode;
        if (this.canvasMode) {
            this.canvas.resize(graphics.width(), graphics.height());
            this.canvas.clear();
        }
    }

    /**
     * Draws a sprite at a whole-number scale, like {@link PixelGraphics#sprite(PixelSprite, PixelPalette, int, int, boolean, boolean, int)}.
     */
    public void sprite(@NotNull PixelSprite sprite, @NotNull PixelPalette palette, int x, int y, int scale) {
        if (this.canvasMode) {
            this.canvas.stamp(sprite, palette, x, y, false, false, scale);
        } else {
            this.graphics().sprite(sprite, palette, x, y, false, false, scale);
        }
    }

    /**
     * Draws a filled rectangle in an ARGB color, like {@link PixelGraphics#rect}.
     */
    public void rect(int x, int y, int width, int height, int argb) {
        if (this.canvasMode) {
            this.canvas.blendRect(x, y, width, height, argb);
        } else {
            this.graphics().rect(x, y, width, height, argb);
        }
    }

    /**
     * Finishes the part: draws the canvas if the part went into it.
     */
    public void end() {
        if (this.canvasMode) this.graphics().canvas(this.canvas, 0, 0);
        this.graphics = null;
    }

    /**
     * Whether the part currently goes into the canvas.
     */
    public boolean isCanvasMode() {
        return this.canvasMode;
    }

    /**
     * Releases the canvas texture and pixels. The layer creates them again when it needs the canvas next time.
     */
    public void release() {
        this.canvas.close();
    }

    @NotNull
    private PixelGraphics graphics() {
        return Objects.requireNonNull(this.graphics, "ParticleLayer can only draw between begin() and end()");
    }

}
