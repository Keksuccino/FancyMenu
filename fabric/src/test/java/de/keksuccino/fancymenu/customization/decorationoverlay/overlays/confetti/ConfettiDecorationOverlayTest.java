package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfettiDecorationOverlayTest {

    @Test
    void theBuilderKeepsItsIdentifierAndNewOverlaysPopAndBurstOnClicks() {
        assertEquals("confetti", new ConfettiDecorationOverlayBuilder().getIdentifier());
        ConfettiDecorationOverlay overlay = new ConfettiDecorationOverlay();
        assertTrue(overlay.confettiPoppers.tryGetNonNullElse(false));
        assertTrue(overlay.confettiClickBursts.tryGetNonNullElse(false));
        assertEquals(ConfettiDecorationOverlay.DEFAULT_LITTER_CAP, overlay.confettiParticleCap.getInteger());
    }

    @Test
    void anOldLayoutKeepsItsSettings() {
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("overlay_type", "confetti");
        serialized.putProperty("confetti_burst_amount", "2.0");
        serialized.putProperty("confetti_burst_density", "0.75");
        serialized.putProperty("confetti_scale", "2.0");
        serialized.putProperty("confetti_fall_speed", "0.5");
        serialized.putProperty("confetti_particle_cap", "500");
        serialized.putProperty("confetti_color_mix_mode", "false");
        serialized.putProperty("confetti_mouse_click_mode", "false");
        serialized.putProperty("confetti_color_hex", "#FF8800");

        ConfettiDecorationOverlay overlay = deserialize(serialized);

        // How often times how many pieces: the old bursts brought as much confetti as 150 percent of the new ones
        assertEquals(150, overlay.amount.getPercent());
        assertEquals(2, overlay.size.get());
        assertEquals(0.5F, overlay.confettiFallSpeed.getFloat());
        assertEquals(500, overlay.confettiParticleCap.getInteger());
        assertFalse(overlay.confettiColorMixMode.tryGetNonNullElse(true));
        assertEquals("#FF8800", overlay.confettiColorHex.get());
        assertTrue(overlay.confettiPoppers.tryGetNonNullElse(false));
        assertTrue(overlay.confettiClickBursts.tryGetNonNullElse(false));
    }

    @Test
    void theOldClickModeTurnsThePoppersOff() {
        PropertyContainer serialized = new PropertyContainer("decoration_overlay");
        serialized.putProperty("overlay_type", "confetti");
        serialized.putProperty("confetti_mouse_click_mode", "true");

        ConfettiDecorationOverlay overlay = deserialize(serialized);

        assertFalse(overlay.confettiPoppers.tryGetNonNullElse(true));
        assertTrue(overlay.confettiClickBursts.tryGetNonNullElse(false));
        assertEquals(100, overlay.amount.getPercent());
    }

    @Test
    void savedLayoutsKeepTheirNewOptions() {
        ConfettiDecorationOverlay original = new ConfettiDecorationOverlay();
        original.confettiPoppers.set(true);
        original.confettiClickBursts.set(false);
        original.amount.property.set(50);

        PropertyContainer written = new ConfettiDecorationOverlayBuilder()._serialize(original);
        // A leftover of an old version must not turn the poppers off again
        written.setInvulnerableProperties(false);
        written.putProperty("confetti_mouse_click_mode", "true");
        ConfettiDecorationOverlay loaded = deserialize(written);

        assertNull(written.getValue("confetti_burst_amount"));
        assertTrue(loaded.confettiPoppers.tryGetNonNullElse(false));
        assertFalse(loaded.confettiClickBursts.tryGetNonNullElse(true));
        assertEquals(50, loaded.amount.getPercent());
    }

    private static ConfettiDecorationOverlay deserialize(PropertyContainer serialized) {
        PropertyContainerSet layout = new PropertyContainerSet("fancymenu_layout");
        layout.putContainer(serialized);
        List<ConfettiDecorationOverlay> instances = new ConfettiDecorationOverlayBuilder()._deserializeAll(layout);
        assertEquals(1, instances.size());
        return instances.get(0);
    }

}
