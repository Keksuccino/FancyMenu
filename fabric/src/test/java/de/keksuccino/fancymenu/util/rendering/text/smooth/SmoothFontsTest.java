package de.keksuccino.fancymenu.util.rendering.text.smooth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothFontsTest {

    /**
     * Loading the UI font needs a running client resource manager, so any load attempt started by the peek would throw here.
     */
    @Test
    void peekingTheMinecraftFontFallbackDoesNotLoadTheUIFont() {
        assertFalse(SmoothFonts.peekShouldUseMinecraftFont(false));
        assertTrue(SmoothFonts.peekShouldUseMinecraftFont(true));
        assertFalse(SmoothFonts.peekShouldUseMinecraftFont(false));
    }

}
