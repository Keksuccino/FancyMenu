package de.keksuccino.fancymenu.util.rendering.ui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;

public class MinecraftLogoRenderer {

    public static final MinecraftLogoRenderer DEFAULT_INSTANCE = new MinecraftLogoRenderer(false);

    public static final Identifier MINECRAFT_LOGO = Identifier.withDefaultNamespace("textures/gui/title/minecraft.png");
    public static final Identifier EASTER_EGG_LOGO = Identifier.withDefaultNamespace("textures/gui/title/minceraft.png");
    public static final Identifier MINECRAFT_EDITION = Identifier.withDefaultNamespace("textures/gui/title/edition.png");
    public static final int LOGO_WIDTH = 256;
    public static final int LOGO_HEIGHT = 44;
    private static final int LOGO_TEXTURE_WIDTH = 256;
    private static final int LOGO_TEXTURE_HEIGHT = 64;
    private static final int EDITION_WIDTH = 128;
    private static final int EDITION_HEIGHT = 14;
    private static final int EDITION_TEXTURE_WIDTH = 128;
    private static final int EDITION_TEXTURE_HEIGHT = 16;
    public static final int DEFAULT_HEIGHT_OFFSET = 30;
    private static final int EDITION_LOGO_OVERLAP = 7;
    private final boolean showEasterEgg = (double)RandomSource.create().nextFloat() < 1.0E-4;
    private final boolean keepLogoThroughFade;

    public MinecraftLogoRenderer(boolean keepLogoThroughFade) {
        this.keepLogoThroughFade = keepLogoThroughFade;
    }

    /**
     * Returns the width of the main logo.
     * @return The width of the logo in pixels.
     */
    public int getWidth() {
        return LOGO_WIDTH;
    }

    /**
     * Returns the total height of the logo including the edition logo,
     * accounting for the overlap between them.
     * @return The total height of the logo in pixels.
     */
    public int getHeight() {
        return LOGO_HEIGHT + EDITION_HEIGHT - EDITION_LOGO_OVERLAP;
    }

    /**
     * Renders the logo with the default height offset.
     * @param guiGraphics The graphics context to render with.
     * @param screenWidth The width of the screen.
     * @param transparency The transparency value for the logo.
     */
    public void renderLogo(GuiGraphics guiGraphics, int screenWidth, float transparency) {
        this.renderLogo(guiGraphics, screenWidth, transparency, DEFAULT_HEIGHT_OFFSET);
    }

    /**
     * Renders the logo at the specified height.
     * @param guiGraphics The graphics context to render with.
     * @param screenWidth The width of the screen.
     * @param transparency The transparency value for the logo.
     * @param height The vertical position of the logo from the top of the screen.
     */
    public void renderLogo(GuiGraphics guiGraphics, int screenWidth, float transparency, int height) {
        int i = screenWidth / 2 - 128;
        float f = this.keepLogoThroughFade ? 1.0F : transparency;
        int j = ARGB.white(f);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.showEasterEgg ? EASTER_EGG_LOGO : MINECRAFT_LOGO, i, height, 0.0F, 0.0F, 256, 44, 256, 64, j);
        int k = screenWidth / 2 - 64;
        int l = height + 44 - 7;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MINECRAFT_EDITION, k, l, 0.0F, 0.0F, 128, 14, 128, 16, j);
    }

    /**
     * Renders the logo with its top-left corner at the specified position.
     * @param guiGraphics The graphics context to render with.
     * @param x The x-coordinate for the top-left corner of the logo.
     * @param y The y-coordinate for the top-left corner of the logo.
     * @param transparency The transparency value for the logo.
     */
    public void renderLogoAtPosition(GuiGraphics guiGraphics, int x, int y, float transparency) {
        float f = this.keepLogoThroughFade ? 1.0F : transparency;
        int color = ARGB.white(f);

        // Render main logo with top-left corner at (x,y)
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, this.showEasterEgg ? EASTER_EGG_LOGO : MINECRAFT_LOGO,
                x, y, 0.0F, 0.0F, LOGO_WIDTH, LOGO_HEIGHT, LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT, color);

        // Calculate edition logo position
        // The edition logo is centered horizontally relative to the main logo
        int editionX = x + (LOGO_WIDTH - EDITION_WIDTH) / 2;
        int editionY = y + LOGO_HEIGHT - EDITION_LOGO_OVERLAP;

        // Render edition logo
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MINECRAFT_EDITION,
                editionX, editionY, 0.0F, 0.0F, EDITION_WIDTH, EDITION_HEIGHT, EDITION_TEXTURE_WIDTH, EDITION_TEXTURE_HEIGHT, color);
    }

}