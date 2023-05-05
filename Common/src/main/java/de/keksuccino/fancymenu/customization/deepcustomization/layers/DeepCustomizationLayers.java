package de.keksuccino.fancymenu.customization.deepcustomization.layers;

import de.keksuccino.fancymenu.customization.deepcustomization.DeepCustomizationLayerRegistry;
import de.keksuccino.fancymenu.customization.deepcustomization.layers.titlescreen.TitleScreenLayer;

public class DeepCustomizationLayers {

    public static void registerAll() {

        DeepCustomizationLayerRegistry.registerLayer(new TitleScreenLayer());

    }

}
