package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.util.properties.Property;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OvergrowthDecorationOverlayTest {

    @Test
    void theOverlayIsKnownByItsIdentifier() {
        assertEquals("overgrowth", new OvergrowthDecorationOverlayBuilder().getIdentifier());
    }

    @Test
    void optionsHaveTheirKeysAndDefaults() {
        OvergrowthDecorationOverlay overlay = new OvergrowthDecorationOverlay();
        assertDefault(overlay, "grass", true);
        assertDefault(overlay, "left_vines", true);
        assertDefault(overlay, "right_vines", true);
        assertDefault(overlay, "on_elements", true);
        assertDefault(overlay, "flowers_on_vines", true);
        assertDefault(overlay, "flowers_on_elements", true);
        assertDefault(overlay, "flowers_in_grass", true);
        assertDefault(overlay, "flower_amount", 25);
    }

    private static void assertDefault(OvergrowthDecorationOverlay overlay, String key, Object value) {
        Property<?> property = overlay.getProperty(key);
        assertNotNull(property, key);
        assertEquals(value, property.getDefault(), key);
    }

}
