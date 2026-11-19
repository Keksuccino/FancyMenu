//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Replaces the placeholders of texts and of placeholder values, see {@link PlaceholderParser}.
 *
 * <p>A parse of a text goes through these steps. Placeholder values go through the same steps when their placeholder gets replaced:</p>
 * <ol>
 *     <li>A cached result of the text gets returned (see {@link PlaceholderResultCache}).</li>
 *     <li>The BEFORE processors run on it. If it contains no placeholders and no {@code $$} references after that (see {@link #passesGate(String)}),
 *     it gets returned as it is, without running the AFTER processors.</li>
 *     <li>Its placeholders get replaced. The result gets replaced again and again, until a pass doesn't change it anymore,
 *     because placeholders can return placeholders.</li>
 *     <li>The AFTER processors run on it.</li>
 *     <li>The result gets cached.</li>
 * </ol>
 *
 * <p>Identical placeholders of one parse get replaced once and share their replacement, across all values and passes.</p>
 */
final class PlaceholderEvaluator {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long LOG_COOLDOWN_MS = 10000;
    private static final int LOG_COOLDOWN_MAX_ENTRIES = 256;
    private static final long LOG_COOLDOWN_MAX_WEIGHT = 262_144L;
    private static final int CONTAINS_PLACEHOLDERS_MAX_ENTRIES = 2048;
    private static final long CONTAINS_PLACEHOLDERS_MAX_WEIGHT = 1_048_576L;
    private static final long CACHE_ENTRY_OVERHEAD_WEIGHT = 64L;
    private static final int MIN_LENGTH_FOR_PARSING = 8;
    private static final String SHORT_VARIABLE_PLACEHOLDER_PREFIX = "$$";
    private static final String EMPTY_STRING = "";

    // String length is measured in retained UTF-16 code units, with a fixed allowance for each entry's objects.
    private static final LogCooldownTracker LOG_COOLDOWN = new LogCooldownTracker(LOG_COOLDOWN_MS, LOG_COOLDOWN_MAX_ENTRIES, LOG_COOLDOWN_MAX_WEIGHT);
    private static final BoundedConcurrentCache<String, Boolean> CONTAINS_PLACEHOLDERS = new BoundedConcurrentCache<>(CONTAINS_PLACEHOLDERS_MAX_ENTRIES, CONTAINS_PLACEHOLDERS_MAX_WEIGHT, (text, ignored) -> CACHE_ENTRY_OVERHEAD_WEIGHT + text.length());

    private PlaceholderEvaluator() {
    }

    @NotNull
    static ParsingContext createParsingContext(boolean preserveFormattingCodes, boolean limitTextLength) {
        ParsingProcessorRegistry.Snapshot processors = ParsingProcessorRegistry.snapshot();
        PlaceholderResultCache.CachingControllerSnapshot caching = PlaceholderResultCache.cachingControllerSnapshot();
        boolean cachePlaceholders = false;
        long cachingDurationMillis = 0L;
        if (!preserveFormattingCodes && caching.controller().shouldCachePlaceholders().get()) {
            cachingDurationMillis = caching.controller().cachingDurationMillis().get();
            cachePlaceholders = cachingDurationMillis > 0L;
        }
        return new ParsingContext(processors, caching.revision(), cachePlaceholders, cachingDurationMillis, preserveFormattingCodes, limitTextLength);
    }

    /**
     * See {@link PlaceholderParser#containsPlaceholders(String)}.
     */
    static boolean containsPlaceholders(@Nullable String in) {
        if (in == null) return false;
        if ((in.length() >= MIN_LENGTH_FOR_PARSING) && PlaceholderScanner.containsPlaceholderStart(in)) return true;
        return containsVariableReference(in);
    }

    /**
     * Checks if the text contains {@code $$} followed by at least one character, which is the shortest possible {@code $$name} reference.
     * Variable names can contain any character, so this doesn't check the name itself.
     */
    private static boolean containsVariableReference(@NotNull String in) {
        // Only the first "$$" needs a check: if nothing follows it, it ends the text, so there is no other "$$" after it
        int prefixIndex = in.indexOf(SHORT_VARIABLE_PLACEHOLDER_PREFIX);
        return (prefixIndex != -1) && ((prefixIndex + SHORT_VARIABLE_PLACEHOLDER_PREFIX.length()) < in.length());
    }

    /**
     * If the text needs to get parsed, so if it contains placeholders or {@code $$} references (see {@link #containsPlaceholders(String)}).
     * The answers for longer texts get cached, because looking up the same text instance again is far cheaper than scanning it again.
     */
    static boolean passesGate(@NotNull String text) {
        // Texts this short can only contain variable references, and checking them directly is cheaper than a cache lookup
        if (text.length() < MIN_LENGTH_FOR_PARSING) return containsVariableReference(text);
        Boolean containsPlaceholders = CONTAINS_PLACEHOLDERS.get(text);
        if (containsPlaceholders == null) {
            containsPlaceholders = containsPlaceholders(text);
            CONTAINS_PLACEHOLDERS.put(text, containsPlaceholders);
        }
        return containsPlaceholders;
    }

    /**
     * Replaces the placeholders of a text or of a placeholder value, see the class comment.
     *
     * @param in The text before any processor ran on it.
     * @param parsed Replacements of the placeholders of the running parse by their raw text, or null if this starts a new parse.
     */
    @NotNull
    static String replacePlaceholders(@NotNull String in, @Nullable HashMap<String, String> parsed, @NotNull ParsingContext context) {

        if (context.limitTextLength() && (in.length() >= PlaceholderParser.MAX_TEXT_LENGTH)) return PlaceholderParser.TOO_LONG_TO_PARSE_ERROR_MESSAGE;

        // Cache hits deliberately skip BEFORE processors, so insertion must use this exact pre-processor key too.
        String cacheKey = in;
        if (context.cachePlaceholders()) {
            String cached = PlaceholderResultCache.get(cacheKey, context);
            if (cached != null) return cached;
        }

        in = context.processors().processBeforeReplacement(in);

        if (!passesGate(in)) return in;

        // Used to cache replacements for already parsed placeholders, so they can get reused to improve performance
        if (parsed == null) parsed = new HashMap<>();

        int hash = in.hashCode();
        while (true) {
            // Reverse the list to start replacing from the end of the String, so all nested placeholders get replaced first
            for (PlaceholderParser.ParsedPlaceholder p : Lists.reverse(PlaceholderParser.findPlaceholders(in, parsed, context))) {
                String replacement = parsed.get(p.placeholderString);
                if (replacement == null) {
                    replacement = p.getReplacement();
                    parsed.put(p.placeholderString, replacement);
                }
                in = StringUtils.replace(in, p.placeholderString, replacement);
            }
            int hashNew = in.hashCode();
            if (hashNew == hash) break;
            hash = hashNew;
        }

        in = context.processors().processAfterReplacement(in, context.preserveFormattingCodes() ? PlaceholderParser.FORMATTING_CODE_PROCESSOR_ID : ParsingProcessorRegistry.NO_PROCESSOR_ID);

        if (context.cachePlaceholders()) PlaceholderResultCache.put(cacheKey, in, context);

        return in;

    }

    /**
     * Replaces one placeholder found by {@link PlaceholderParser#findPlaceholders(String, HashMap)}, see {@link PlaceholderParser.ParsedPlaceholder#getReplacement()}.
     * Its values come from {@link PlaceholderParser.ParsedPlaceholder#getValues()}, so subclasses can change them.
     */
    static String replaceParsedPlaceholder(@NotNull PlaceholderParser.ParsedPlaceholder parsed, @NotNull Placeholder placeholder, @NotNull String identifier, @NotNull HashMap<String, String> memo, @NotNull ParsingContext context) {
        String raw = parsed.placeholderString;
        if (!placeholder.checkAsync()) return raw;
        HashMap<String, String> values = parsed.getValues();
        if (values == null) return raw;
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString(identifier, null, raw);
        for (Map.Entry<String, String> value : values.entrySet()) {
            String valueText = value.getValue();
            dps.values.put(value.getKey(), (valueText != null) ? replacePlaceholders(valueText, memo, context) : EMPTY_STRING);
        }
        return placeholder.getReplacementFor(dps);
    }

    /**
     * Logs the error at most once per cooldown, so errors of texts that get parsed every frame don't flood the log.
     * The error text is the cooldown key, so it must not contain anything that changes from parse to parse.
     */
    static void logError(@NotNull String error, @Nullable Exception ex) {
        if (!LOG_COOLDOWN.tryAcquire(error, System.currentTimeMillis())) return;
        if (ex != null) {
            LOGGER.error(error, ex);
        } else {
            LOGGER.error(error);
        }
    }

    /**
     * @param limitTextLength If texts of MAX_TEXT_LENGTH characters or more get answered with an error text. This includes the values of nested placeholders.
     */
    record ParsingContext(@NotNull ParsingProcessorRegistry.Snapshot processors, long cachingControllerRevision, boolean cachePlaceholders, long cachingDurationMillis, boolean preserveFormattingCodes, boolean limitTextLength) {
    }

}
