package de.keksuccino.drippyloadingscreen.customization.deepcustomization;

import de.keksuccino.drippyloadingscreen.customization.deepcustomization.overlay.OverlayDeepCustomizationLayer;
import de.keksuccino.fancymenu.menu.fancy.menuhandler.deepcustomizationlayer.DeepCustomizationLayerRegistry;

public class DeepCustomizationLayers {

    public static void registerAll() {

        DeepCustomizationLayerRegistry.registerLayer(new OverlayDeepCustomizationLayer());

    }

}
