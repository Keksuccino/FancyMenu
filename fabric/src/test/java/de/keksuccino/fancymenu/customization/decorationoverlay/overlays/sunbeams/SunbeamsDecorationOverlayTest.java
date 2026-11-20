package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams;

import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SunbeamsDecorationOverlayTest {

    @Test
    void optionsStartWithTheMockupLook() {
        SunbeamsDecorationOverlay overlay = new SunbeamsDecorationOverlay();
        assertEquals(SunbeamsSide.TOP, overlay.sunSide.getDefault());
        assertEquals(-50, overlay.sunAngle.getDefault());
        assertEquals("#FFE9A6", overlay.sunColor.getDefault());
        assertEquals(100, overlay.sunIntensity.getDefault());
        assertEquals(true, overlay.sunGlow.getDefault());
        assertEquals(true, overlay.dust.getDefault());
        assertEquals(true, overlay.hitElements.getDefault());
    }

    @Test
    void optionsSurviveSavingAndLoading() {
        SunbeamsDecorationOverlay original = new SunbeamsDecorationOverlay();
        original.sunSide.set(SunbeamsSide.LEFT);
        original.sunAngle.set(35);
        original.sunColor.set("#A0C8FF");
        original.sunIntensity.set(150);
        original.sunGlow.set(false);
        original.dust.set(false);
        original.hitElements.set(false);
        PropertyContainer saved = new SunbeamsDecorationOverlayBuilder()._serialize(original);
        assertEquals("left", saved.getValue("sun_side"));
        PropertyContainerSet layout = new PropertyContainerSet("fancymenu_layout");
        layout.putContainer(saved);
        List<SunbeamsDecorationOverlay> loaded = new SunbeamsDecorationOverlayBuilder()._deserializeAll(layout);
        assertEquals(1, loaded.size());
        SunbeamsDecorationOverlay overlay = loaded.getFirst();
        assertEquals(SunbeamsSide.LEFT, overlay.sunSide.get());
        assertEquals(35, overlay.sunAngle.getInteger());
        assertEquals("#A0C8FF", overlay.sunColor.get());
        assertEquals(150, overlay.sunIntensity.getInteger());
        assertEquals(false, overlay.sunGlow.get());
        assertEquals(false, overlay.dust.get());
        assertEquals(false, overlay.hitElements.get());
    }

}
