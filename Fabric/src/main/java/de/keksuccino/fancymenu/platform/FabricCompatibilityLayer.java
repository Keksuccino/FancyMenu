package de.keksuccino.fancymenu.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.TitleScreenLayer;
import de.keksuccino.fancymenu.platform.services.IPlatformCompatibilityLayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

public class FabricCompatibilityLayer implements IPlatformCompatibilityLayer {

    @Override
    public void renderTitleScreenOverlay(PoseStack matrix, Font font, Screen screen, boolean showBranding, boolean showForgeNotificationTop, boolean showForgeNotificationCopyright) {
    }

    @Override
    public void renderCustomTitleScreenBrandingLines(PoseStack matrix, Font font, Screen screen, int lastWidth, int lastHeight, Consumer<Integer> lastWidthSetter, Consumer<Integer> lastHeightSetter) {
    }

    @Override
    public void registerTitleScreenDeepCustomizationLayerElements(TitleScreenLayer layer) {
    }

}
