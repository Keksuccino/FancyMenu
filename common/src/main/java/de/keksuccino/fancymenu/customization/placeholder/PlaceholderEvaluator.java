//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

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
 *     <li>Its {@link PlaceholderTemplate} gets replaced. The result gets replaced again and again, until a pass doesn't change it anymore,
 *     because placeholders can return placeholders.</li>
 *     <li>The AFTER processors run on it.</li>
 *     <li>The result gets cached, unless it contains placeholders that got skipped because they can only get replaced on the main thread.</li>
 * </ol>
 *
 * <p>Identical placeholders of one parse get replaced once and share their replacement, across all values and passes
 * (the memo of {@link EvaluationState}).</p>
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
    private static final int MAX_BUILDER_CAPACITY = Integer.MAX_VALUE - 8;

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
     * @param text The text before any processor ran on it.
     * @param slot The compiled value, if the text is the raw text of a value of a {@link PlaceholderTemplate}.
     * @param context The context of the parse, which is the context of the state if there is one.
     * @param state The state of the running parse, or null if this starts a new parse.
     */
    @NotNull
    static String replacePlaceholders(@NotNull String text, @Nullable PlaceholderTemplate.ValueSlot slot, @NotNull ParsingContext context, @Nullable EvaluationState state) {

        if (context.limitTextLength() && (text.length() >= PlaceholderParser.MAX_TEXT_LENGTH)) return PlaceholderParser.TOO_LONG_TO_PARSE_ERROR_MESSAGE;

        // Compiled values without placeholders or $$ references never get their result cached, so they skip the lookup.
        // Cache hits deliberately skip BEFORE processors, so insertion must use this exact pre-processor key too.
        boolean plainValue = (slot != null) && (slot.template == null);
        if (context.cachePlaceholders() && !plainValue) {
            String cached = PlaceholderResultCache.get(text, context);
            if (cached != null) return cached;
        }

        String processed = context.processors().processBeforeReplacement(text);

        PlaceholderTemplate template;
        if ((slot != null) && slot.isProcessedText(processed)) {
            if (slot.template == null) return processed;
            template = slot.template;
        } else {
            if (!passesGate(processed)) return processed;
            template = PlaceholderTemplate.get(processed, context);
        }

        if (state == null) state = new EvaluationState(context, null);
        int asyncSkips = state.asyncSkips;
        String result = replaceTemplate(template, state);

        result = context.processors().processAfterReplacement(result, context.preserveFormattingCodes() ? PlaceholderParser.FORMATTING_CODE_PROCESSOR_ID : ParsingProcessorRegistry.NO_PROCESSOR_ID);

        // The main thread must never get a result that still contains placeholders that got skipped because they can only get replaced on the main thread
        if (context.cachePlaceholders() && (state.asyncSkips == asyncSkips)) PlaceholderResultCache.put(text, result, context);

        return result;

    }

    /**
     * Replaces one placeholder found by {@link PlaceholderParser#findPlaceholders(String, HashMap)}, see {@link PlaceholderParser.ParsedPlaceholder#getReplacement()}.
     * Its values come from {@link PlaceholderParser.ParsedPlaceholder#getValues()}, so subclasses can change them.
     */
    @NotNull
    static String replaceParsedPlaceholder(@NotNull PlaceholderParser.ParsedPlaceholder parsed, @NotNull Placeholder placeholder, @NotNull String identifier, @NotNull HashMap<String, String> memo, @NotNull ParsingContext context) {
        String raw = parsed.placeholderString;
        if (!placeholder.checkAsync()) return raw;
        HashMap<String, String> values = parsed.getValues();
        if (values == null) return raw;
        EvaluationState state = new EvaluationState(context, memo);
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString(identifier, null, raw);
        for (Map.Entry<String, String> value : values.entrySet()) {
            String valueText = value.getValue();
            dps.values.put(value.getKey(), (valueText != null) ? replacePlaceholders(valueText, null, context, state) : EMPTY_STRING);
        }
        return getReplacement(placeholder, identifier, raw, dps);
    }

    /**
     * Replaces the template, then parses the result again and again until a pass doesn't change it anymore.
     *
     * <p>There is deliberately no limit for the number of passes, complex layouts can need many of them.
     * Placeholders that return each other in a cycle never stop, which is a known limitation.</p>
     */
    @NotNull
    private static String replaceTemplate(@NotNull PlaceholderTemplate template, @NotNull EvaluationState state) {
        String text = template.source;
        String result = replacePass(template, state);
        while (!result.equals(text)) {
            text = result;
            // Without a placeholder start, the next pass can't change anything
            if (!PlaceholderScanner.containsPlaceholderStart(text)) break;
            // Pass results are new texts that often change from parse to parse, so caching their templates would only push
            // stable templates out of the cache, and hashing long new texts for a lookup takes longer than compiling them
            result = replacePass(PlaceholderTemplate.compile(text, state.context), state);
        }
        return result;
    }

    /**
     * Replaces every node of the template once and puts the replacements between the text of the template, by position.
     *
     * @return The source of the template if nothing got replaced.
     */
    @NotNull
    private static String replacePass(@NotNull PlaceholderTemplate template, @NotNull EvaluationState state) {
        String source = template.source;
        PlaceholderTemplate.Node[] nodes = template.nodes;
        int count = nodes.length;
        if (count == 0) return source;
        String[] replacements = new String[count];
        boolean replaced = false;
        long length = source.length();
        // Placeholders get replaced from the last to the first one. Keep this order, placeholders with side effects can depend on it.
        for (int i = count - 1; i >= 0; i--) {
            PlaceholderTemplate.Node node = nodes[i];
            String replacement = replaceNode(node, state);
            replacements[i] = replacement;
            if (replacement != node.raw) replaced = true;
            length += replacement.length() - (node.end - node.start);
        }
        if (!replaced) return source;
        if ((count == 1) && (nodes[0].start == 0) && (nodes[0].end == source.length())) return replacements[0];
        StringBuilder builder = new StringBuilder((int) Math.min(length, MAX_BUILDER_CAPACITY));
        int position = 0;
        for (int i = 0; i < count; i++) {
            PlaceholderTemplate.Node node = nodes[i];
            builder.append(source, position, node.start).append(replacements[i]);
            position = node.end;
        }
        return builder.append(source, position, source.length()).toString();
    }

    @NotNull
    private static String replaceNode(@NotNull PlaceholderTemplate.Node node, @NotNull EvaluationState state) {
        if (state.memo != null) {
            String memoized = state.memo.get(node.raw);
            if (memoized != null) return memoized;
        }
        if (!node.placeholder.checkAsync()) {
            state.asyncSkips++;
            return node.raw;
        }
        int asyncSkips = state.asyncSkips;
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString(node.identifier, null, node.raw);
        for (PlaceholderTemplate.ValueSlot slot : node.values) {
            dps.values.put(slot.name, replacePlaceholders(slot.raw, slot, state.context, state));
        }
        String replacement = getReplacement(node.placeholder, node.identifier, node.raw, dps);
        // Replacements that miss skipped main-thread-only placeholders must not get shared
        if (state.asyncSkips == asyncSkips) {
            if (state.memo == null) state.memo = new HashMap<>();
            state.memo.put(node.raw, replacement);
        }
        return replacement;
    }

    /**
     * Asks the placeholder for its replacement. A placeholder that fails or returns null keeps its raw text.
     */
    @NotNull
    private static String getReplacement(@NotNull Placeholder placeholder, @NotNull String identifier, @NotNull String raw, @NotNull DeserializedPlaceholderString dps) {
        try {
            String replacement = placeholder.getReplacementFor(dps);
            return (replacement != null) ? replacement : raw;
        } catch (Exception ex) {
            logError("[FANCYMENU] Failed to replace placeholder: " + identifier, ex);
            return raw;
        }
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
     * The state of one parse, shared by all of its values and passes. Only used by the thread that parses.
     */
    static final class EvaluationState {

        @NotNull
        final ParsingContext context;
        /** Replacements of the placeholders of the parse, by raw placeholder text. */
        @Nullable
        HashMap<String, String> memo;
        /** How many placeholders got skipped because they can only get replaced on the main thread. Only grows. */
        int asyncSkips;

        EvaluationState(@NotNull ParsingContext context, @Nullable HashMap<String, String> memo) {
            this.context = context;
            this.memo = memo;
        }

    }

    /**
     * @param limitTextLength If texts of MAX_TEXT_LENGTH characters or more get answered with an error text. This includes the values of nested placeholders.
     */
    record ParsingContext(@NotNull ParsingProcessorRegistry.Snapshot processors, long cachingControllerRevision, boolean cachePlaceholders, long cachingDurationMillis, boolean preserveFormattingCodes, boolean limitTextLength) {
    }

}
