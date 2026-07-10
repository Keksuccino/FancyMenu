package de.keksuccino.fancymenu.customization.layer;

import de.keksuccino.fancymenu.test.ScreenTestFactory;
import eu.kennytv.forcecloseloadingscreen.TitleBridgeScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScreenCompatibilityLayerTest {

    private Map<String, ScreenCustomizationLayer> previousLayers;

    @BeforeEach
    void preserveLayers() {
        this.previousLayers = new HashMap<>(ScreenCustomizationLayerHandler.LAYERS);
        ScreenCustomizationLayerHandler.LAYERS.clear();
    }

    @AfterEach
    void restoreLayers() {
        ScreenCustomizationLayerHandler.LAYERS.clear();
        ScreenCustomizationLayerHandler.LAYERS.putAll(this.previousLayers);
    }

    @Test
    void bridgeResolvesRegisteredTitleLayerThroughInstanceAndClassPaths() {
        ScreenCustomizationLayer titleLayer = new ScreenCustomizationLayer("title_screen");
        ScreenCustomizationLayerHandler.registerLayer(titleLayer);

        assertSame(titleLayer, ScreenCustomizationLayerHandler.getLayerOfScreen(ScreenTestFactory.allocateScreen(TitleScreen.class)));
        assertSame(titleLayer, ScreenCustomizationLayerHandler.getLayerOfScreen(ScreenTestFactory.allocateScreen(TitleBridgeScreen.class)));
        assertSame(titleLayer, ScreenCustomizationLayerHandler.getLayerOfScreen(TitleBridgeScreen.class));
        assertNull(ScreenCustomizationLayerHandler.getLayerOfScreen(UnrelatedTitleScreen.class));
    }

    private static class UnrelatedTitleScreen extends TitleScreen {
    }

}
