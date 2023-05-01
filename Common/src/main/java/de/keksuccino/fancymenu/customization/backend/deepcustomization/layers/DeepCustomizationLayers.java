package de.keksuccino.fancymenu.customization.backend.deepcustomization.layers;

import de.keksuccino.fancymenu.customization.backend.deepcustomization.DeepCustomizationLayerRegistry;
import de.keksuccino.fancymenu.customization.backend.deepcustomization.layers.titlescreen.TitleScreenLayer;

public class DeepCustomizationLayers {

    public static void registerAll() {

        DeepCustomizationLayerRegistry.registerLayer(new TitleScreenLayer());

    }

}
