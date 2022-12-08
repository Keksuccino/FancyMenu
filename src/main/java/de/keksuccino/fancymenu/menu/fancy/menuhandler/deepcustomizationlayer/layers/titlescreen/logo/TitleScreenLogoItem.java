package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.logo;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;

public class TitleScreenLogoItem extends DeepCustomizationItem {

    private static final ResourceLocation MINECRAFT_LOGO = new ResourceLocation("textures/gui/title/minecraft.png");
    private static final ResourceLocation MINECRAFT_EDITION = new ResourceLocation("textures/gui/title/edition.png");

    public TitleScreenLogoItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(MatrixStack matrix, Screen menu) throws IOException {

        int j = menu.width / 2 - 137;

        this.posX = j;
        this.posY = 30;
        this.setWidth(155 + 119);
        this.setHeight(52);

        RenderSystem.enableBlend();

        Minecraft.getInstance().textureManager.bind(MINECRAFT_LOGO);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, j + 0, 30, 0, 0, 155, 44);
        blit(matrix, j + 155, 30, 0, 45, 155, 44);

        Minecraft.getInstance().textureManager.bind(MINECRAFT_EDITION);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        blit(matrix, j + 88, 67, 0.0F, 0.0F, 98, 14, 128, 16);

    }

}