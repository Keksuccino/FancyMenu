package de.keksuccino.fancymenu.customization.decorationoverlay;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecorationOverlayLayersTest {

    private static final DecorationOverlayPart BACK = DecorationOverlayPart.BACK;
    private static final DecorationOverlayPart ELEMENT = DecorationOverlayPart.ELEMENT;
    private static final DecorationOverlayPart FRONT = DecorationOverlayPart.FRONT;

    @Test
    void backgroundAndForegroundKeepsEveryPartInItsOwnLayer() {
        DecorationOverlayLayers layers = DecorationOverlayLayers.BACKGROUND_AND_FOREGROUND;
        assertEquals(List.of(BACK), layers.getParts(DecorationOverlayPass.BACKGROUND));
        assertEquals(List.of(ELEMENT, FRONT), layers.getParts(DecorationOverlayPass.FOREGROUND));
    }

    @Test
    void onlyBackgroundMergesTheScreenWidePartsBehindTheElements() {
        DecorationOverlayLayers layers = DecorationOverlayLayers.ONLY_BACKGROUND;
        assertEquals(List.of(BACK, FRONT), layers.getParts(DecorationOverlayPass.BACKGROUND));
        assertEquals(List.of(ELEMENT), layers.getParts(DecorationOverlayPass.FOREGROUND));
    }

    @Test
    void onlyForegroundMergesEverythingInFrontOfTheElements() {
        DecorationOverlayLayers layers = DecorationOverlayLayers.ONLY_FOREGROUND;
        assertEquals(List.of(), layers.getParts(DecorationOverlayPass.BACKGROUND));
        assertEquals(List.of(BACK, ELEMENT, FRONT), layers.getParts(DecorationOverlayPass.FOREGROUND));
    }

    @Test
    void everyModeDrawsEveryPartExactlyOnceAndElementPartsAlwaysInFront() {
        for (DecorationOverlayLayers layers : DecorationOverlayLayers.values()) {
            List<DecorationOverlayPart> drawn = new ArrayList<>(layers.getParts(DecorationOverlayPass.BACKGROUND));
            drawn.addAll(layers.getParts(DecorationOverlayPass.FOREGROUND));
            assertEquals(3, drawn.size(), layers.name());
            assertTrue(drawn.containsAll(List.of(BACK, ELEMENT, FRONT)), layers.name());
            assertTrue(layers.getParts(DecorationOverlayPass.FOREGROUND).contains(ELEMENT), layers.name());
            // Back content never draws in front of front content
            assertTrue(drawn.indexOf(BACK) < drawn.indexOf(FRONT), layers.name());
        }
    }

    @Test
    void partListsCanNotBeChanged() {
        assertThrows(UnsupportedOperationException.class, () -> DecorationOverlayLayers.DEFAULT.getParts(DecorationOverlayPass.FOREGROUND).add(BACK));
        assertEquals(DecorationOverlayLayers.BACKGROUND_AND_FOREGROUND, DecorationOverlayLayers.DEFAULT);
    }

}
