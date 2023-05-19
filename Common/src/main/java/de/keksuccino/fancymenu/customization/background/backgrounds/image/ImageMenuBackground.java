package de.keksuccino.fancymenu.customization.background.backgrounds.image;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.MenuBackgroundBuilder;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.fancymenu.resources.texture.LocalTexture;
import de.keksuccino.fancymenu.resources.texture.TextureHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ImageMenuBackground extends MenuBackground {

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public String imagePath;
    public boolean slideLeftRight = false;

    protected double slidePos = 0.0D;
    protected boolean slideMoveBack = false;
    protected boolean slideStop = false;
    protected int slideTick = 0;

    public ImageMenuBackground(MenuBackgroundBuilder<ImageMenuBackground> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        int imageWidth = 10;
        int imageHeight = 10;
        ResourceLocation r = null;
        if (this.imagePath != null) {
            LocalTexture external = TextureHandler.INSTANCE.getTexture(this.imagePath);
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

        if (this.slideLeftRight) {
            AspectRatio ratio = new AspectRatio(imageWidth, imageHeight);
            int w = ratio.getAspectRatioWidth(getScreenHeight());
            //Check if background should move to the left or the right side
            if ((slidePos + (w - getScreenWidth())) <= 0) {
                slideMoveBack = true;
            }
            if (slidePos >= 0) {
                slideMoveBack = false;
            }
            //Fix pos after resizing
            if (slidePos + (w - getScreenWidth()) < 0) {
                slidePos = -(w - getScreenWidth());
            }
            if (slidePos > 0) {
                slidePos = 0;
            }
            if (!slideStop) {
                if (slideTick >= 1) {
                    slideTick = 0;
                    if (slideMoveBack) {
                        slidePos = slidePos + 0.5;
                    } else {
                        slidePos = slidePos - 0.5;
                    }

                    if (slidePos + (w - getScreenWidth()) == 0) {
                        slideStop = true;
                    }
                    if (slidePos == 0) {
                        slideStop = true;
                    }
                } else {
                    slideTick++;
                }
            } else {
                if (slideTick >= 300) {
                    slideStop = false;
                    slideTick = 0;
                } else {
                    slideTick++;
                }
            }
            if (w <= getScreenWidth()) {
                blit(pose, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
            } else {
                RenderUtils.doubleBlit(slidePos, 0, 0.0F, 0.0F, w,getScreenHeight());
            }
        } else if (this.keepBackgroundAspectRatio) {
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
        } else {
            blit(pose, 0, 0, 0.0F, 0.0F, getScreenWidth(), getScreenHeight(), getScreenWidth(), getScreenHeight());
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}
