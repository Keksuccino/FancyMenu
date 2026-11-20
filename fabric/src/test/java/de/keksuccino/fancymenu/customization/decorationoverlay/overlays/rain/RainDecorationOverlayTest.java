package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RainDecorationOverlayTest {

    @Test
    void theBuilderKeepsItsIdentifierAndThunderStaysOffByDefault() {
        assertEquals("rainfall", new RainDecorationOverlayBuilder().getIdentifier());
        assertFalse(new RainDecorationOverlay().rainThunder.tryGetNonNullElse(true));
    }

    @Test
    void anOldLayoutKeepsItsSettings() {
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("overlay_type", "rainfall");
        serialized.putProperty("rain_intensity", "0.5");
        serialized.putProperty("rain_scale", "3.0");
        serialized.putProperty("rain_thunder", "true");
        serialized.putProperty("rain_thunder_brightness", "0.4");
        serialized.putProperty("rain_puddles", "false");
        serialized.putProperty("rain_drips", "false");
        serialized.putProperty("rain_color_hex", "#A0FFA0");

        RainDecorationOverlay overlay = deserialize(serialized);

        assertEquals(50, overlay.amount.getPercent());
        assertEquals(3, overlay.size.get());
        assertTrue(overlay.rainThunder.tryGetNonNullElse(false));
        assertEquals(0.4F, overlay.rainThunderBrightness.getFloat());
        assertFalse(overlay.rainPuddles.tryGetNonNullElse(true));
        assertFalse(overlay.rainDrips.tryGetNonNullElse(true));
        assertEquals("#A0FFA0", overlay.rainColorHex.get());
    }

    @Test
    void savedLayoutsWriteTheNewOptionsOnly() {
        RainDecorationOverlay original = new RainDecorationOverlay();
        original.amount.property.set(175);
        original.size.property.set(2);

        PropertyContainer written = new RainDecorationOverlayBuilder()._serialize(original);

        assertNull(written.getValue("rain_intensity"));
        assertNull(written.getValue("rain_scale"));
        RainDecorationOverlay loaded = deserialize(written);
        assertEquals(175, loaded.amount.getPercent());
        assertEquals(2, loaded.size.get());
    }

    private static RainDecorationOverlay deserialize(PropertyContainer serialized) {
        PropertyContainerSet layout = new PropertyContainerSet("fancymenu_layout");
        layout.putContainer(serialized);
        List<RainDecorationOverlay> instances = new RainDecorationOverlayBuilder()._deserializeAll(layout);
        assertEquals(1, instances.size());
        return instances.getFirst();
    }

}
