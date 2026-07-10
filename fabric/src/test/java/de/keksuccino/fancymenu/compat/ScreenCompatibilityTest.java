package de.keksuccino.fancymenu.compat;

import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import eu.kennytv.forcecloseloadingscreen.TitleBridgeScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScreenCompatibilityTest {

    @Test
    void aliasesForceCloseWorldLoadingScreenTitleBridgeToTitleScreen() {
        assertSame(TitleScreen.class, ScreenCompatibility.getCompatibleScreenClass(TitleBridgeScreen.class));
        assertEquals(TitleScreen.class.getName(), ScreenCompatibility.getCompatibleScreenClassName(TitleBridgeScreen.class.getName()));
        assertEquals(TitleScreen.class.getName(), ScreenIdentifierHandler.tryFixInvalidIdentifierWithNonUniversal(TitleBridgeScreen.class.getName()));
        assertEquals("title_screen", ScreenIdentifierHandler.getBestIdentifier(TitleBridgeScreen.class.getName()));
    }

    @Test
    void leavesUnrelatedTitleScreenSubclassesIndependent() {
        assertSame(UnrelatedTitleScreen.class, ScreenCompatibility.getCompatibleScreenClass(UnrelatedTitleScreen.class));
    }

    private static class UnrelatedTitleScreen extends TitleScreen {
    }

}
