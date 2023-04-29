package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.branding;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationItem;
import de.keksuccino.fancymenu.platform.Services;
import de.keksuccino.konkrete.properties.PropertiesSection;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

import java.io.IOException;

public class TitleScreenBrandingItem extends DeepCustomizationItem {

    protected int lastWidth = 0;
    protected int lastHeight = 0;

    public TitleScreenBrandingItem(DeepCustomizationElement parentElement, PropertiesSection item) {
        super(parentElement, item);
    }

    @Override
    public void render(PoseStack matrix, Screen menu) throws IOException {

        RenderSystem.enableBlend();

        lastWidth = 0;
        lastHeight = 0;

        Font font = Minecraft.getInstance().font;
        if (Services.PLATFORM.getPlatformName().equals("forge")) {
            Services.COMPAT.renderCustomTitleScreenBrandingLines(matrix, font, menu, lastWidth, lastHeight, (width) -> lastWidth = width, (height) -> lastHeight = height);
        } else {
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
        }

        this.width = lastWidth;
        this.height = lastHeight;
        this.posX = 2;
        this.posY = menu.height - 2 - lastHeight;

    }

}