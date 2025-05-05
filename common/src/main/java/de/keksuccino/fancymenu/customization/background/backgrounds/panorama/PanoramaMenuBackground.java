package de.keksuccino.fancymenu.customization.background.backgrounds.panorama;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.panorama.LocalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class PanoramaMenuBackground extends MenuBackground {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ResourceLocation MISSING = TextureManager.INTENTIONAL_MISSING_TEXTURE;

    public String panoramaName;
    protected String lastPanoramaName;
    protected LocalTexturePanoramaRenderer panorama;

    public PanoramaMenuBackground(MenuBackgroundBuilder<PanoramaMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.panoramaName != null) {
            if ((this.lastPanoramaName == null) || !this.lastPanoramaName.equals(this.panoramaName)) {
                this.panorama = PanoramaHandler.getPanorama(this.panoramaName);
            }
            this.lastPanoramaName = this.panoramaName;
        } else {
            this.panorama = null;
        }

        if (this.panorama != null) {
            this.panorama.opacity = this.opacity;
            graphics.flush();
            this.panorama.render(graphics, mouseX, mouseY, partial);
            graphics.flush();
            this.panorama.opacity = 1.0F;
        } else {
            graphics.blit(RenderType::guiTextured, MISSING, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        }

    }

}
