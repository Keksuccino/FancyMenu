package de.keksuccino.fancymenu.util.rendering.overlay.pixel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import java.util.Objects;

/**
 * What pixel-art overlays draw with, the in-game counterpart of the mockup's {@code Buf}. All coordinates are whole GUI pixels
 * and all colors are ARGB, so translucent pixels are just colors with a lower alpha byte.<br><br>
 *
 * Every call is one quad: a sprite is one textured quad from the shared {@link PixelAtlas}, a canvas is one quad, a pixel or rect is one
 * filled quad. Use pixels, rects and lines for small things only. Large areas made of many pixels belong into a {@link PixelCanvas}.
 * Together with {@link PixelTextures}, this is the only class of the toolkit that calls version-specific rendering code.<br><br>
 *
 * The game draws GUI quads right away on this Minecraft version, so the quads wait in the batches of the game's buffer source instead:
 * quads that follow each other with the same texture (or all rects in a row) get drawn with one draw call, and switching to another
 * texture draws the batch before, so everything still ends up in the order it was drawn in. {@link #end()} draws what is left.<br><br>
 *
 * Overlays get an instance from their draw methods and must not keep it. Everything here runs on the render thread.
 */
public final class PixelGraphics {

    @Nullable
    private GuiGraphics graphics;
    private int width;
    private int height;
    private int quadCount;

    /**
     * Points this instance at the graphics of the current pass. Only for the overlay base class.
     */
    @ApiStatus.Internal
    @NotNull
    public PixelGraphics begin(@NotNull GuiGraphics graphics, int width, int height) {
        this.graphics = graphics;
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Draws the quads that still wait in their batches. Only for the overlay base class.
     */
    @ApiStatus.Internal
    public void end() {
        if (this.graphics != null) this.graphics.flush();
        this.graphics = null;
    }

    /**
     * The game's graphics, for the rare cases the toolkit has no method for. Calls on it are version-specific.
     * Draws the quads that still wait in their batches first, so what gets drawn with it ends up in front of them.
     */
    @NotNull
    public GuiGraphics graphics() {
        GuiGraphics current = this.current();
        current.flush();
        return current;
    }

    @NotNull
    private GuiGraphics current() {
        return Objects.requireNonNull(this.graphics, "PixelGraphics can only be used inside of a draw method");
    }

    /**
     * Width of the screen in GUI pixels.
     */
    public int width() {
        return this.width;
    }

    /**
     * Height of the screen in GUI pixels.
     */
    public int height() {
        return this.height;
    }

    /**
     * Number of quads drawn through this instance since {@link #resetQuadCount()}.
     */
    public int quadCount() {
        return this.quadCount;
    }

    @ApiStatus.Internal
    public void resetQuadCount() {
        this.quadCount = 0;
    }

    /**
     * Draws one pixel.
     */
    public void pixel(int x, int y, int argb) {
        this.rect(x, y, 1, 1, argb);
    }

    /**
     * Draws a filled rectangle. Fully transparent colors and empty sizes draw nothing.
     */
    public void rect(int x, int y, int width, int height, int argb) {
        if (((argb >>> 24) == 0) || (width <= 0) || (height <= 0)) return;
        GuiGraphics current = this.current();
        Matrix4f pose = current.pose().last().pose();
        float left = x;
        float top = y;
        float right = x + width;
        float bottom = y + height;
        // Same render type and corner order as GuiGraphics#fill, which draws every rect on its own instead
        VertexConsumer buffer = current.bufferSource().getBuffer(RenderType.gui());
        buffer.addVertex(pose, right, bottom, 0.0F).setColor(argb);
        buffer.addVertex(pose, right, top, 0.0F).setColor(argb);
        buffer.addVertex(pose, left, top, 0.0F).setColor(argb);
        buffer.addVertex(pose, left, bottom, 0.0F).setColor(argb);
        this.quadCount++;
    }

    /**
     * Draws a one-pixel line from one pixel to another, both included, with the same steps as the mockup's {@code line}.
     * Each straight run of pixels is one quad, so flat and steep lines stay cheap.
     */
    public void line(int x0, int y0, int x1, int y1, int argb) {
        if ((argb >>> 24) == 0) return;
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = (x0 < x1) ? 1 : -1;
        int sy = (y0 < y1) ? 1 : -1;
        int error = dx + dy;
        boolean flat = dx >= -dy;
        int runStartX = x0;
        int runStartY = y0;
        while (true) {
            if ((x0 == x1) && (y0 == y1)) break;
            int doubled = 2 * error;
            int nextX = x0;
            int nextY = y0;
            if (doubled >= dy) {
                error += dy;
                nextX += sx;
            }
            if (doubled <= dx) {
                error += dx;
                nextY += sy;
            }
            // A run ends where the line steps across its main direction
            if (flat ? (nextY != y0) : (nextX != x0)) {
                this.lineRun(runStartX, runStartY, x0, y0, argb);
                runStartX = nextX;
                runStartY = nextY;
            }
            x0 = nextX;
            y0 = nextY;
        }
        this.lineRun(runStartX, runStartY, x0, y0, argb);
    }

    private void lineRun(int xa, int ya, int xb, int yb, int argb) {
        int left = Math.min(xa, xb);
        int top = Math.min(ya, yb);
        this.rect(left, top, Math.abs(xb - xa) + 1, Math.abs(yb - ya) + 1, argb);
    }

    /**
     * Draws a sprite in a palette at its natural size, with its top left corner at the given position.
     */
    public void sprite(@NotNull PixelSprite sprite, @NotNull PixelPalette palette, int x, int y) {
        this.sprite(sprite, palette, x, y, false, false, 1, -1);
    }

    /**
     * Draws a sprite in a palette, mirrored and scaled by a whole number (1 to 3 for the overlays' Size option), so every art pixel
     * stays a square of {@code scale} by {@code scale} GUI pixels.
     */
    public void sprite(@NotNull PixelSprite sprite, @NotNull PixelPalette palette, int x, int y, boolean flipX, boolean flipY, int scale) {
        this.sprite(sprite, palette, x, y, flipX, flipY, scale, -1);
    }

    /**
     * Draws a sprite like {@link #sprite(PixelSprite, PixelPalette, int, int, boolean, boolean, int)}, with all of its colors multiplied
     * by an ARGB color. {@code -1} (opaque white) changes nothing, {@code PixelMath.withAlpha(0xFFFFFF, a)} fades the whole sprite.
     */
    public void sprite(@NotNull PixelSprite sprite, @NotNull PixelPalette palette, int x, int y, boolean flipX, boolean flipY, int scale, int tint) {
        if (((tint >>> 24) == 0) || (scale < 1)) return;
        PixelAtlas.Entry entry = PixelAtlas.get().resolve(sprite, palette);
        if ((entry == null) || (entry.page == null)) return;
        int w = sprite.width();
        int h = sprite.height();
        entry.page.texture.draw(this.current(), x, y, w * scale, h * scale, entry.u, entry.v, w, h, flipX, flipY, tint);
        this.quadCount++;
    }

    /**
     * Draws a canvas at its size with its top left corner at the given position, uploading its changes first.
     */
    public void canvas(@NotNull PixelCanvas canvas, int x, int y) {
        // The upload happens right away, so quads of the canvas that still wait in their batch have to get drawn before it
        if (canvas.isDirty()) this.current().flush();
        PixelTexture texture = canvas.flush();
        if (texture == null) return;
        texture.draw(this.current(), x, y, canvas.width(), canvas.height(), 0, 0, canvas.width(), canvas.height(), false, false, -1);
        this.quadCount++;
    }

}
