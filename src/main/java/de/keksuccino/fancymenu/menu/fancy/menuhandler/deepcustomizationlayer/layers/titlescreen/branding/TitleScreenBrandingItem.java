package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.branding;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

import java.io.IOException;

public class TitleScreenBrandingItem extends DeepCustomizationItem {

    public TitleScreenBrandingItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        RenderSystem.enableBlend();

        Font font = Minecraft.getInstance().font;
        String branding = "Minecraft " + SharedConstants.getCurrentVersion().getName();
        if (Minecraft.getInstance().isDemo()) {
            branding = branding + " Demo";
        } else {
            branding = branding + ("release".equalsIgnoreCase(Minecraft.getInstance().getVersionType()) ? "" : "/" + Minecraft.getInstance().getVersionType());
        }
        if (Minecraft.checkModStatus().shouldReportAsModified()) {
            branding = branding + I18n.get("menu.modded");
        }
        drawString(matrix, font, branding, 2, menu.height - 10, -1);

        this.setWidth(font.width(branding));
        this.setHeight(10);
        this.posX = 2;
        this.posY = menu.height - 2 - this.getHeight();

    }

}