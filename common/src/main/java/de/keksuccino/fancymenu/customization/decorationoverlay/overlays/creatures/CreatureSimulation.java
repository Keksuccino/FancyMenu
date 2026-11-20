package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.creatures;

import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelGraphics;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelPalette;
import de.keksuccino.fancymenu.util.rendering.overlay.pixel.PixelSprite;
import org.jetbrains.annotations.NotNull;

/**
 * The simulation and drawing of one creature overlay, the port of one {@code <x>New()} function of the mockup.
 * Its overlay ({@link CreatureDecorationOverlay}) owns it and calls it with the frame's state in the {@link CreatureScene}.
 * Simulations only know the scene, so tests can run them without a game.<br><br>
 *
 * {@link #update(float)} runs once per frame, the draw methods only draw. Which render pass draws which part is up to the
 * Layers setting of the overlay: the back part is what the mockup draws {@code behind}, the element part is everything attached
 * to a real element (creatures resting on or hanging from one, bubbles caught under one), the front part is everything else.
 * Creatures resting on the floor are front content: the floor is no element, so they follow the Layers setting like anything
 * else drawn across the screen (the mockup also marks them with {@code el}).
 */
public abstract class CreatureSimulation {

    @NotNull
    protected final CreatureScene scene;

    protected CreatureSimulation(@NotNull CreatureScene scene) {
        this.scene = scene;
    }

    /**
     * Advances the simulation by one frame.
     *
     * @param deltaSeconds time since the last update, from 0 to 0.1 seconds
     */
    public abstract void update(float deltaSeconds);

    public void drawBack(@NotNull PixelGraphics graphics) {
    }

    public void drawElement(@NotNull PixelGraphics graphics) {
    }

    public void drawFront(@NotNull PixelGraphics graphics) {
    }

    /**
     * Gets every click on the screen. The click always goes on to the widgets.
     */
    public void onClick(float x, float y, boolean primaryButton) {
    }

    /**
     * Draws a sprite scaled by a whole number, with its top left corner at the given position. Skips sprites that are completely
     * off the screen, which creatures flying in or out often are.
     */
    protected final void sprite(@NotNull PixelGraphics graphics, @NotNull PixelSprite sprite, @NotNull PixelPalette palette, int x, int y, boolean flipX, int scale) {
        if ((x >= this.scene.width()) || (y >= this.scene.height())) return;
        if ((x + sprite.width() * scale <= 0) || (y + sprite.height() * scale <= 0)) return;
        graphics.sprite(sprite, palette, x, y, flipX, false, scale);
    }

}
