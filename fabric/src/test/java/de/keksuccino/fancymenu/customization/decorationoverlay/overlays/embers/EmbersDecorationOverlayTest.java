package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.embers;

import de.keksuccino.fancymenu.util.properties.Property;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EmbersDecorationOverlayTest {

    @Test
    void theOverlayIsKnownByItsIdentifier() {
        assertEquals("embers", new EmbersDecorationOverlayBuilder().getIdentifier());
    }

    @Test
    void optionsHaveTheirKeysAndDefaults() {
        EmbersDecorationOverlay overlay = new EmbersDecorationOverlay();
        assertDefault(overlay, "ash_on_floor", true);
        assertDefault(overlay, "ash_on_elements", true);
    }

    private static void assertDefault(EmbersDecorationOverlay overlay, String key, Object value) {
        Property<?> property = overlay.getProperty(key);
        assertNotNull(property, key);
        assertEquals(value, property.getDefault(), key);
    }

}
