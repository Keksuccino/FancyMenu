package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public class MinecraftLogoRenderer {

    public static final MinecraftLogoRenderer DEFAULT_INSTANCE = new MinecraftLogoRenderer(false);

    public static final ResourceLocation MINECRAFT_LOGO = ResourceLocation.withDefaultNamespace("textures/gui/title/minecraft.png");
    public static final ResourceLocation EASTER_EGG_LOGO = ResourceLocation.withDefaultNamespace("textures/gui/title/minceraft.png");
    public static final ResourceLocation MINECRAFT_EDITION = ResourceLocation.withDefaultNamespace("textures/gui/title/edition.png");
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
        DrawableColor.WHITE.setAsShaderColor(guiGraphics, f);
        guiGraphics.blit(this.showEasterEgg ? EASTER_EGG_LOGO : MINECRAFT_LOGO, i, height, 0.0F, 0.0F, 256, 44, 256, 64);
        int k = screenWidth / 2 - 64;
        int l = height + 44 - 7;
        guiGraphics.blit(MINECRAFT_EDITION, k, l, 0.0F, 0.0F, 128, 14, 128, 16);
        RenderingUtils.resetShaderColor(guiGraphics);
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

        DrawableColor.WHITE.setAsShaderColor(guiGraphics, f);

        // Render main logo with top-left corner at (x,y)
        guiGraphics.blit(this.showEasterEgg ? EASTER_EGG_LOGO : MINECRAFT_LOGO,
                x, y, 0.0F, 0.0F, LOGO_WIDTH, LOGO_HEIGHT, LOGO_TEXTURE_WIDTH, LOGO_TEXTURE_HEIGHT);

        // Calculate edition logo position
        // The edition logo is centered horizontally relative to the main logo
        int editionX = x + (LOGO_WIDTH - EDITION_WIDTH) / 2;
        int editionY = y + LOGO_HEIGHT - EDITION_LOGO_OVERLAP;

        // Render edition logo
        guiGraphics.blit(MINECRAFT_EDITION,
                editionX, editionY, 0.0F, 0.0F, EDITION_WIDTH, EDITION_HEIGHT, EDITION_TEXTURE_WIDTH, EDITION_TEXTURE_HEIGHT);

        RenderingUtils.resetShaderColor(guiGraphics);

    }

}