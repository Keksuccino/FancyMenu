package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class FireworksDecorationOverlayTest {

    @Test
    void theBuilderKeepsItsIdentifier() {
        assertEquals("fireworks", new FireworksDecorationOverlayBuilder().getIdentifier());
    }

    @Test
    void anOldLayoutKeepsItsSettings() {
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("overlay_type", "fireworks");
        serialized.putProperty("fireworks_amount", "0.1");
        serialized.putProperty("fireworks_scale", "2.0");
        serialized.putProperty("fireworks_explosion_size", "1.5");
        serialized.putProperty("fireworks_show_rockets", "false");

        FireworksDecorationOverlay overlay = deserialize(serialized);

        assertEquals(25, overlay.amount.getPercent());
        assertEquals(2, overlay.size.get());
        assertEquals(1.5F, overlay.fireworksExplosionSize.getFloat());
        assertFalse(overlay.fireworksShowRockets.tryGetNonNullElse(true));
    }

    @Test
    void savedLayoutsWriteTheNewOptionsOnly() {
        FireworksDecorationOverlay original = new FireworksDecorationOverlay();
        original.amount.property.set(300);

        PropertyContainer written = new FireworksDecorationOverlayBuilder()._serialize(original);

        assertNull(written.getValue("fireworks_amount"));
        assertNull(written.getValue("fireworks_scale"));
        assertEquals(300, deserialize(written).amount.getPercent());
    }

    private static FireworksDecorationOverlay deserialize(PropertyContainer serialized) {
        PropertyContainerSet layout = new PropertyContainerSet("fancymenu_layout");
        layout.putContainer(serialized);
        List<FireworksDecorationOverlay> instances = new FireworksDecorationOverlayBuilder()._deserializeAll(layout);
        assertEquals(1, instances.size());
        return instances.get(0);
    }

}
