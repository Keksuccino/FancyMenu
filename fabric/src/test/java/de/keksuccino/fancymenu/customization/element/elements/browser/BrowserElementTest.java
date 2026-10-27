package de.keksuccino.fancymenu.customization.element.elements.browser;

import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
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
    @ResourceLock("PlaceholderParser global state")
    void resolvedUrlKeepsParametersThatLookLikeFormattingCodes() {
        String url = "https://example.com/page?theme=" + TestPlaceholders.echo("dark") + "&embed=true&f=1&lang=en&r=0&0=k";

        assertEquals("https://example.com/page?theme=dark&embed=true&f=1&lang=en&r=0&0=k", resolveUrl(url));
    }

    @Test
    @ResourceLock("PlaceholderParser global state")
    void resolvedUrlWithoutPlaceholdersStaysUnchanged() {
        String url = "https://example.com/?a=1&embed=true&f=1";

        assertEquals(url, resolveUrl(url));
        assertEquals("", resolveUrl(""));
    }

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

    private static String resolveUrl(String url) {
        PlaceholderParser.PlaceholderCachingController originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        try {
            PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
            BrowserElement element = new BrowserElement(new BrowserElementBuilder());
            element.url = url;
            return element.resolveUrl();
        } finally {
            PlaceholderParser.setPlaceholderCachingController(originalCachingController);
        }
    }

}
