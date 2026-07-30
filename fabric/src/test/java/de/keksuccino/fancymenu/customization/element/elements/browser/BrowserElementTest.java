package de.keksuccino.fancymenu.customization.element.elements.browser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserElementTest {

    @Test
    void browserInputRequiresRenderedInteractableNonEditorElement() {
        assertTrue(BrowserElement.isBrowserInputEnabled(true, true, false));
        assertFalse(BrowserElement.isBrowserInputEnabled(false, true, false));
        assertFalse(BrowserElement.isBrowserInputEnabled(true, false, false));
        assertFalse(BrowserElement.isBrowserInputEnabled(true, true, true));
    }

}
