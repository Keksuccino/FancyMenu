package de.keksuccino.fancymenu.customization.deep.layers;

import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayerRegistry;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.TitleScreenLayer;

public class DeepScreenCustomizationLayers {

    public static final TitleScreenLayer TITLE_SCREEN = new TitleScreenLayer();

    public static void registerAll() {

        DeepScreenCustomizationLayerRegistry.register(TITLE_SCREEN);

    }

}
