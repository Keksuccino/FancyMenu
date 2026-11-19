package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static de.keksuccino.fancymenu.testing.TestPlaceholders.echo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Text Width measures what gets rendered, so {@code &} formatting codes get converted to the {@code §} codes the font knows before measuring.
 * The tests measure with a stand-in for the font, which needs a running Minecraft client.
 */
@ResourceLock("PlaceholderParser global state")
class TextWidthPlaceholderTest {

    /** Every text the registered placeholder measured. Only used while the global parser state is locked. */
    private static final List<String> MEASURED = new ArrayList<>();

    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeEach
    void disableCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
        MEASURED.clear();
    }

    @AfterEach
    void restoreCaching() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void ampersandCodesGetConvertedBeforeMeasuring() {
        List<String> measured = new ArrayList<>();
        TextWidthPlaceholder placeholder = new TextWidthPlaceholder(text -> {
            measured.add(text);
            return visibleCharacters(text);
        });

        assertEquals("2", replacement(placeholder, "&lHi"));
        assertEquals("2", replacement(placeholder, "§lHi"));
        assertEquals("5", replacement(placeholder, "&a&lHello&r"));
        assertEquals(List.of("§lHi", "§lHi", "§a§lHello§r"), measured);
    }

    @Test
    void textsWithoutCodesGetMeasuredAsTheyAre() {
        List<String> measured = new ArrayList<>();
        TextWidthPlaceholder placeholder = new TextWidthPlaceholder(text -> {
            measured.add(text);
            return text.length();
        });
        String text = "Rock & Roll &A &";

        assertEquals("16", replacement(placeholder, text));
        assertEquals("0", replacement(placeholder, ""));
        assertEquals(text, measured.get(0));
    }

    @Test
    void missingTextHasNoReplacement() {
        assertNull(replacement(new TextWidthPlaceholder(String::length), null));
    }

    @Test
    void widthIsTheSameWithAndWithoutPlaceholdersInTheText() {
        registerParserPlaceholder();

        assertEquals("5", PlaceholderParser.replacePlaceholders(serialized("&lHello")));
        assertEquals("5", PlaceholderParser.replacePlaceholders(serialized("&l" + echo("Hello"))));
        assertEquals("5", PlaceholderParser.replacePlaceholders(serialized(echo("&l") + "Hello")));
        assertEquals("5", PlaceholderParser.replacePlaceholders(serialized(echo("§lHello"))));
        assertEquals(List.of("§lHello", "§lHello", "§lHello", "§lHello"), MEASURED);
    }

    /**
     * Counts the characters the font would render: a {@code §} hides itself and the character behind it, like Minecraft's text rendering does.
     */
    private static int visibleCharacters(@NotNull String text) {
        int visible = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '§') {
                i++;
            } else {
                visible++;
            }
        }
        return visible;
    }

    @Nullable
    private static String replacement(@NotNull TextWidthPlaceholder placeholder, @Nullable String text) {
        HashMap<String, String> values = new HashMap<>();
        if (text != null) values.put("text", text);
        return placeholder.getReplacementFor(new DeserializedPlaceholderString("text_width", values, ""));
    }

    @NotNull
    private static String serialized(@NotNull String text) {
        return "{\"placeholder\":\"text_width\",\"values\":{\"text\":\"" + text + "\"}}";
    }

    /**
     * The placeholder registry is global and has no removal API, so the parser tests register the placeholder once per test JVM.
     */
    private static void registerParserPlaceholder() {
        if (PlaceholderRegistry.getPlaceholder("text_width") != null) return;
        PlaceholderRegistry.register(new TextWidthPlaceholder(text -> {
            MEASURED.add(text);
            return visibleCharacters(text);
        }) {
            // The default check needs a running Minecraft client to compare threads.
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
    }

}
