package de.keksuccino.fancymenu.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.TitleScreenLayer;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.forge.copyright.TitleScreenForgeCopyrightElement;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.forge.top.TitleScreenForgeTopElement;
import de.keksuccino.fancymenu.platform.services.IPlatformCompatibilityLayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.internal.BrandingControl;

import java.util.function.Consumer;

public class ForgeCompatibilityLayer implements IPlatformCompatibilityLayer {

    @Override
    public void renderTitleScreenOverlay(PoseStack matrix, Font font, Screen screen, boolean showBranding, boolean showForgeNotificationTop, boolean showForgeNotificationCopyright) {
        if (showBranding) {
            BrandingControl.forEachLine(true, true, (brdline, brd) -> {
                GuiComponent.drawString(matrix, font, brd, 2, screen.height - (10 + brdline * (font.lineHeight + 1)), 16777215);
            });
        }
        if (showForgeNotificationTop) {
            ForgeHooksClient.renderMainMenu((TitleScreen) screen, matrix, Minecraft.getInstance().font, screen.width, screen.height, 255);
        }
        if (showForgeNotificationCopyright) {
            BrandingControl.forEachAboveCopyrightLine((brdline, brd) -> {
                GuiComponent.drawString(matrix, font, brd, screen.width - font.width(brd) - 1, screen.height - (11 + (brdline + 1) * (font.lineHeight + 1)), 16777215);
            });
        }
    }

    @Override
    public void renderCustomTitleScreenBrandingLines(PoseStack matrix, Font font, Screen screen, final int lastWidth, final int lastHeight, Consumer<Integer> lastWidthSetter, Consumer<Integer> lastHeightSetter) {
        BrandingControl.forEachLine(true, true, (brdline, brd) -> {
            GuiComponent.drawString(matrix, font, brd, 2, screen.height - ( 10 + brdline * (font.lineHeight + 1)), 16777215);
            int w = font.width(brd);
            if (lastWidth < w) {
                lastWidthSetter.accept(w);
            }
            lastHeightSetter.accept(lastHeight + font.lineHeight + 1);
        });
    }

    @Override
    public void registerTitleScreenDeepCustomizationLayerElements(TitleScreenLayer layer) {
        layer.registerElement(new TitleScreenForgeCopyrightElement(layer));
        layer.registerElement(new TitleScreenForgeTopElement(layer));
    }

}
