package de.keksuccino.fancymenu.customization.background.backgrounds.animation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.IAnimationRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class AnimationMenuBackground extends MenuBackground {

    //TODO Choose Animation Screen in neuem GUI Stil
    // - links list mit allen verfügbaren animationen
    // - rechts oben Vorschau von ausgewählter Animation (animation ist NICHT gemutet)
    // - rechts unten Buttons für Cancel und Done (Done deaktiviert wenn keine Animation ausgewählt)

    //TODO Choose Animation Screen in Animation Background Builder nutzen

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public String animationName;

    protected String lastAnimationName;
    protected IAnimationRenderer animation;

    public AnimationMenuBackground(MenuBackgroundBuilder<AnimationMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.animationName != null) {
            if ((this.lastAnimationName == null) || (this.lastAnimationName != this.animationName)) {
                //TODO get animation
                this.animation = ;
            }
            this.lastAnimationName = this.animationName;
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
                int[] size = ratio.getAspectRatioSize(getScreenWidth(), getScreenHeight());
                int x = Math.max(0, size[0] - getScreenWidth());
                if (x > 0) {
                    x = x / 2;
                }
                int y = Math.max(0, size[1] - getScreenHeight());
                if (y > 0) {
                    y = y / 2;
                }
                this.animation.setWidth(size[0] + 1);
                this.animation.setHeight(size[1] + 1);
                this.animation.setPosX(x);
                this.animation.setPosY(y);
            }

            this.animation.setOpacity(this.opacity);

            this.animation.render(pose);

            this.animation.setWidth(imageWidth);
            this.animation.setHeight(imageHeight);
            this.animation.setPosX(cachedX);
            this.animation.setPosY(cachedY);
            this.animation.setStretchImageToScreensize(cachedStretchToScreenSize);
            this.animation.setOpacity(1.0F);

        } else {
            RenderSystem.enableBlend();
            RenderUtils.bindTexture(MISSING);
            blit(pose, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}
