package de.keksuccino.fancymenu.customization.menuhandler.deepcustomizationlayer.layers;

import de.keksuccino.fancymenu.customization.menuhandler.deepcustomizationlayer.DeepCustomizationLayerRegistry;
import de.keksuccino.fancymenu.customization.menuhandler.deepcustomizationlayer.layers.titlescreen.TitleScreenLayer;

public class DeepCustomizationLayers {

    public static void registerAll() {

        DeepCustomizationLayerRegistry.registerLayer(new TitleScreenLayer());

    }

}
