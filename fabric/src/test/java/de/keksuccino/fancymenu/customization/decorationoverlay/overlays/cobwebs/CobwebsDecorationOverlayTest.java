package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cobwebs;

import de.keksuccino.fancymenu.util.properties.Property;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CobwebsDecorationOverlayTest {

    @Test
    void theOverlayIsKnownByItsIdentifier() {
        assertEquals("cobwebs", new CobwebsDecorationOverlayBuilder().getIdentifier());
    }

    @Test
    void optionsHaveTheirKeysAndDefaults() {
        CobwebsDecorationOverlay overlay = new CobwebsDecorationOverlay();
        assertDefault(overlay, "left_web", true);
        assertDefault(overlay, "right_web", true);
        assertDefault(overlay, "spiders", true);
        assertDefault(overlay, "closed_front", false);
    }

    private static void assertDefault(CobwebsDecorationOverlay overlay, String key, Object value) {
        Property<?> property = overlay.getProperty(key);
        assertNotNull(property, key);
        assertEquals(value, property.getDefault(), key);
    }

}
