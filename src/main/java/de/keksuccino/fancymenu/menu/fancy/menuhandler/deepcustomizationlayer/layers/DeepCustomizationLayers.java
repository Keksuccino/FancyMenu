//---
package de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers;

import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayerRegistry;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.layers.titlescreen.TitleScreenLayer;

public class DeepCustomizationLayers {

    public static void registerAll() {

        DeepCustomizationLayerRegistry.registerLayer(new TitleScreenLayer());

    }

}
