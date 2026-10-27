package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("PlaceholderParser global state")
class RawValuePlaceholderResolverTest {

    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void disableCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
    }

    @AfterEach
    void restoreCaching() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void keepsParametersThatLookLikeFormattingCodes() {
        String url = "https://example.com/page?theme=" + TestPlaceholders.echo("dark") + "&embed=true&f=1&lang=en&r=0&0=k";

        assertEquals("https://example.com/page?theme=dark&embed=true&f=1&lang=en&r=0&0=k", new RawValuePlaceholderResolver().resolve(url));
    }

    @Test
    void keepsFormattingCodeLookalikesInNestedPlaceholderValues() {
        String path = "config/fancymenu/assets/" + TestPlaceholders.echo("a&eb" + TestPlaceholders.echo("c&fd")) + ".png";

        assertEquals("config/fancymenu/assets/a&ebc&fd.png", new RawValuePlaceholderResolver().resolve(path));
    }

    @Test
    void valuesWithoutPlaceholdersStayUnchanged() {
        RawValuePlaceholderResolver resolver = new RawValuePlaceholderResolver();

        assertEquals("https://example.com/?a=1&embed=true&f=1", resolver.resolve("https://example.com/?a=1&embed=true&f=1"));
        assertEquals("", resolver.resolve(""));
    }

    @Test
    void followsChangingPlaceholderResults() {
        String key = "raw_value_resolver_changes";
        TestPlaceholders.setValue(key, "first");
        RawValuePlaceholderResolver resolver = new RawValuePlaceholderResolver();
        String url = "https://example.com/?page=" + TestPlaceholders.value(key) + "&embed=true";

        assertEquals("https://example.com/?page=first&embed=true", resolver.resolve(url));
        TestPlaceholders.setValue(key, "second");
        assertEquals("https://example.com/?page=second&embed=true", resolver.resolve(url));
    }

    @Test
    void onlyRunsTheFormattingPreservingParseAgainAfterAChange() {
        String key = "raw_value_resolver_evaluations";
        TestPlaceholders.setValue(key, "first");
        RawValuePlaceholderResolver resolver = new RawValuePlaceholderResolver();
        String url = "https://example.com/?page=" + TestPlaceholders.value(key) + "&embed=true";

        resolver.resolve(url);
        assertEquals(2, TestPlaceholders.evaluations(key));
        resolver.resolve(url);
        resolver.resolve(url);
        assertEquals(4, TestPlaceholders.evaluations(key));
        TestPlaceholders.setValue(key, "second");
        resolver.resolve(url);
        assertEquals(6, TestPlaceholders.evaluations(key));
    }

    @Test
    void resolvesAgainWhenOnlyTheRawValueChanges() {
        RawValuePlaceholderResolver resolver = new RawValuePlaceholderResolver();
        String placeholder = TestPlaceholders.echo("x");

        assertEquals("x&e", resolver.resolve(placeholder + "&e"));
        assertEquals("x§e", resolver.resolve(placeholder + "§e"));
    }

}
