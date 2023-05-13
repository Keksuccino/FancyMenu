package de.keksuccino.fancymenu.platform.services;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.TitleScreenLayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Consumer;

public interface IPlatformCompatibilityLayer {

    void renderTitleScreenOverlay(PoseStack matrix, Font font, Screen screen, boolean showBranding, boolean showForgeNotificationTop, boolean showForgeNotificationCopyright);

    void renderTitleScreenDeepCustomizationBranding(PoseStack matrix, Font font, Screen screen, int lastWidth, int lastHeight, Consumer<Integer> lastWidthSetter, Consumer<Integer> lastHeightSetter);

    void registerTitleScreenDeepCustomizationLayerElements(TitleScreenLayer layer);

}
