package de.keksuccino.fancymenu.customization.element.elements.browser;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserElementTest {

    private static final String ECHO_PLACEHOLDER_ID = "browser_url_echo_test";

    @BeforeAll
    static void registerEchoPlaceholder() {
        // The registry is global and has no removal API, so register once per test JVM.
        if (PlaceholderRegistry.getPlaceholder(ECHO_PLACEHOLDER_ID) == null) PlaceholderRegistry.register(new EchoPlaceholder());
    }

    @Test
    @ResourceLock("PlaceholderParser global state")
    void resolvedUrlKeepsParametersThatLookLikeFormattingCodes() {
        String url = "https://example.com/page?theme=" + echo("dark") + "&embed=true&f=1&lang=en&r=0&0=k";

        assertEquals("https://example.com/page?theme=dark&embed=true&f=1&lang=en&r=0&0=k", BrowserElement.resolveUrl(url));
    }

    @Test
    @ResourceLock("PlaceholderParser global state")
    void resolvedUrlKeepsFormattingCodeLookalikesInNestedPlaceholderValues() {
        String url = "https://example.com/?q=" + echo("a&eb" + echo("c&fd"));

        assertEquals("https://example.com/?q=a&ebc&fd", BrowserElement.resolveUrl(url));
    }

    @Test
    @ResourceLock("PlaceholderParser global state")
    void resolvedUrlWithoutPlaceholdersStaysUnchanged() {
        String url = "https://example.com/?a=1&embed=true&f=1";

        assertEquals(url, BrowserElement.resolveUrl(url));
        assertEquals("", BrowserElement.resolveUrl(""));
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

    private static String echo(String text) {
        return "{\"placeholder\":\"" + ECHO_PLACEHOLDER_ID + "\",\"values\":{\"text\":\"" + text + "\"}}";
    }

    private static final class EchoPlaceholder extends Placeholder {

        private EchoPlaceholder() {
            super(ECHO_PLACEHOLDER_ID);
        }

        @Override
        public String getReplacementFor(DeserializedPlaceholderString dps) {
            return dps.values.get("text");
        }

        @Override
        public List<String> getValueNames() {
            return List.of("text");
        }

        @Override
        public String getDisplayName() {
            return "Browser URL Echo Test";
        }

        @Override
        public List<String> getDescription() {
            return List.of();
        }

        @Override
        public String getCategory() {
            return "test";
        }

        @Override
        public DeserializedPlaceholderString getDefaultPlaceholderString() {
            return new DeserializedPlaceholderString(this.getIdentifier(), new HashMap<>(), echo(""));
        }

        // The default check needs a running Minecraft client to compare threads.
        @Override
        public boolean checkAsync() {
            return true;
        }

    }

}
