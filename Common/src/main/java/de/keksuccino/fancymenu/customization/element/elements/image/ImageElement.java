package de.keksuccino.fancymenu.customization.element.elements.image;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.ElementBuilder;
import de.keksuccino.fancymenu.rendering.AspectRatio;
import de.keksuccino.fancymenu.rendering.texture.ExternalTextureHandler;
import de.keksuccino.konkrete.rendering.RenderUtils;
import de.keksuccino.konkrete.rendering.animation.ExternalGifAnimationRenderer;
import de.keksuccino.konkrete.resources.ExternalTextureResourceLocation;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ImageElement extends AbstractElement {

    private static final ResourceLocation MISSING = new ResourceLocation("missing_texture");

    public String path;

    protected ExternalTextureResourceLocation texture;
    protected ExternalGifAnimationRenderer gif;
    protected String lastPath;
    protected int originalWidth = 10;
    protected int originalHeight = 10;

    public ImageElement(@NotNull ElementBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (this.shouldRender()) {

            this.updateResources();

            int x = this.getX();
            int y = this.getY();

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);

            if (this.gif != null) {
                int w = this.gif.getWidth();
                int h = this.gif.getHeight();
                int x2 = this.gif.getPosX();
                int y2 = this.gif.getPosY();
                this.gif.setPosX(x);
                this.gif.setPosY(y);
                this.gif.setWidth(this.getWidth());
                this.gif.setHeight(this.getHeight());
                this.gif.setOpacity(this.opacity);
                this.gif.render(pose);
                this.gif.setPosX(x2);
                this.gif.setPosY(y2);
                this.gif.setWidth(w);
                this.gif.setHeight(h);
            } else if (this.texture != null) {
                RenderUtils.bindTexture(this.texture.getResourceLocation());
                blit(pose, x, y, 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            } else {
                RenderUtils.bindTexture(MISSING);
                blit(pose, x, y, 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();

        }

    }

    protected void updateResources() {
        if ((this.path != null) && ((this.lastPath == null) || (!this.lastPath.equals(this.path)))) {
            File f = new File(ScreenCustomization.getAbsoluteGameDirectoryPath(this.path));
            if (f.exists() && f.isFile() && (f.getName().endsWith(".png") || f.getName().endsWith(".jpg") || f.getName().endsWith(".jpeg") || f.getName().endsWith(".gif"))) {
                if (f.getName().endsWith(".gif")) {
                    this.gif = ExternalTextureHandler.INSTANCE.getGif(this.path);
                    if (this.gif != null) {
                        this.originalWidth = this.gif.getWidth();
                        this.originalHeight = this.gif.getHeight();
                    }
                } else {
                    this.texture = ExternalTextureHandler.INSTANCE.getTexture(this.path);
                    if (this.texture != null) {
                        this.originalWidth = this.texture.getWidth();
                        this.originalHeight = this.texture.getHeight();
                    }
                }
            }
            if (isEditor()) {
                this.restoreAspectRatio();
            }
        }
        this.lastPath = this.path;
    }

    public void restoreAspectRatio() {
        AspectRatio ratio = new AspectRatio(this.originalWidth, this.originalHeight);
        this.setWidth(ratio.getAspectRatioWidth(this.getHeight()));
    }

}
