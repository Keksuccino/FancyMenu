package de.keksuccino.fancymenu.customization.element.elements.slideshow;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.customization.slideshow.ExternalTextureSlideshowRenderer;
import de.keksuccino.fancymenu.customization.slideshow.SlideshowHandler;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class SlideshowElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public String slideshowName;

    protected ExternalTextureSlideshowRenderer slideshow = null;
    protected String lastName;
    protected int originalWidth;
    protected int originalHeight;

    public SlideshowElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
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
            graphics.blit(MISSING, this.getAbsoluteX(), this.getAbsoluteY(), 0.0F, 0.0F, this.getAbsoluteWidth(), this.getAbsoluteHeight(), this.getAbsoluteWidth(), this.getAbsoluteHeight());
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

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
