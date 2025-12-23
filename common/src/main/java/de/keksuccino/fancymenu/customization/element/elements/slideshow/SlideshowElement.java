package de.keksuccino.fancymenu.customization.element.elements.slideshow;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.fancymenu.util.resource.resources.texture.ITexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class SlideshowElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Identifier MISSING = ITexture.MISSING_TEXTURE_LOCATION;

    public String slideshowName;

    protected ExternalTextureSlideshowRenderer slideshow = null;
    protected String lastName;
    protected int originalWidth;
    protected int originalHeight;

    public SlideshowElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
        this.allowDepthTestManipulation = true;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        this.updateResources();

        if ((this.slideshow != null) && this.slideshow.isReady()) {

            int cachedX = this.slideshow.x;
            int cachedY = this.slideshow.y;
            int cachedWidth = this.slideshow.width;
            int cachedHeight = this.slideshow.height;

            this.slideshow.slideshowOpacity = this.opacity;
            this.slideshow.x = this.getAbsoluteX();
            this.slideshow.y = this.getAbsoluteY();
            this.slideshow.width = this.getAbsoluteWidth();
            this.slideshow.height = this.getAbsoluteHeight();

            this.slideshow.render(graphics);

            this.slideshow.x = cachedX;
            this.slideshow.y = cachedY;
            this.slideshow.width = cachedWidth;
            this.slideshow.height = cachedHeight;
            this.slideshow.slideshowOpacity = 1.0F;

        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, MISSING, this.getAbsoluteX(), this.getAbsoluteY(), 0.0F, 0.0F, this.getAbsoluteWidth(), this.getAbsoluteHeight(), this.getAbsoluteWidth(), this.getAbsoluteHeight());
        }

    }

    protected void updateResources() {

        if ((this.slideshowName != null) && ((this.lastName == null) || !this.lastName.equals(this.slideshowName))) {
            if (SlideshowHandler.slideshowExists(this.slideshowName)) {
                this.slideshow = SlideshowHandler.getSlideshow(this.slideshowName);
                if (this.slideshow != null) {
                    this.originalWidth = this.slideshow.getImageWidth();
                    this.originalHeight = this.slideshow.getImageHeight();
                }
            }
            if (isEditor()) {
                this.restoreAspectRatio();
            }
        }
        this.lastName = this.slideshowName;

    }

    public void restoreAspectRatio() {
        AspectRatio ratio = new AspectRatio(this.originalWidth, this.originalHeight);
        this.baseWidth = ratio.getAspectRatioWidth(this.getAbsoluteHeight());
    }

}
