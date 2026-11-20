package de.keksuccino.fancymenu.customization.decorationoverlay;

import com.mojang.blaze3d.platform.InputConstants;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.util.rendering.overlay.CollisionAreaBounds;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelCanvas;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPointer;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelRandom;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSurfaces;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Base of the pixel-art decoration overlays. It connects the render passes of {@link AbstractDecorationOverlay} with the pixel toolkit
 * ({@code de.keksuccino.fancymenu.util.rendering.overlay.pixel}), so overlays only write their simulation and their drawing:
 *
 * <ul>
 *     <li>{@link #update(float)} advances the simulation, exactly once per frame. The screen size, pointer, surfaces and screen canvases are up to date by then.</li>
 *     <li>{@link #drawBack}, {@link #drawElement} and {@link #drawFront} draw the three parts with a {@link PixelGraphics}. The Layers setting
 *     decides in which pass each of them runs, so they must only draw, never advance anything.</li>
 *     <li>{@link #onResize} runs before the first update and whenever the screen size changes (window resize, GUI scale change).</li>
 *     <li>{@link #onClick} gets every click on the screen without taking it away from the widgets.</li>
 * </ul>
 *
 * Overlays drawn with this class never call version-specific code, so they port to other Minecraft versions unchanged.
 * 1 art pixel is 1 GUI pixel.
 *
 * @param <O> the overlay class itself
 */
public abstract class PixelDecorationOverlay<O extends PixelDecorationOverlay<?>> extends AbstractDecorationOverlay<O> {

    /** Where the mouse pointer is. Updated right before {@link #update(float)}. */
    protected final PixelPointer pointer = new PixelPointer();
    /** A random number generator of this overlay. */
    protected final PixelRandom random = PixelRandom.create();
    private final PixelGraphics pixelGraphics = new PixelGraphics();
    private final List<PixelCanvas> screenCanvases = new ArrayList<>();
    private final List<PixelSurfaces<?>> surfaces = new ArrayList<>();
    @NotNull
    private List<CollisionAreaBounds> elementBoxes = List.of();
    private int width = 0;
    private int height = 0;
    private float time = 0.0F;
    private int lastFrameQuadCount = 0;

    /**
     * Advances the simulation by one frame. Runs exactly once per frame, before any part of that frame gets drawn.
     *
     * @param deltaSeconds time since the last update, from 0 to 0.1 seconds
     */
    protected abstract void update(float deltaSeconds);

    /**
     * Runs before the first {@link #update(float)} and whenever the screen size changed, with the new size in GUI pixels.
     * Screen canvases have the new size and are empty by then.
     */
    protected void onResize(int width, int height) {
    }

    /**
     * Draws the screen-wide content meant to be behind the elements.
     */
    protected void drawBack(@NotNull PixelGraphics graphics) {
    }

    /**
     * Draws the decorations attached to elements (snow caps, frost or moss on a button, a bird sitting on one).
     * They always draw in front of the elements, whatever the Layers setting is.
     */
    protected void drawElement(@NotNull PixelGraphics graphics) {
    }

    /**
     * Draws the screen-wide content meant to be in front of the elements.
     */
    protected void drawFront(@NotNull PixelGraphics graphics) {
    }

    /**
     * Gets called for every click on the screen while the overlay is shown, before the widgets get it. The click always goes on to them.
     *
     * @param primaryButton true for the left mouse button
     */
    protected void onClick(float x, float y, boolean primaryButton) {
    }

    /**
     * Creates a canvas that always has the size of the screen and gets released when the screen closes.
     * Call it once, for example in a field initializer, and keep the canvas.
     */
    @NotNull
    protected final PixelCanvas createScreenCanvas(@NotNull String name) {
        PixelCanvas canvas = new PixelCanvas(name);
        canvas.resize(this.width, this.height);
        this.screenCanvases.add(canvas);
        return canvas;
    }

    /**
     * Creates surfaces (element boxes plus the floor) that stay in sync with the elements and the screen size.
     * They are up to date at every {@link #update(float)}. Call it once and keep them.
     *
     * @param stateFactory creates the overlay's state of a new surface (a snow cap, a moss cushion), NULL for none
     */
    @NotNull
    protected final <S> PixelSurfaces<S> createSurfaces(@Nullable Function<PixelSurfaces.Surface<S>, S> stateFactory) {
        PixelSurfaces<S> created = new PixelSurfaces<>(stateFactory);
        created.update(this.elementBoxes, this.width, this.height);
        this.surfaces.add(created);
        return created;
    }

    /**
     * The boxes of the elements overlays can collide with, in GUI pixels. Empty in the layout editor.
     */
    @NotNull
    protected final List<CollisionAreaBounds> getElementBoxes() {
        return this.elementBoxes;
    }

    /**
     * Screen width in GUI pixels.
     */
    public final int getWidth() {
        return this.width;
    }

    /**
     * Screen height in GUI pixels.
     */
    public final int getHeight() {
        return this.height;
    }

    /**
     * Seconds the overlay has been running, the sum of all update deltas.
     */
    public final float getTime() {
        return this.time;
    }

    /**
     * Quads the overlay drew in the last complete frame, all parts together. For checks of the dev-only overlay preview.
     */
    @ApiStatus.Internal
    public int getLastFrameQuadCount() {
        return this.lastFrameQuadCount;
    }

    @Override
    protected final void updateFrame(float deltaSeconds, int mouseX, int mouseY) {
        this.lastFrameQuadCount = this.pixelGraphics.quadCount();
        this.pixelGraphics.resetQuadCount();
        int screenWidth = getScreenWidth();
        int screenHeight = getScreenHeight();
        boolean resized = (screenWidth != this.width) || (screenHeight != this.height);
        this.width = screenWidth;
        this.height = screenHeight;
        if (resized) {
            for (PixelCanvas canvas : this.screenCanvases) canvas.resize(screenWidth, screenHeight);
        }
        for (PixelSurfaces<?> s : this.surfaces) s.update(this.elementBoxes, screenWidth, screenHeight);
        this.pointer.update(mouseX, mouseY, screenWidth, screenHeight);
        if (resized) this.onResize(screenWidth, screenHeight);
        this.time += deltaSeconds;
        this.update(deltaSeconds);
    }

    @Override
    protected final void renderBackPart(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        try {
            this.drawBack(this.pixelGraphics.begin(graphics, this.width, this.height));
        } finally {
            this.pixelGraphics.end();
        }
    }

    @Override
    protected final void renderElementPart(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        try {
            this.drawElement(this.pixelGraphics.begin(graphics, this.width, this.height));
        } finally {
            this.pixelGraphics.end();
        }
    }

    @Override
    protected final void renderFrontPart(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        try {
            this.drawFront(this.pixelGraphics.begin(graphics, this.width, this.height));
        } finally {
            this.pixelGraphics.end();
        }
    }

    @Override
    protected final void onPointerClicked(double mouseX, double mouseY, int button) {
        this.onClick((float)mouseX, (float)mouseY, button == InputConstants.MOUSE_BUTTON_LEFT);
    }

    /**
     * Subclasses overriding this have to call super, so the element boxes stay up to date.
     */
    @Override
    public void onScreenInitializedOrResized(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {
        this.elementBoxes = collectCollisionBoxes(screen, elements);
    }

    /**
     * Subclasses overriding this have to call super, so the element boxes stay up to date.
     */
    @Override
    public void tick(@NotNull Screen screen, @NotNull List<AbstractElement> elements) {
        this.elementBoxes = collectCollisionBoxes(screen, elements);
    }

    /**
     * Subclasses overriding this have to call super, so the screen canvases release their textures.
     */
    @Override
    public void onCloseScreen(@Nullable Screen closedScreen, @Nullable Screen newScreen) {
        for (PixelCanvas canvas : this.screenCanvases) canvas.releaseTexture();
    }

}
