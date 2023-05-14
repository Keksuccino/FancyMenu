package de.keksuccino.fancymenu.customization.background.backgrounds.slideshow;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SlideshowMenuBackground extends MenuBackground {

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public String slideshowName;

    protected String lastSlideshowName;
    protected ExternalTextureSlideshowRenderer slideshow;

    public SlideshowMenuBackground(MenuBackgroundBuilder<SlideshowMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.slideshowName != null) {
            if ((this.lastSlideshowName == null) || !this.lastSlideshowName.equals(this.slideshowName)) {
                this.slideshow = SlideshowHandler.getSlideshow(this.slideshowName);
            }
            this.lastSlideshowName = this.slideshowName;
        } else {
            this.slideshow = null;
        }

        if (this.slideshow != null) {

            if (!this.slideshow.isReady()) {
                this.slideshow.prepareSlideshow();
            }

            int imageWidth = this.slideshow.getImageWidth();
            int imageHeight = this.slideshow.getImageHeight();

            if (!this.keepBackgroundAspectRatio) {
                this.slideshow.x = 0;
                this.slideshow.y = 0;
                this.slideshow.width = getScreenWidth();
                this.slideshow.height = getScreenHeight();
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
                this.slideshow.width = size[0] + 1;
                this.slideshow.height = size[1] + 1;
                this.slideshow.x = x;
                this.slideshow.y = y;
            }

            this.slideshow.slideshowOpacity = this.opacity;

            this.slideshow.render(pose);

            this.slideshow.slideshowOpacity = 1.0F;

        } else {
            RenderSystem.enableBlend();
            RenderUtils.bindTexture(MISSING);
            blit(pose, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}
