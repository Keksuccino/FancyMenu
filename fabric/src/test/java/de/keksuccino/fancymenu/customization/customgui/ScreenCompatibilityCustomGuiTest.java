package de.keksuccino.fancymenu.customization.customgui;

import de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler;
import de.keksuccino.fancymenu.test.ScreenTestFactory;
import eu.kennytv.forcecloseloadingscreen.TitleBridgeScreen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ScreenCompatibilityCustomGuiTest {

    private Map<String, CustomGui> previousGuis;
    private Map<String, String> previousOverrides;

    @BeforeEach
    void preserveCustomGuiState() {
        this.previousGuis = new HashMap<>(CustomGuiHandler.CUSTOM_GUI_SCREENS);
        this.previousOverrides = new HashMap<>(CustomGuiHandler.OVERRIDDEN_SCREENS);
        CustomGuiHandler.CUSTOM_GUI_SCREENS.clear();
        CustomGuiHandler.OVERRIDDEN_SCREENS.clear();
    }

    @AfterEach
    void restoreCustomGuiState() {
        CustomGuiHandler.CUSTOM_GUI_SCREENS.clear();
        CustomGuiHandler.CUSTOM_GUI_SCREENS.putAll(this.previousGuis);
        CustomGuiHandler.OVERRIDDEN_SCREENS.clear();
        CustomGuiHandler.OVERRIDDEN_SCREENS.putAll(this.previousOverrides);
    }

    @Test
    void canonicalBridgeOverrideResolvesThroughLogicalTitleScreen() {
        CustomGui gui = customGui("replacement");
        CustomGuiHandler.CUSTOM_GUI_SCREENS.put(gui.identifier, gui);
        CustomGuiHandler.OVERRIDDEN_SCREENS.put(ScreenIdentifierHandler.getBestIdentifier(TitleBridgeScreen.class.getName()), gui.identifier);

        assertEquals(Map.of("title_screen", gui.identifier), CustomGuiHandler.getOverriddenScreens());
        assertSame(gui, CustomGuiHandler.getGuiForOverriddenScreen(ScreenTestFactory.allocateScreen(TitleBridgeScreen.class)));
    }

    @Test
    void unresolvedCustomGuiOverrideFailsWithoutReplacingScreen() {
        CustomGuiHandler.OVERRIDDEN_SCREENS.put(ScreenIdentifierHandler.getBestIdentifier(TitleBridgeScreen.class.getName()), "missing");

        assertFalse(CustomGuiHandler.getOverriddenScreens().containsKey(TitleBridgeScreen.class.getName()));
        assertNull(CustomGuiHandler.getGuiForOverriddenScreen(ScreenTestFactory.allocateScreen(TitleBridgeScreen.class)));
    }

    private static CustomGui customGui(String identifier) {
        CustomGui gui = new CustomGui();
        gui.identifier = identifier;
        return gui;
    }

}
