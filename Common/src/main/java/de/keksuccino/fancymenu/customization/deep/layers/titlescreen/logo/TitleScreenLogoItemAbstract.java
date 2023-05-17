package de.keksuccino.fancymenu.customization.deep.layers.titlescreen.logo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.deep.DeepElementBuilder;
import de.keksuccino.fancymenu.customization.deep.AbstractDeepElement;
import de.keksuccino.fancymenu.properties.PropertyContainer;
import de.keksuccino.konkrete.rendering.RenderUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

public class TitleScreenLogoItemAbstract extends AbstractDeepElement {

    private static final ResourceLocation MINECRAFT_LOGO = new ResourceLocation("textures/gui/title/minecraft.png");
    private static final ResourceLocation MINECRAFT_EDITION = new ResourceLocation("textures/gui/title/edition.png");

    public TitleScreenLogoItemAbstract(DeepElementBuilder parentElement, PropertyContainer item) {
        super(parentElement, item);
    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        int j = menu.width / 2 - 137;

        this.baseX = j;
        this.baseY = 30;
        this.width = 155 + 119;
        this.height = 52;

        RenderSystem.enableBlend();

        RenderUtils.bindTexture(MINECRAFT_LOGO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, j + 0, 30, 0, 0, 155, 44);
        blit(matrix, j + 155, 30, 0, 45, 155, 44);

        RenderUtils.bindTexture(MINECRAFT_EDITION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, j + 88, 67, 0.0F, 0.0F, 98, 14, 128, 16);

    }

}