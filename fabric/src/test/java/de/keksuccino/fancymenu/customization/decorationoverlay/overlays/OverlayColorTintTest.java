package de.keksuccino.fancymenu.customization.decorationoverlay.overlays;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.util.properties.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock("PlaceholderParser global state")
class OverlayColorTintTest {

    private static final int REFERENCE = 0xCFE7FF;

    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void disablePlaceholderCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restorePlaceholderCaching() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void theReferenceColorKeepsThePalette() {
        assertEquals(0xB8D6F4, OverlayColorTint.recolor(0xB8D6F4, REFERENCE, REFERENCE));
        assertEquals(0x86AEDD, OverlayColorTint.recolor(0x86AEDD, REFERENCE, REFERENCE));
        // White as reference is a plain multiply, so white keeps everything
        assertEquals(0xA9C4E8, OverlayColorTint.recolor(0xA9C4E8, 0xFFFFFF, 0xFFFFFF));
    }

    @Test
    void theReferenceTurnsIntoTheUserColorAndShadesStayDarker() {
        int user = 0xFF4080;
        assertEquals(user, OverlayColorTint.recolor(REFERENCE, REFERENCE, user));
        int light = OverlayColorTint.recolor(0xF0F8FF, REFERENCE, user);
        int dark = OverlayColorTint.recolor(0x86AEDD, REFERENCE, user);
        assertTrue(((light >> 16) & 0xFF) > ((dark >> 16) & 0xFF));
        assertTrue(((light >> 8) & 0xFF) > ((dark >> 8) & 0xFF));
        assertTrue((light & 0xFF) > (dark & 0xFF));
    }

    @Test
    void brighterUserColorsClampAtWhite() {
        assertEquals(0xFFFFFF, OverlayColorTint.recolor(0xF0F8FF, REFERENCE, 0xFFFFFF));
        assertEquals(0xFF0000, OverlayColorTint.recolor(0xFFFFFF, 0xFFFFFF, 0xFF0000));
    }

    @Test
    void theUserAlphaFadesThePalette() {
        OverlayColorTint tint = new OverlayColorTint(0xFFFFFF);
        Property.ColorProperty color = Property.hexColorProperty("color", "#FFFFFF", true, "test");
        color.set("#FF000080");
        assertTrue(tint.update(color));
        assertEquals(0x80, tint.alpha());
        assertEquals(0x80FF0000, tint.tint(0xFFFFFF));
        assertEquals(0x40FF0000, tint.tint(0xFFFFFF, 128));
    }

    @Test
    void updateOnlyReportsRealChanges() {
        OverlayColorTint tint = new OverlayColorTint(0xFFFFFF);
        Property.ColorProperty color = Property.hexColorProperty("color", "#FFFFFF", true, "test");
        // The default color is what the tint starts with
        assertFalse(tint.update(color));
        color.set("#00FF00");
        assertTrue(tint.update(color));
        assertFalse(tint.update(color));
        assertEquals(0xFF00FF00, tint.argb());
    }

}
