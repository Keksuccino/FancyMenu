package de.keksuccino.fancymenu.customization.background.backgrounds.panorama;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.panorama.ExternalTexturePanoramaRenderer;
import de.keksuccino.fancymenu.customization.panorama.PanoramaHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class PanoramaMenuBackground extends MenuBackground {

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public String panoramaName;

    protected String lastPanoramaName;
    protected ExternalTexturePanoramaRenderer panorama;

    public PanoramaMenuBackground(MenuBackgroundBuilder<PanoramaMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.panoramaName != null) {
            if ((this.lastPanoramaName == null) || !this.lastPanoramaName.equals(this.panoramaName)) {
                this.panorama = PanoramaHandler.getPanorama(this.panoramaName);
            }
            this.lastPanoramaName = this.panoramaName;
        } else {
            this.panorama = null;
        }

        if (this.panorama != null) {
            if (!this.panorama.isReady()) {
                this.panorama.preparePanorama();
            }
            this.panorama.opacity = this.opacity;
            this.panorama.render(pose);
            this.panorama.opacity = 1.0F;
        } else {
            RenderSystem.enableBlend();
            RenderUtils.bindTexture(MISSING);
            blit(pose, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}
