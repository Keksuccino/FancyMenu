package de.keksuccino.fancymenu.customization.background.backgrounds.animation;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.animation.AdvancedAnimation;
import de.keksuccino.fancymenu.customization.animation.AnimationHandler;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.util.rendering.AspectRatio;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AnimationMenuBackground extends MenuBackground {

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public String animationName;
    public boolean restartOnMenuLoad = false;

    protected String lastAnimationName;
    protected IAnimationRenderer animation;
    protected boolean restarted = false;

    public AnimationMenuBackground(MenuBackgroundBuilder<AnimationMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.animationName != null) {
            if ((this.lastAnimationName == null) || !this.lastAnimationName.equals(this.animationName)) {
                this.animation = AnimationHandler.getAnimation(this.animationName);
                if (this.restartOnMenuLoad && !this.restarted && (this.animation != null)) {
                    this.animation.resetAnimation();
                    if (this.animation instanceof AdvancedAnimation a) {
                        a.stopAudio();
                        a.resetAudio();
                    }
                    this.restarted = true;
                }
            }
            this.lastAnimationName = this.animationName;
        } else {
            this.animation = null;
        }

        if ((this.animation != null) && this.animation.isReady()) {

            boolean cachedStretchToScreenSize = this.animation.isStretchedToStreensize();
            int imageWidth = this.animation.getWidth();
            int imageHeight = this.animation.getHeight();
            int cachedX = this.animation.getPosX();
            int cachedY = this.animation.getPosY();

            if (!this.keepBackgroundAspectRatio) {
                this.animation.setStretchImageToScreensize(true);
            } else {
                AspectRatio ratio = new AspectRatio(imageWidth, imageHeight);
                int[] size = ratio.getAspectRatioSizeByMinimumSize(getScreenWidth(), getScreenHeight());
                this.animation.setWidth(size[0] + 1);
                this.animation.setHeight(size[1] + 1);
                int x = 0;
                if (size[0] > getScreenWidth()) {
                    x = -((size[0] - getScreenWidth()) / 2);
                }
                int y = 0;
                if (size[1] > getScreenHeight()) {
                    y = -((size[1] - getScreenHeight()) / 2);
                }
                this.animation.setPosX(x);
                this.animation.setPosY(y);
            }

            this.animation.setOpacity(this.opacity);

            this.animation.render(graphics);

            this.animation.setWidth(imageWidth);
            this.animation.setHeight(imageHeight);
            this.animation.setPosX(cachedX);
            this.animation.setPosY(cachedY);
            this.animation.setStretchImageToScreensize(cachedStretchToScreenSize);
            this.animation.setOpacity(1.0F);

        } else {
            RenderSystem.enableBlend();
            graphics.blit(MISSING, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}
