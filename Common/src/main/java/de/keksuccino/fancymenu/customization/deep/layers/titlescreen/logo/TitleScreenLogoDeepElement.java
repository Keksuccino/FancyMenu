package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.logo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;

public class TitleScreenLogoDeepElement extends AbstractDeepElement {

    private static final ResourceLocation MINECRAFT_LOGO = new ResourceLocation("textures/gui/title/minecraft.png");
    private static final ResourceLocation MINECRAFT_EDITION = new ResourceLocation("textures/gui/title/edition.png");

    private final boolean showEasterEgg = (double) RandomSource.create().nextFloat() < 1.0E-4D;

    public TitleScreenLogoDeepElement(DeepElementBuilder<?, ?, ?> builder) {
        super(builder);
    }

    @Override
    public void render(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {

        if (!this.shouldRender()) return;

        this.renderLogo(pose);

    }

    public void renderLogo(PoseStack pose) {

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, MINECRAFT_LOGO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.opacity);

        this.baseX = (getScreenWidth() / 2) - 137;
        this.baseY = 30;
        this.width = 155 + 119;
        this.height = 52;

        if (this.showEasterEgg) {
            blitOutlineBlack(this.getX(), this.getY(), (i1, i2) -> {
                blit(pose, i1, i2, 0, 0, 99, 44);
                blit(pose, i1 + 99, i2, 129, 0, 27, 44);
                blit(pose, i1 + 99 + 26, i2, 126, 0, 3, 44);
                blit(pose, i1 + 99 + 26 + 3, i2, 99, 0, 26, 44);
                blit(pose, i1 + 155, i2, 0, 45, 155, 44);
            });
        } else {
            blitOutlineBlack(this.getX(), this.getY(), (i1, i2) -> {
                blit(pose, i1, i2, 0, 0, 155, 44);
                blit(pose, i1 + 155, i2, 0, 45, 155, 44);
            });
        }
        RenderSystem.setShaderTexture(0, MINECRAFT_EDITION);
        blit(pose, this.getX() + 88, this.getY() + 37, 0.0F, 0.0F, 98, 14, 128, 16);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    }

}