package de.keksuccino.fancymenu.mixin.support.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TitleScreenBrandingCaptureTest {

    @Test
    void capturesTransformedBrandingAndResizesUncustomizedBounds() {
        TitleScreenBrandingCapture capture = new TitleScreenBrandingCapture();

        assertNull(capture.getCapturedText());

        TitleScreenBrandingCapture.Update update = capture.capture("Minecraft 1.20.1/Fabric (42 Mods)", null, null);

        assertTrue(update.textChanged());
        assertTrue(update.resizeWidth());
        assertTrue(update.resizeHeight());
        assertEquals("Minecraft 1.20.1/Fabric (42 Mods)", capture.getCapturedText());
    }

    @Test
    void ignoresRepeatedBrandingForTheSameScreen() {
        TitleScreenBrandingCapture capture = new TitleScreenBrandingCapture();
        capture.capture("Minecraft 1.20.1/Fabric (42 Mods)", null, null);

        TitleScreenBrandingCapture.Update update = capture.capture("Minecraft 1.20.1/Fabric (42 Mods)", null, null);

        assertFalse(update.textChanged());
        assertFalse(update.resizeWidth());
        assertFalse(update.resizeHeight());
    }

    @Test
    void preservesCapturedBrandingAcrossRendererReinitialization() {
        TitleScreenBrandingCapture capture = new TitleScreenBrandingCapture();
        capture.capture("Minecraft 1.20.1/Fabric (42 Mods)", null, null);

        String replacementRendererText = capture.getCapturedText();
        TitleScreenBrandingCapture.Update repeatedCapture = capture.capture(replacementRendererText, null, null);

        assertEquals("Minecraft 1.20.1/Fabric (42 Mods)", replacementRendererText);
        assertFalse(repeatedCapture.textChanged());
        assertFalse(repeatedCapture.resizeWidth());
        assertFalse(repeatedCapture.resizeHeight());
    }

    @Test
    void capturesChangedBrandingAndPreservesCustomizedBoundsIndependently() {
        TitleScreenBrandingCapture capture = new TitleScreenBrandingCapture();
        capture.capture("Minecraft 1.20.1/Fabric (42 Mods)", null, null);

        TitleScreenBrandingCapture.Update customWidthUpdate = capture.capture("Minecraft 1.20.1/Fabric (43 Mods)", 200, null);
        TitleScreenBrandingCapture.Update customHeightUpdate = capture.capture("Minecraft 1.20.1/Fabric (44 Mods)", null, 20);

        assertTrue(customWidthUpdate.textChanged());
        assertFalse(customWidthUpdate.resizeWidth());
        assertTrue(customWidthUpdate.resizeHeight());
        assertTrue(customHeightUpdate.textChanged());
        assertTrue(customHeightUpdate.resizeWidth());
        assertFalse(customHeightUpdate.resizeHeight());
    }

    @Test
    void keepsCapturedBrandingIsolatedPerScreenController() {
        TitleScreenBrandingCapture firstScreenCapture = new TitleScreenBrandingCapture();
        TitleScreenBrandingCapture secondScreenCapture = new TitleScreenBrandingCapture();
        firstScreenCapture.capture("Minecraft 1.20.1/Fabric (42 Mods)", null, null);

        TitleScreenBrandingCapture.Update secondScreenUpdate = secondScreenCapture.capture("Minecraft 1.20.1/Fabric (42 Mods)", null, null);

        assertTrue(secondScreenUpdate.textChanged());
    }
}
