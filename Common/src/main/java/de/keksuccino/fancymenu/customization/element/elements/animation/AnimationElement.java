package de.keksuccino.fancymenu.customization.element.elements.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class AnimationElement extends AbstractElement {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public String animationName;

    protected IAnimationRenderer animation = null;
    protected String lastName;
    protected int originalWidth;
    protected int originalHeight;

    public AnimationElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        this.updateResources();

        if ((this.animation != null) && this.animation.isReady()) {

            int cachedX = this.animation.getPosX();
            int cachedY = this.animation.getPosY();
            int cachedWidth = this.animation.getWidth();
            int cachedHeight = this.animation.getHeight();

            this.animation.setOpacity(this.opacity);
            this.animation.setPosX(this.getX());
            this.animation.setPosY(this.getY());
            this.animation.setWidth(this.getWidth());
            this.animation.setHeight(this.getHeight());

            this.animation.render(pose);

            this.animation.setPosX(cachedX);
            this.animation.setPosY(cachedY);
            this.animation.setWidth(cachedWidth);
            this.animation.setHeight(cachedHeight);
            this.animation.setOpacity(1.0F);

        } else {
            RenderUtils.bindTexture(MISSING);
            blit(pose, this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
        }

    }

    protected void updateResources() {

        if ((this.animationName != null) && ((this.lastName == null) || !this.lastName.equals(this.animationName))) {
            if (AnimationHandler.animationExists(this.animationName)) {
                this.animation = AnimationHandler.getAnimation(this.animationName);
                if (this.animation != null) {
                    this.originalWidth = this.animation.getWidth();
                    this.originalHeight = this.animation.getHeight();
                }
            }
            if (isEditor()) {
                this.restoreAspectRatio();
            }
        }
        this.lastName = this.animationName;

    }

    public void restoreAspectRatio() {
        AspectRatio ratio = new AspectRatio(this.originalWidth, this.originalHeight);
        this.setWidth(ratio.getAspectRatioWidth(this.getHeight()));
    }

}
