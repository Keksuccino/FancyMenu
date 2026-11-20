package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.frost;

import de.keksuccino.fancymenu.util.properties.Property;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FrostDecorationOverlayTest {

    @Test
    void theOverlayIsKnownByItsIdentifier() {
        assertEquals("frost", new FrostDecorationOverlayBuilder().getIdentifier());
    }

    @Test
    void optionsHaveTheirKeysAndDefaults() {
        FrostDecorationOverlay overlay = new FrostDecorationOverlay();
        assertDefault(overlay, "screen_edges", true);
        assertDefault(overlay, "corners", true);
        assertDefault(overlay, "on_elements", true);
    }

    private static void assertDefault(FrostDecorationOverlay overlay, String key, Object value) {
        Property<?> property = overlay.getProperty(key);
        assertNotNull(property, key);
        assertEquals(value, property.getDefault(), key);
    }

}
