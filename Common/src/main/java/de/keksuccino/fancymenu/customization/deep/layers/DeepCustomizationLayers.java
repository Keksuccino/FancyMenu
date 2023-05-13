package de.keksuccino.fancymenu.customization.deep.layers;

import de.keksuccino.fancymenu.customization.deep.DeepScreenCustomizationLayerRegistry;
import de.keksuccino.fancymenu.customization.deep.layers.titlescreen.TitleScreenLayer;

public class DeepCustomizationLayers {

    public static void registerAll() {

        DeepScreenCustomizationLayerRegistry.register(new TitleScreenLayer());

    }

}
