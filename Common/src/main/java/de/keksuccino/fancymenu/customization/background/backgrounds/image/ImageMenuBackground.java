package de.keksuccino.fancymenu.customization.background.backgrounds.image;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.fancymenu.rendering.texture.ExternalTextureHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ImageMenuBackground extends MenuBackground {

    //TODO add "slide background left to right"

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public String imagePath;

    public ImageMenuBackground(MenuBackgroundBuilder<ImageMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        int imageWidth = 10;
        int imageHeight = 10;
        ResourceLocation r = null;
        if (this.imagePath != null) {
            ExternalTextureResourceLocation external = ExternalTextureHandler.INSTANCE.getTexture(this.imagePath);
            if (external != null) {
                imageWidth = external.getWidth();
                imageHeight = external.getHeight();
                r = external.getResourceLocation();
            }
        }
        if (r == null) {
            r = MISSING;
        }
        RenderSystem.enableBlend();
        RenderUtils.bindTexture(r);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);
        if (!this.keepBackgroundAspectRatio) {
            blit(pose, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        } else {
            AspectRatio ratio = new AspectRatio(imageWidth, imageHeight);
            int[] size = ratio.getAspectRatioSize(getScreenWidth(), getScreenHeight());
            int x = Math.max(0, size[0] - getScreenWidth());
            if (x > 0) {
                x = x / 2;
            }
            int y = Math.max(0, size[1] - getScreenHeight());
            if (y > 0) {
                y = y / 2;
            }
            blit(pose, -x, -y, 0.0F, 0.0F, size[0], size[1], size[0], size[1]);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}
