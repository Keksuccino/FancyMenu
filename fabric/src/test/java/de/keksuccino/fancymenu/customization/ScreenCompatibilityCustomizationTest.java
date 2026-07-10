package de.keksuccino.fancymenu.customization;

import de.keksuccino.fancymenu.customization.customgui.CustomGui;
import de.keksuccino.fancymenu.customization.customgui.CustomGuiBaseScreen;
import de.keksuccino.fancymenu.test.ScreenTestFactory;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import de.keksuccino.fancymenu.util.properties.PropertyContainerSet;
import eu.kennytv.forcecloseloadingscreen.TitleBridgeScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenCompatibilityCustomizationTest {

    private PropertyContainerSet previousCustomizableScreens;

    @BeforeEach
    void preserveCustomizationState() throws ReflectiveOperationException {
        Field customizableScreens = ScreenCustomization.class.getDeclaredField("customizableScreens");
        customizableScreens.setAccessible(true);
        this.previousCustomizableScreens = (PropertyContainerSet) customizableScreens.get(null);
    }

    @AfterEach
    void restoreCustomizationState() throws ReflectiveOperationException {
        Field customizableScreens = ScreenCustomization.class.getDeclaredField("customizableScreens");
        customizableScreens.setAccessible(true);
        customizableScreens.set(null, this.previousCustomizableScreens);
    }

    @Test
    void canonicalTitleCustomizationEnablesBridgeScreen() throws ReflectiveOperationException {
        PropertyContainerSet customizableScreens = new PropertyContainerSet("customizablemenus");
        customizableScreens.putContainer(new PropertyContainer(TitleScreen.class.getName()));
        Field field = ScreenCustomization.class.getDeclaredField("customizableScreens");
        field.setAccessible(true);
        field.set(null, customizableScreens);

        assertTrue(ScreenCustomization.isCustomizationEnabledForScreen(ScreenTestFactory.allocateScreen(TitleScreen.class), true));
        assertTrue(ScreenCustomization.isCustomizationEnabledForScreen(ScreenTestFactory.allocateScreen(TitleBridgeScreen.class), true));
        assertFalse(ScreenCustomization.isCustomizationEnabledForScreen(ScreenTestFactory.allocateScreen(UnrelatedTitleScreen.class), true));
    }

    @Test
    void bridgeAndVanillaTitleAreOneLogicalScreenAcrossLifecycleChanges() {
        assertTrue(ScreenCustomizationEvents.isNewLogicalScreen(null, ScreenTestFactory.allocateScreen(TitleBridgeScreen.class)));
        assertFalse(ScreenCustomizationEvents.isNewLogicalScreen(ScreenTestFactory.allocateScreen(TitleScreen.class), ScreenTestFactory.allocateScreen(TitleBridgeScreen.class)));
        assertFalse(ScreenCustomizationEvents.isNewLogicalScreen(ScreenTestFactory.allocateScreen(TitleBridgeScreen.class), ScreenTestFactory.allocateScreen(TitleScreen.class)));
        assertTrue(ScreenCustomizationEvents.isNewLogicalScreen(ScreenTestFactory.allocateScreen(TitleBridgeScreen.class), ScreenTestFactory.allocateScreen(UnrelatedTitleScreen.class)));
    }

    @Test
    void customGuiLifecycleStillUsesCustomIdentifiers() throws ReflectiveOperationException {
        assertFalse(ScreenCustomizationEvents.isNewLogicalScreen(customGui("same"), customGui("same")));
        assertTrue(ScreenCustomizationEvents.isNewLogicalScreen(customGui("first"), customGui("second")));
    }

    private static CustomGuiBaseScreen customGui(String identifier) throws ReflectiveOperationException {
        CustomGui gui = new CustomGui();
        gui.identifier = identifier;
        CustomGuiBaseScreen screen = ScreenTestFactory.allocateScreen(CustomGuiBaseScreen.class);
        Field guiField = CustomGuiBaseScreen.class.getDeclaredField("gui");
        guiField.setAccessible(true);
        guiField.set(screen, gui);
        return screen;
    }

    private static class UnrelatedTitleScreen extends TitleScreen {
    }

}
