package de.keksuccino.fancymenu.platform.services;

import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.TitleScreenLayer;
import net.minecraft.network.chat.Component;

import java.util.List;

public interface IPlatformCompatibilityLayer {

    List<Component> getTitleScreenBrandingLines();

    void registerTitleScreenDeepCustomizationLayerElements(TitleScreenLayer layer);

}
