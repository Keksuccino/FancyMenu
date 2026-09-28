package de.keksuccino.fancymenu.customization.element.elements.browser;

import org.junit.jupiter.api.Test;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserElementTest {

    @Test
    void pendingBrowserReservesAnInputSlotBeforeRuntimeReadiness() {
        BrowserElement element = new BrowserElement(new BrowserElementBuilder());
        List<GuiEventListener> widgets = element.getWidgetsToRegister();

        assertNotNull(widgets);
        assertEquals(1, widgets.size());
        assertInstanceOf(BrowserWidgetSlot.class, widgets.get(0));
        assertInstanceOf(NarratableEntry.class, widgets.get(0));
        assertFalse(widgets.get(0).isMouseOver(0, 0));
    }

    @Test
    void browserStatusDistinguishesMissingStartingAndFailedRuntime() {
        assertEquals("fancymenu.elements.browser.rinku_not_loaded", BrowserElement.getBrowserStatusKey(false, false));
        assertEquals("fancymenu.elements.browser.rinku_initializing", BrowserElement.getBrowserStatusKey(true, false));
        assertEquals("fancymenu.elements.browser.rinku_failed", BrowserElement.getBrowserStatusKey(true, true));
        assertEquals("fancymenu.elements.browser.rinku_failed", BrowserElement.getBrowserStatusKey(false, true));
    }

    @Test
    void browserInputRequiresRenderedInteractableNonEditorElement() {
        assertTrue(BrowserElement.isBrowserInputEnabled(true, true, false));
        assertFalse(BrowserElement.isBrowserInputEnabled(false, true, false));
        assertFalse(BrowserElement.isBrowserInputEnabled(true, false, false));
        assertFalse(BrowserElement.isBrowserInputEnabled(true, true, true));
    }

}
