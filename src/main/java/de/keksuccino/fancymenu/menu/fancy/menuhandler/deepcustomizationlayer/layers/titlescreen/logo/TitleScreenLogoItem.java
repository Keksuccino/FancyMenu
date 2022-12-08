package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.logo;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeVersion;

import java.io.IOException;

public class TitleScreenLogoItem extends DeepCustomizationItem {

    private static final ResourceLocation MINECRAFT_LOGO = new ResourceLocation("textures/gui/title/minecraft.png");
    private static final ResourceLocation MINECRAFT_EDITION = new ResourceLocation("textures/gui/title/edition.png");

    public TitleScreenLogoItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(GuiScreen menu) throws IOException {

        int j = menu.width / 2 - 137;

        this.posX = j;
        this.posY = 30;
        this.setWidth(155 + 119);
        this.setHeight(52);

        GlStateManager.enableBlend();

        Minecraft.getMinecraft().getTextureManager().bindTexture(MINECRAFT_LOGO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        menu.drawTexturedModalRect(j + 0, 30, 0, 0, 155, 44);
        menu.drawTexturedModalRect(j + 155, 30, 0, 45, 155, 44);

        String version = ForgeVersion.mcVersion;
        if (version.equals("1.12.2")) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(MINECRAFT_EDITION);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            menu.drawModalRectWithCustomSizedTexture(j + 88, 67, 0.0F, 0.0F, 98, 14, 128.0F, 16.0F);
        }

    }

}