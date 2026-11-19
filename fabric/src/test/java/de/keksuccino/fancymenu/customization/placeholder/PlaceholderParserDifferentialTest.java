package de.keksuccino.fancymenu.customization.placeholder;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.testing.TestPlaceholders;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static de.keksuccino.fancymenu.testing.TestPlaceholders.echo;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.join;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.nullReplacement;
import static de.keksuccino.fancymenu.testing.TestPlaceholders.value;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Compares the parser with a copy of the original parse loop, which replaced placeholders text-wide with {@link StringUtils#replace(String, String, String)},
 * on generated texts where the original loop was correct.
 *
 * <p>The original loop depended on the order of placeholders when the output of a placeholder got spliced into the raw text of another
 * placeholder, and converted formatting codes in placeholder values, so the generated texts avoid both: outputs have no quotes, braces,
 * {@code &} or {@code $$}, placeholders that keep their raw text only stand at the top level, and placeholders whose output contains
 * placeholders are unique.</p>
 */
@ResourceLock("PlaceholderParser global state")
class PlaceholderParserDifferentialTest {

    private static final String[] TEXT_FRAGMENTS = {"a", "b c", " ", "  ", "%n%", ":", ",", "x-y", "1", "\n", "\t", "Hello World"};
    private static final String[] VALUE_FRAGMENTS = {"v", "w x", " ", "%n%", ":", ",", "7", "end ", "\n"};
    private static final int PLAIN_KEYS = 5;

    private PlaceholderParser.PlaceholderCachingController originalCachingController;
    private int uniqueKeys = 0;

    @BeforeEach
    void disableCaching() {
        this.originalCachingController = PlaceholderParser.getPlaceholderCachingController();
        PlaceholderParser.setPlaceholderCachingController(new PlaceholderParser.PlaceholderCachingController(() -> false, () -> 0L));
        for (int i = 0; i < PLAIN_KEYS; i++) TestPlaceholders.setValue("differential_plain_" + i, "plain " + i);
    }

    @AfterEach
    void restoreCaching() {
        PlaceholderParser.setPlaceholderCachingController(this.originalCachingController);
    }

    @Test
    void fixedCasesMatchTheOriginalLoop() {
        TestPlaceholders.setValue("differential_fixed_rescan", "r " + echo("deep") + " " + value("differential_plain_1"));
        String plain = value("differential_plain_0");
        String[] cases = {
                "",
                "no placeholders",
                echo("a"),
                plain + plain + " " + echo(plain) + " " + join(plain, echo(echo(plain))),
                join(echo("x"), value("differential_fixed_rescan")) + " " + value("differential_plain_1"),
                "{\"placeholder\":\"differential_unknown\"} " + echo("y") + " {\"placeholder\":\"\"} " + nullReplacement("n"),
                echo("a%n%b") + "%n%" + join(" spaced ", " value "),
                "{ \"placeholder\" : \"" + TestPlaceholders.echoIdentifier() + "\" , \"values\" : { \"text\" : \"spaced\" } } " + echo("z"),
                echo("x") + " {\"placeholder\":\"" + TestPlaceholders.echoIdentifier() + "\",\"values\":{\"text\":\"no end\"}"
        };
        for (String text : cases) assertMatchesOriginal(text);
    }

    @Test
    void generatedTextsMatchTheOriginalLoop() {
        Random random = new Random(0x5EEDL);
        for (int i = 0; i < 3000; i++) {
            StringBuilder text = new StringBuilder();
            int items = random.nextInt(9);
            for (int item = 0; item < items; item++) {
                if (random.nextInt(3) == 0) {
                    text.append(TEXT_FRAGMENTS[random.nextInt(TEXT_FRAGMENTS.length)]);
                } else {
                    text.append(this.topLevelPlaceholder(random));
                }
            }
            // A start without end can swallow the placeholders after it into one unknown placeholder, so it only stands at the end
            if (random.nextInt(8) == 0) text.append("{\"placeholder\":\"differential_broken\",\"values\":{\"text\":\"x");
            assertMatchesOriginal(text.toString());
        }
    }

    @NotNull
    private String topLevelPlaceholder(@NotNull Random random) {
        switch (random.nextInt(10)) {
            case 0:
                return "{\"placeholder\":\"differential_unknown\",\"values\":{\"text\":\"" + this.plainValue(random) + "\"}}";
            case 1:
                return "{\"placeholder\":\"\"}";
            case 2:
                return nullReplacement(this.plainValue(random));
            default:
                return this.placeholder(random, 0);
        }
    }

    /** A placeholder whose final output is plain text. */
    @NotNull
    private String placeholder(@NotNull Random random, int depth) {
        int kind = random.nextInt((depth >= 3) ? 2 : 5);
        switch (kind) {
            case 0:
                return value("differential_plain_" + random.nextInt(PLAIN_KEYS));
            case 1:
                return echo(this.plainValue(random));
            case 2:
                return echo(this.valueText(random, depth + 1));
            case 3:
                return join(this.valueText(random, depth + 1), this.valueText(random, depth + 1));
            default:
                // Its output contains placeholders, so it needs more passes. It is unique, so its raw text never gets replaced inside another placeholder.
                String key = "differential_rescan_" + (this.uniqueKeys++);
                TestPlaceholders.setValue(key, this.plainValue(random) + this.placeholder(random, depth + 1));
                return value(key);
        }
    }

    @NotNull
    private String valueText(@NotNull Random random, int depth) {
        StringBuilder value = new StringBuilder();
        int items = 1 + random.nextInt(3);
        for (int item = 0; item < items; item++) {
            if (random.nextBoolean()) {
                value.append(VALUE_FRAGMENTS[random.nextInt(VALUE_FRAGMENTS.length)]);
            } else {
                value.append(this.placeholder(random, depth));
            }
        }
        return value.toString();
    }

    @NotNull
    private String plainValue(@NotNull Random random) {
        return VALUE_FRAGMENTS[random.nextInt(VALUE_FRAGMENTS.length)];
    }

    private static void assertMatchesOriginal(@NotNull String text) {
        String expected = originalReplacePlaceholders(text, new HashMap<>(), ParsingProcessorRegistry.snapshot());
        assertEquals(expected, PlaceholderParser.replacePlaceholders(text), text);
    }

    /**
     * A copy of the original parse loop, without the result cache and the length limit.
     */
    @NotNull
    private static String originalReplacePlaceholders(@Nullable String in, @NotNull HashMap<String, String> parsed, @NotNull ParsingProcessorRegistry.Snapshot processors) {
        if (in == null) return "";
        in = processors.processBeforeReplacement(in);
        if (!PlaceholderParser.containsPlaceholders(in)) return in;
        int hash = in.hashCode();
        while (true) {
            for (PlaceholderParser.ParsedPlaceholder p : Lists.reverse(PlaceholderParser.findPlaceholders(in, parsed))) {
                String replacement = parsed.get(p.placeholderString);
                if (replacement == null) {
                    replacement = originalGetReplacement(p, parsed, processors);
                    parsed.put(p.placeholderString, replacement);
                }
                in = StringUtils.replace(in, p.placeholderString, replacement);
            }
            int hashNew = in.hashCode();
            if (hashNew == hash) break;
            hash = hashNew;
        }
        return processors.processAfterReplacement(in, ParsingProcessorRegistry.NO_PROCESSOR_ID);
    }

    @Nullable
    private static String originalGetReplacement(@NotNull PlaceholderParser.ParsedPlaceholder p, @NotNull HashMap<String, String> parsed, @NotNull ParsingProcessorRegistry.Snapshot processors) {
        String identifier = p.getIdentifier();
        if (identifier == null) return p.placeholderString;
        Placeholder placeholder = p.getPlaceholder();
        if (placeholder == null) return p.placeholderString;
        if (!placeholder.checkAsync()) return p.placeholderString;
        HashMap<String, String> values = p.getValues();
        if (values == null) return p.placeholderString;
        DeserializedPlaceholderString deserialized = new DeserializedPlaceholderString(identifier, null, p.placeholderString);
        for (Map.Entry<String, String> value : values.entrySet()) {
            deserialized.values.put(value.getKey(), originalReplacePlaceholders(value.getValue(), parsed, processors));
        }
        return placeholder.getReplacementFor(deserialized);
    }

}
