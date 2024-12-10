package de.keksuccino.fancymenu.customization.background.backgrounds.slideshow;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.customization.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SlideshowMenuBackground extends MenuBackground {

    private static final ResourceLocation MISSING = ITexture.MISSING_TEXTURE_LOCATION;

    public String slideshowName;

    protected String lastSlideshowName;
    protected ExternalTextureSlideshowRenderer slideshow;

    public SlideshowMenuBackground(MenuBackgroundBuilder<SlideshowMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

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
                int[] size = ratio.getAspectRatioSizeByMinimumSize(getScreenWidth(), getScreenHeight());
                int x = 0;
                if (size[0] > getScreenWidth()) {
                    x = -((size[0] - getScreenWidth()) / 2);
                }
                int y = 0;
                if (size[1] > getScreenHeight()) {
                    y = -((size[1] - getScreenHeight()) / 2);
                }
                this.slideshow.width = size[0];
                this.slideshow.height = size[1];
                this.slideshow.x = x;
                this.slideshow.y = y;
            }

            this.slideshow.slideshowOpacity = this.opacity;

            this.slideshow.render(graphics);

            this.slideshow.slideshowOpacity = 1.0F;

        } else {
            RenderSystem.enableBlend();
            graphics.blit(RenderType::guiTextured, MISSING, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        }

    }

}
