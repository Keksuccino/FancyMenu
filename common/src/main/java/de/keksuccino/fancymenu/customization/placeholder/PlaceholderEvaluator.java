//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.customization.FreshValuesScope;
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
 *     <li>A cached result of the text gets returned (see {@link PlaceholderResultCache}), unless the parse runs inside a {@link FreshValuesScope}.</li>
 *     <li>The BEFORE processors run on it. If it contains no placeholders and no {@code $$} references after that (see {@link #passesGate(String)}),
 *     it gets returned as it is, without running the AFTER processors.</li>
 *     <li>Its {@link PlaceholderTemplate} gets replaced. The result gets replaced again and again, until a pass doesn't change it anymore,
 *     because placeholders can return placeholders.</li>
 *     <li>The AFTER processors run on it. Values skip the formatting-code conversion, so placeholders get their values with the original
 *     {@code &} characters (URLs and file paths need them). The text they end up in still gets converted.</li>
 *     <li>The result gets cached, also inside a {@link FreshValuesScope}, so rendering gets the fresh result too. Results that contain placeholders
 *     that got skipped because they can only get replaced on the main thread don't get cached. Values that replaced a placeholder evaluated
 *     per occurrence don't get cached either, so every occurrence of them gets its own value.</li>
 * </ol>
 *
 * <p>Identical placeholders of one parse get replaced once and share their replacement, across all values and passes
 * (the memo of {@link EvaluationState}). Placeholders that get evaluated per occurrence (see {@link Placeholder#isEvaluatedPerOccurrence(DeserializedPlaceholderString)})
 * and placeholders with such a placeholder in their values get replaced at every occurrence instead.</p>
 *
 * <p>Whether a parse runs inside a {@link FreshValuesScope} gets read once, when its {@link ParsingContext} gets created, and holds for all of
 * its values and passes. A placeholder found by {@link PlaceholderParser#findPlaceholders(String, HashMap)} reads it again every time it gets
 * replaced. A parse that a placeholder starts itself through {@link PlaceholderParser} while it gets replaced (Stringify does that) gets a
 * context of its own, but on the same thread, so it reads the same scope. The memo, compiled templates and the cache of
 * {@link #passesGate(String)} only depend on the text, so they stay in use inside a scope.</p>
 */
final class PlaceholderEvaluator {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long LOG_COOLDOWN_MS = 10000;
    private static final int LOG_COOLDOWN_MAX_ENTRIES = 256;
    // Errors of placeholders whose values can't be read contain their raw text, which can be as long as a whole text, see PlaceholderParser#MAX_TEXT_LENGTH.
    // An error heavier than this would never get its cooldown and get logged every frame.
    private static final long LOG_COOLDOWN_MAX_WEIGHT = 1_048_576L;
    private static final int CONTAINS_PLACEHOLDERS_MAX_ENTRIES = 2048;
    // Sized for PlaceholderParser#MAX_TEXT_LENGTH, see there
    private static final long CONTAINS_PLACEHOLDERS_MAX_WEIGHT = 2_097_152L;
    private static final long CACHE_ENTRY_OVERHEAD_WEIGHT = 64L;
    private static final int MIN_LENGTH_FOR_PARSING = 8;
    private static final String SHORT_VARIABLE_PLACEHOLDER_PREFIX = "$$";
    private static final String EMPTY_STRING = "";
    private static final int MAX_BUILDER_CAPACITY = Integer.MAX_VALUE - 8;

    // String length is measured in retained UTF-16 code units, with a fixed allowance for each entry's objects.
    static final LogCooldownTracker LOG_COOLDOWN = new LogCooldownTracker(LOG_COOLDOWN_MS, LOG_COOLDOWN_MAX_ENTRIES, LOG_COOLDOWN_MAX_WEIGHT);
    static final BoundedConcurrentCache<String, Boolean> CONTAINS_PLACEHOLDERS = new BoundedConcurrentCache<>(CONTAINS_PLACEHOLDERS_MAX_ENTRIES, CONTAINS_PLACEHOLDERS_MAX_WEIGHT, (text, ignored) -> CACHE_ENTRY_OVERHEAD_WEIGHT + text.length());

    private PlaceholderEvaluator() {
    }

    /**
     * Creates the context of a new parse. It holds everything that must stay the same for the whole parse, including whether it may read
     * cached results, which depends on the {@link FreshValuesScope} of the current thread at this moment.
     */
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
        return new ParsingContext(processors, caching.revision(), cachePlaceholders, readsCachedResults(cachePlaceholders), cachingDurationMillis, preserveFormattingCodes, limitTextLength);
    }

    /**
     * Script logic reads values right after changing them, so inside a {@link FreshValuesScope} parses must not read results
     * cached for rendering. They still store their results, see {@link ParsingContext}.
     * The scope only gets checked while caching is on, so parses without caching never pay for it.
     */
    private static boolean readsCachedResults(boolean cachePlaceholders) {
        return cachePlaceholders && !FreshValuesScope.isActive();
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
     * @param value If the text is the value of a placeholder.
     * @param context The context of the parse, which is the context of the state if there is one.
     * @param state The state of the running parse, or null if this starts a new parse.
     */
    @NotNull
    static String replacePlaceholders(@NotNull String text, @Nullable PlaceholderTemplate.ValueSlot slot, boolean value, @NotNull ParsingContext context, @Nullable EvaluationState state) {

        if (context.limitTextLength() && PlaceholderParser.isTooLongToParse(text)) return PlaceholderParser.getTextTooLongMessage();

        // Compiled values without placeholders or $$ references never get their result cached, so they skip the lookup.
        // Cache hits deliberately skip BEFORE processors, so insertion must use this exact pre-processor key too.
        // Inside a fresh-values scope, the lookup gets skipped, but the result still gets stored at the end.
        boolean plainValue = (slot != null) && (slot.template == null);
        if (context.readCachedResults() && !plainValue) {
            String cached = PlaceholderResultCache.get(text, value, context);
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
        int perOccurrenceReplacements = state.perOccurrenceReplacements;
        String result = replaceTemplate(template, state);

        result = context.processors().processAfterReplacement(result, (value || context.preserveFormattingCodes()) ? PlaceholderParser.FORMATTING_CODE_PROCESSOR_ID : ParsingProcessorRegistry.NO_PROCESSOR_ID);

        // The main thread must never get a result that still contains placeholders that got skipped because they can only get replaced on the main thread.
        // Values that replaced a placeholder evaluated per occurrence don't get cached either, or the next occurrence of the same value would get the same result.
        // Whole texts still keep their per-occurrence values for the caching duration. The counters include nested values and later passes, because they only grow.
        boolean cacheable = (state.asyncSkips == asyncSkips) && (!value || (state.perOccurrenceReplacements == perOccurrenceReplacements));
        if (context.cachePlaceholders() && cacheable) PlaceholderResultCache.put(text, result, value, context);

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
        // The context got created when the placeholder got found, but the replacement can happen later, inside or outside
        // a fresh-values scope, or on another thread. Every replacement is a parse of its own, so it reads the scope of its own moment.
        EvaluationState state = new EvaluationState(context.withFreshValuesScopeOfCurrentThread(), memo);
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString(identifier, null, raw);
        for (Map.Entry<String, String> value : values.entrySet()) {
            String valueText = value.getValue();
            dps.values.put(value.getKey(), (valueText != null) ? replacePlaceholders(valueText, null, true, state.context, state) : EMPTY_STRING);
        }
        return getReplacement(placeholder, identifier, raw, dps, state);
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
        int perOccurrenceReplacements = state.perOccurrenceReplacements;
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString(node.identifier, null, node.raw);
        for (PlaceholderTemplate.ValueSlot slot : node.values) {
            dps.values.put(slot.name, replacePlaceholders(slot.raw, slot, true, state.context, state));
        }
        String replacement = getReplacement(node.placeholder, node.identifier, node.raw, dps, state);
        // Replacements that depend on their occurrence or miss skipped main-thread-only placeholders must not get shared
        if ((state.asyncSkips == asyncSkips) && (state.perOccurrenceReplacements == perOccurrenceReplacements)) {
            if (state.memo == null) state.memo = new HashMap<>();
            state.memo.put(node.raw, replacement);
        }
        return replacement;
    }

    /**
     * Asks the placeholder for its replacement. A placeholder that fails or returns null keeps its raw text.
     */
    @NotNull
    private static String getReplacement(@NotNull Placeholder placeholder, @NotNull String identifier, @NotNull String raw, @NotNull DeserializedPlaceholderString dps, @NotNull EvaluationState state) {
        try {
            if (placeholder.isEvaluatedPerOccurrence(dps)) state.perOccurrenceReplacements++;
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
        /** How many placeholders got replaced that get evaluated per occurrence. Only grows. */
        int perOccurrenceReplacements;

        EvaluationState(@NotNull ParsingContext context, @Nullable HashMap<String, String> memo) {
            this.context = context;
            this.memo = memo;
        }

    }

    /**
     * What stays the same for a whole parse, including all of its values and passes.
     *
     * @param cachePlaceholders If results get stored in the {@link PlaceholderResultCache}.
     * @param readCachedResults If results may come from the {@link PlaceholderResultCache}. Only true while caching is on and the parse
     *                          doesn't run inside a {@link FreshValuesScope}. Inside a scope, results still get stored, so rendering gets them too.
     * @param limitTextLength If texts longer than {@link PlaceholderParser#MAX_TEXT_LENGTH} get answered with an error text. This includes the values of nested placeholders.
     */
    record ParsingContext(@NotNull ParsingProcessorRegistry.Snapshot processors, long cachingControllerRevision, boolean cachePlaceholders, boolean readCachedResults, long cachingDurationMillis, boolean preserveFormattingCodes, boolean limitTextLength) {

        /**
         * @return This context, or a copy of it with {@link #readCachedResults()} for the {@link FreshValuesScope} the current thread is in right now.
         */
        @NotNull
        ParsingContext withFreshValuesScopeOfCurrentThread() {
            boolean readCachedResults = readsCachedResults(this.cachePlaceholders);
            if (readCachedResults == this.readCachedResults) return this;
            return new ParsingContext(this.processors, this.cachingControllerRevision, this.cachePlaceholders, readCachedResults, this.cachingDurationMillis, this.preserveFormattingCodes, this.limitTextLength);
        }

    }

}
