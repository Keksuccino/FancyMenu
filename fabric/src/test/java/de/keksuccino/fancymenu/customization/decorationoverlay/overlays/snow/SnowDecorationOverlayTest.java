package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SnowDecorationOverlayTest {

    @Test
    void theBuilderKeepsItsIdentifier() {
        assertEquals("snowfall", new SnowDecorationOverlayBuilder().getIdentifier());
    }

    @Test
    void anOldLayoutKeepsItsSettings() {
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("overlay_type", "snowfall");
        serialized.putProperty("show_decoration_overlay", "true");
        serialized.putProperty("snow_intensity", "1.5");
        serialized.putProperty("snow_scale", "2.0");
        serialized.putProperty("snow_speed", "0.5");
        serialized.putProperty("snow_accumulation", "false");
        serialized.putProperty("snow_color_hex", "#FFC0CB");

        SnowDecorationOverlay overlay = deserialize(serialized);

        assertEquals(150, overlay.amount.getPercent());
        assertEquals(2, overlay.size.get());
        assertEquals(0.5F, overlay.snowSpeed.getFloat());
        assertFalse(overlay.snowAccumulation.tryGetNonNullElse(true));
        assertEquals("#FFC0CB", overlay.snowColorHex.get());
    }

    @Test
    void anOldLayoutWithoutTheLegacyNumbersGetsTheDefaults() {
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("overlay_type", "snowfall");

        SnowDecorationOverlay overlay = deserialize(serialized);

        assertEquals(100, overlay.amount.getPercent());
        assertEquals(1, overlay.size.get());
    }

    @Test
    void savedLayoutsWriteTheNewOptionsOnly() {
        SnowDecorationOverlay original = new SnowDecorationOverlay();
        original.amount.property.set(250);
        original.size.property.set(3);

        PropertyContainer written = new SnowDecorationOverlayBuilder()._serialize(original);

        assertEquals("250", written.getValue("amount"));
        assertEquals("3", written.getValue("size"));
        assertNull(written.getValue("snow_intensity"));
        assertNull(written.getValue("snow_scale"));
        SnowDecorationOverlay loaded = deserialize(written);
        assertEquals(250, loaded.amount.getPercent());
        assertEquals(3, loaded.size.get());
    }

    private static SnowDecorationOverlay deserialize(PropertyContainer serialized) {
        PropertyContainerSet layout = new PropertyContainerSet("fancymenu_layout");
        layout.putContainer(serialized);
        List<SnowDecorationOverlay> instances = new SnowDecorationOverlayBuilder()._deserializeAll(layout);
        assertEquals(1, instances.size());
        return instances.get(0);
    }

}
