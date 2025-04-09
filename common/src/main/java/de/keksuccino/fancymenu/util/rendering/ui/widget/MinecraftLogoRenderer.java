package de.keksuccino.fancymenu.util.rendering.ui.widget;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * A utility class for rendering the Minecraft logo and edition texture at specified positions.
 * This class extracts the logo rendering logic from the TitleScreen class to make it reusable.
 */
public class MinecraftLogoRenderer {

    public static final MinecraftLogoRenderer DEFAULT_INSTANCE = new MinecraftLogoRenderer();

    private static final ResourceLocation MINECRAFT_LOGO = new ResourceLocation("textures/gui/title/minecraft.png");
    private static final ResourceLocation MINECRAFT_EDITION = new ResourceLocation("textures/gui/title/edition.png");
    
    private final boolean minceraftEasterEgg;
    
    /**
     * Creates a new LogoRenderer instance.
     * 
     * @param minceraftEasterEgg Whether to render the "Minceraft" easter egg variant of the logo
     */
    public MinecraftLogoRenderer(boolean minceraftEasterEgg) {
        this.minceraftEasterEgg = minceraftEasterEgg;
    }
    
    /**
     * Creates a new LogoRenderer instance with a random chance of enabling the "Minceraft" easter egg.
     * There's a 1 in 10,000 (0.01%) chance the easter egg will be enabled.
     */
    public MinecraftLogoRenderer() {
        this((double)RandomSource.create().nextFloat() < 1.0E-4);
    }

    /**
     * Renders both the Minecraft logo and edition texture at the specified position.
     * The edition texture is positioned relative to the logo.
     *
     * @param graphics The GuiGraphics instance for rendering
     * @param x The x position to render the logo at
     * @param y The y position to render the logo at
     * @param alpha The alpha value for rendering (0.0F to 1.0F)
     */
    public void render(GuiGraphics graphics, int x, int y, float alpha) {

        // Set up blending
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);

        // Render the logo
        renderLogo(graphics, x, y, alpha);

        // Render the edition texture (positioned relative to the logo)
        renderEdition(graphics, x + 88, y + 37, alpha);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }
    
    /**
     * Renders the Minecraft logo at the specified position.
     * 
     * @param graphics The GuiGraphics instance for rendering
     * @param x The x position to render the logo at
     * @param y The y position to render the logo at
     * @param alpha The alpha value for rendering (0.0F to 1.0F)
     */
    protected void renderLogo(GuiGraphics graphics, int x, int y, float alpha) {

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        
        if (this.minceraftEasterEgg) {
            this.blitOutlineBlack(graphics, x, y, (logoX, logoY) -> {
                graphics.blit(MINECRAFT_LOGO, logoX + 0, logoY, 0, 0, 99, 44);
                graphics.blit(MINECRAFT_LOGO, logoX + 99, logoY, 129, 0, 27, 44);
                graphics.blit(MINECRAFT_LOGO, logoX + 99 + 26, logoY, 126, 0, 3, 44);
                graphics.blit(MINECRAFT_LOGO, logoX + 99 + 26 + 3, logoY, 99, 0, 26, 44);
                graphics.blit(MINECRAFT_LOGO, logoX + 155, logoY, 0, 45, 155, 44);
            }, alpha);
        } else {
            this.blitOutlineBlack(graphics, x, y, (logoX, logoY) -> {
                graphics.blit(MINECRAFT_LOGO, logoX + 0, logoY, 0, 0, 155, 44);
                graphics.blit(MINECRAFT_LOGO, logoX + 155, logoY, 0, 45, 155, 44);
            }, alpha);
        }

    }
    
    /**
     * Renders the Minecraft edition texture at the specified position.
     * 
     * @param graphics The GuiGraphics instance for rendering
     * @param x The x position to render the edition texture at
     * @param y The y position to render the edition texture at
     * @param alpha The alpha value for rendering (0.0F to 1.0F)
     */
    protected void renderEdition(GuiGraphics graphics, int x, int y, float alpha) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        graphics.blit(MINECRAFT_EDITION, x, y, 0.0F, 0.0F, 98, 14, 128, 16);
    }
    
    /**
     * Draws the logo with a black outline.
     */
    protected void blitOutlineBlack(GuiGraphics graphics, int x, int y, DrawLogo drawLogo, float alpha) {
        // Use the proper blend function for the outline as implemented in GuiComponent
        RenderSystem.blendFuncSeparate(
            SourceFactor.ZERO, 
            DestFactor.ONE_MINUS_SRC_ALPHA, 
            SourceFactor.SRC_ALPHA, 
            DestFactor.ONE_MINUS_SRC_ALPHA
        );
        
        // Set color with proper alpha
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        
        // Draw the outlines at offset positions (only the 4 cardinal directions like in GuiComponent)
        drawLogo.draw(x + 1, y);
        drawLogo.draw(x - 1, y);
        drawLogo.draw(x, y + 1);
        drawLogo.draw(x, y - 1);
        
        // Reset blend function to normal
        RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
        
        // Draw the actual logo
        drawLogo.draw(x, y);
    }
    
    /**
     * Gets the total width of the Minecraft logo.
     * 
     * @return The width of the Minecraft logo in pixels
     */
    public int getLogoWidth() {
        return 274; // The actual width used in TitleScreen (155 + 119)
    }
    
    /**
     * Gets the total height of the Minecraft logo.
     * 
     * @return The height of the Minecraft logo in pixels
     */
    public int getLogoHeight() {
        return 44;
    }
    
    /**
     * Gets the width of the edition texture.
     * 
     * @return The width of the edition texture in pixels
     */
    public int getEditionWidth() {
        return 98;
    }
    
    /**
     * Gets the height of the edition texture.
     * 
     * @return The height of the edition texture in pixels
     */
    public int getEditionHeight() {
        return 14;
    }
    
    /**
     * Gets the total width of the combined logo and edition.
     * The width is determined by the Minecraft logo since it's wider than the edition texture.
     * 
     * @return The total width in pixels
     */
    public int getTotalWidth() {
        return getLogoWidth();
    }
    
    /**
     * Gets the total height of the combined logo and edition.
     * This includes the logo height and the edition texture positioned below it.
     * 
     * @return The total height in pixels
     */
    public int getTotalHeight() {
        // The edition texture is positioned at y + 37, which means it extends to y + 37 + 14 = y + 51
        // So the total height is 51 pixels from the top of the logo
        return 51;
    }

    /**
     * Functional interface for drawing the logo with outline.
     */
    @FunctionalInterface
    protected interface DrawLogo {
        void draw(int x, int y);
    }

}
