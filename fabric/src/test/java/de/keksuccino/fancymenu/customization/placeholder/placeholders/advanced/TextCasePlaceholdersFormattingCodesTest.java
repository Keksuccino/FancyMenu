package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderParser;
import de.keksuccino.fancymenu.customization.placeholder.PlaceholderRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static de.keksuccino.fancymenu.testing.TestPlaceholders.echo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The case placeholders keep the formatting codes of their text working, no matter if the text contains placeholders or not.
 * Values never get their {@code &} codes converted, so the placeholders always get the codes as they got written.
 */
@ResourceLock("PlaceholderParser global state")
class TextCasePlaceholdersFormattingCodesTest {

    private PlaceholderParser.PlaceholderCachingController originalCachingController;

    @BeforeAll
    static void registerCasePlaceholders() {
        register(new UppercaseTextPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
        register(new LowercaseTextPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
        register(new TitleCaseTextPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
        register(new SentenceCaseTextPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
        register(new AlternatingCaseTextPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
        register(new ToggleCaseTextPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
        register(new SnakeCaseTextPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
        register(new KebabCaseTextPlaceholder() {
            @Override
            public boolean checkAsync() {
                return true;
            }
        });
    }

    /**
     * The placeholder registry is global and has no removal API, so the placeholders get registered once per test JVM.
     * The default async check needs a running Minecraft client to compare threads.
     */
    private static void register(@NotNull Placeholder placeholder) {
        if (PlaceholderRegistry.getPlaceholder(placeholder.getIdentifier()) == null) PlaceholderRegistry.register(placeholder);
    }

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
    void uppercaseKeepsCodesWithAndWithoutPlaceholdersInItsText() {
        assertSameWithAndWithoutPlaceholders("§aSTEVE", "uppercase_text", "&a", "Steve");
        assertSameWithAndWithoutPlaceholders("§lROCK§rOLL", "uppercase_text", "&lrock&r", "oll");
    }

    @Test
    void lowercaseKeepsCodesWithAndWithoutPlaceholdersInItsText() {
        assertSameWithAndWithoutPlaceholders("§ahello §lworld", "lowercase_text", "&aHELLO &l", "WORLD");
    }

    @Test
    void wordAndSentenceTransformsIgnoreCodesWithAndWithoutPlaceholdersInTheirText() {
        assertSameWithAndWithoutPlaceholders("§aHello §lBig World", "title_case_text", "&ahello &l", "big world");
        assertSameWithAndWithoutPlaceholders("§aHello. §lWorld", "sentence_case_text", "&aHELLO. &l", "WORLD");
        assertSameWithAndWithoutPlaceholders("§ahElLo", "alternating_case_text", "&a", "Hello");
        assertSameWithAndWithoutPlaceholders("§ahELLO", "toggle_case_text", "&a", "Hello");
    }

    @Test
    void snakeAndKebabCaseLeaveOutCodesWithAndWithoutPlaceholdersInTheirText() {
        assertSameWithAndWithoutPlaceholders("hello_world", "snake_case_text", "&a", "Hello World");
        assertSameWithAndWithoutPlaceholders("hello-world", "kebab_case_text", "&a", "Hello World");
    }

    @Test
    void codesReturnedByNestedPlaceholdersKeepWorking() {
        assertEquals("§aSTEVE", PlaceholderParser.replacePlaceholders(serialized("uppercase_text", echo("§aSteve"))));
        assertEquals("§Ahello", PlaceholderParser.replacePlaceholders(serialized("lowercase_text", echo("§AHello"))));
        assertEquals("my_server", PlaceholderParser.replacePlaceholders(serialized("snake_case_text", echo("§lMy §aServer"))));
    }

    @Test
    void codesStayAmpersandCodesWhenFormattingCodesGetPreserved() {
        assertEquals("&aSTEVE", PlaceholderParser.replacePlaceholdersPreservingFormattingCodes(serialized("uppercase_text", "&aSteve")));
        assertEquals("&aSTEVE", PlaceholderParser.replacePlaceholdersPreservingFormattingCodes(serialized("uppercase_text", "&a" + echo("Steve"))));
    }

    /**
     * Parses the placeholder once with the whole text as plain value and once with the second part of the text coming from a nested placeholder.
     */
    private static void assertSameWithAndWithoutPlaceholders(@NotNull String expected, @NotNull String identifier, @NotNull String codePart, @NotNull String textPart) {
        assertEquals(expected, PlaceholderParser.replacePlaceholders(serialized(identifier, codePart + textPart)), identifier);
        assertEquals(expected, PlaceholderParser.replacePlaceholders(serialized(identifier, codePart + echo(textPart))), identifier);
        assertEquals(expected, PlaceholderParser.replacePlaceholders(serialized(identifier, echo(codePart) + textPart)), identifier);
    }

    @NotNull
    private static String serialized(@NotNull String identifier, @NotNull String text) {
        return "{\"placeholder\":\"" + identifier + "\",\"values\":{\"text\":\"" + text + "\"}}";
    }

}
