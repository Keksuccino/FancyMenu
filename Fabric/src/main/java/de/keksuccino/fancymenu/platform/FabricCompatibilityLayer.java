package de.keksuccino.fancymenu.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.TitleScreenLayer;
import de.keksuccino.fancymenu.platform.services.IPlatformCompatibilityLayer;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;

import java.util.function.Consumer;

public class FabricCompatibilityLayer implements IPlatformCompatibilityLayer {

    @Override
    public void renderTitleScreenOverlay(PoseStack matrix, Font font, Screen screen, boolean showBranding, boolean showForgeNotificationTop, boolean showForgeNotificationCopyright) {
        if (showBranding) {
            String branding = "Minecraft " + SharedConstants.getCurrentVersion().getName();
            if (Minecraft.getInstance().isDemo()) {
                branding = branding + " Demo";
            } else {
                branding = branding + ("release".equalsIgnoreCase(Minecraft.getInstance().getVersionType()) ? "" : "/" + Minecraft.getInstance().getVersionType());
            }
            if (Minecraft.checkModStatus().shouldReportAsModified()) {
                branding = branding + I18n.get("menu.modded");
            }
            GuiComponent.drawString(matrix, font, branding, 2, screen.height - 10, -1);
        }
    }

    @Override
    public void renderTitleScreenDeepCustomizationBranding(PoseStack matrix, Font font, Screen screen, int lastWidth, int lastHeight, Consumer<Integer> lastWidthSetter, Consumer<Integer> lastHeightSetter) {
        String branding = "Minecraft " + SharedConstants.getCurrentVersion().getName();
        if (Minecraft.getInstance().isDemo()) {
            branding = branding + " Demo";
        } else {
            branding = branding + ("release".equalsIgnoreCase(Minecraft.getInstance().getVersionType()) ? "" : "/" + Minecraft.getInstance().getVersionType());
        }
        if (Minecraft.checkModStatus().shouldReportAsModified()) {
            branding = branding + I18n.get("menu.modded");
        }
        GuiComponent.drawString(matrix, font, branding, 2, screen.height - 10, -1);
        lastWidthSetter.accept(font.width(branding));
        lastHeightSetter.accept(10);
    }

    @Override
    public void registerTitleScreenDeepCustomizationLayerElements(TitleScreenLayer layer) {
    }

}
