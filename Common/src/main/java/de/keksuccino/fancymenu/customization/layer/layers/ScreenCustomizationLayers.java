package de.keksuccino.fancymenu.customization.layer.layers;

import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;

public class ScreenCustomizationLayers {

    public static void registerAll() {

        ScreenCustomizationLayerHandler.registerLayer(new TitleScreenLayer());
        ScreenCustomizationLayerHandler.registerLayer(new MoreRefinedStorageTitleScreenLayer());
        ScreenCustomizationLayerHandler.registerLayer(new DummyCoreTitleScreenLayer());
        ScreenCustomizationLayerHandler.registerLayer(new WorldLoadingScreenLayer());
        ScreenCustomizationLayerHandler.registerLayer(new PauseScreenLayer());

    }

}
