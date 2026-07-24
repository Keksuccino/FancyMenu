//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.variables.UserVariableSnapshot;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Supplier;

//PLACEHOLDER STRING EXAMPLES:
//{"placeholder":"ph_id","values":{"value_1":"{"placeholder":"ph_id_2"}","value_2":"content"}}
//{"placeholder":"ph_id_2"}

public class PlaceholderParser {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final long LOG_COOLDOWN_MS = 10000;
    private static final int LOG_COOLDOWN_MAX_ENTRIES = 256;
    private static final long LOG_COOLDOWN_MAX_WEIGHT = 262_144L;
    private static final int CONTAINS_PLACEHOLDERS_MAX_ENTRIES = 2048;
    private static final long CONTAINS_PLACEHOLDERS_MAX_WEIGHT = 1_048_576L;
    private static final int PLACEHOLDER_CACHE_MAX_ENTRIES = 512;
    private static final long PLACEHOLDER_CACHE_MAX_WEIGHT = 4_194_304L;
    private static final long CACHE_ENTRY_OVERHEAD_WEIGHT = 64L;

    // String length is measured in retained UTF-16 code units, with a fixed allowance for each entry's objects.
    private static final LogCooldownTracker LOG_COOLDOWN = new LogCooldownTracker(LOG_COOLDOWN_MS, LOG_COOLDOWN_MAX_ENTRIES, LOG_COOLDOWN_MAX_WEIGHT);
    private static final BoundedConcurrentCache<String, Boolean> CONTAINS_PLACEHOLDERS = new BoundedConcurrentCache<>(CONTAINS_PLACEHOLDERS_MAX_ENTRIES, CONTAINS_PLACEHOLDERS_MAX_WEIGHT, (text, ignored) -> CACHE_ENTRY_OVERHEAD_WEIGHT + text.length());
    private static final BoundedConcurrentCache<String, CachedPlaceholder> PLACEHOLDER_CACHE = new BoundedConcurrentCache<>(PLACEHOLDER_CACHE_MAX_ENTRIES, PLACEHOLDER_CACHE_MAX_WEIGHT, (text, cached) -> CACHE_ENTRY_OVERHEAD_WEIGHT + text.length() + cached.replacement().length());
    private static final Object PARSING_PROCESSOR_REGISTRATION_LOCK = new Object();
    private static final Map<Long, ConsumingSupplier<String, String>> PARSING_PROCESSORS_BEFORE_REPLACING_PLACEHOLDERS = new LinkedHashMap<>();
    private static final Map<Long, ConsumingSupplier<String, String>> PARSING_PROCESSORS_AFTER_REPLACING_PLACEHOLDERS = new LinkedHashMap<>();
    private static volatile ParsingProcessorSnapshot parsingProcessorSnapshot = new ParsingProcessorSnapshot(0L, List.of(), List.of());
    private static final Object CACHING_CONTROLLER_LOCK = new Object();

    private static final int MIN_LENGTH_FOR_PARSING = 8;
    private static final int MAX_TEXT_LENGTH = 17000;
    private static final String PLACEHOLDER_PREFIX = "{\"placeholder\":\"";
    private static final String EMPTY_STRING = "";
    private static final char OPEN_CURLY_BRACKETS_CHAR = '{';
    private static final char CLOSE_CURLY_BRACKETS_CHAR = '}';
    private static final String FORMATTING_PREFIX_AND = "&";
    private static final String FORMATTING_PREFIX_PARAGRAPH = "§";
    private static final String SHORT_VARIABLE_PLACEHOLDER_PREFIX = "$$";
    private static final char DOLLAR_CHAR = '$';
    private static final String APOSTROPHE = "\"";
    private static final char APOSTROPHE_CHAR = '\"';
    private static final char NEWLINE_CHAR = '\n';
    private static final char BACKSLASH_CHAR = '\\';
    private static final char SPACE_CHAR = ' ';
    private static final char TAB_CHAR = '\t';
    private static final char CARRIAGE_RETURN_CHAR = '\r';
    private static final char PERCENT_CHAR = '%';
    private static final char LOWERCASE_N_CHAR = 'n';
    private static final String COMMA = ",";
    private static final String COMMA_WRAPPED_IN_APOSTROPHES = "\",\"";
    private static final String COLON_WRAPPED_IN_APOSTROPHES = "\":\"";
    private static final String PERCENT_NEWLINE_CODE = "%n%";
    private static final String TOO_LONG_TO_PARSE_ERROR_MESSAGE = "ERROR: Text too long to parse placeholders! 17,000 characters at max!";

    private static long processorId = 0L;
    private static long processorRevision = 0L;
    private static long cachingControllerRevision = 0L;
    private static final long FORMATTING_CODE_PROCESSOR_ID;
    private static volatile CachingControllerSnapshot cachingControllerSnapshot = new CachingControllerSnapshot(0L, new PlaceholderCachingController(() -> true, () -> 30L));

    static {

        setPlaceholderCachingController(new PlaceholderCachingController(
                () -> FancyMenu.getOptions().placeholderCachingDurationMs.getValue() > 0,
                () -> FancyMenu.getOptions().placeholderCachingDurationMs.getValue()
        ));

        // Text editor multi-line placeholder compression
        // Can be done for multi-line strings too, because the method only replaces codes instead of actual spaces or newlines
        addParsingProcessor(ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS, TextEditorWindowBody::compileSingleLineString);

        // Minecraft Formatting Codes
        FORMATTING_CODE_PROCESSOR_ID = addParsingProcessor(ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, in -> {
            return TextFormattingUtils.replaceFormattingCodes(in, FORMATTING_PREFIX_AND, FORMATTING_PREFIX_PARAGRAPH);
        });

        // Replace generic reference of FancyMenu's variables ($$variable_name instead of using the "Get Variable" placeholder)
        addParsingProcessor(ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, in -> {
            String replaced = in;
            int index = 0;
            List<UserVariableSnapshot> variables = VariableHandler.getVariableSnapshots();
            for (char c : in.toCharArray()) {
                if (c == DOLLAR_CHAR) {
                    String sub = StringUtils.substring(in, index);
                    if (StringUtils.startsWith(sub, SHORT_VARIABLE_PLACEHOLDER_PREFIX)) {
                        for (UserVariableSnapshot variable : variables) {
                            if (StringUtils.startsWith(sub, SHORT_VARIABLE_PLACEHOLDER_PREFIX + variable.name())) {
                                replaced = StringUtils.replace(replaced, SHORT_VARIABLE_PLACEHOLDER_PREFIX + variable.name(), variable.value());
                                break;
                            }
                        }
                    }
                }
                index++;
            }
            return replaced;
        });

    }

    /**
     * Adds a parsing processor. The timing controls if the processor should get executed before or after replacing placeholders in the string.
     *
     * <p>{@code ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS} processors get executed if the input text is not null, is not too long to get parsed and is not currently in the cache.
     *
     * <p>{@code ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS} processors only get executed if the input text contains placeholders and the system successfully replaced them.
     *
     * @return The unique ID of the processor.
     */
    public static long addParsingProcessor(@NotNull ParsingProcessorTiming timing, @NotNull ConsumingSupplier<String, String> processor) {
        Objects.requireNonNull(timing);
        Objects.requireNonNull(processor);
        synchronized (PARSING_PROCESSOR_REGISTRATION_LOCK) {
            long id = Math.incrementExact(processorId);
            processorId = id;
            if (timing == ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS) {
                PARSING_PROCESSORS_BEFORE_REPLACING_PLACEHOLDERS.put(id, processor);
            } else {
                PARSING_PROCESSORS_AFTER_REPLACING_PLACEHOLDERS.put(id, processor);
            }
            publishParsingProcessorSnapshotLocked();
            return id;
        }
    }

    public static void removeParsingProcessor(long id) {
        synchronized (PARSING_PROCESSOR_REGISTRATION_LOCK) {
            boolean removedBefore = PARSING_PROCESSORS_BEFORE_REPLACING_PLACEHOLDERS.remove(id) != null;
            boolean removedAfter = PARSING_PROCESSORS_AFTER_REPLACING_PLACEHOLDERS.remove(id) != null;
            if (removedBefore || removedAfter) publishParsingProcessorSnapshotLocked();
        }
    }

    @NotNull
    public static PlaceholderCachingController getPlaceholderCachingController() {
        return cachingControllerSnapshot.controller();
    }

    public static void setPlaceholderCachingController(@NotNull PlaceholderCachingController cachingController) {
        Objects.requireNonNull(cachingController);
        synchronized (CACHING_CONTROLLER_LOCK) {
            long revision = Math.incrementExact(cachingControllerRevision);
            cachingControllerRevision = revision;
            cachingControllerSnapshot = new CachingControllerSnapshot(revision, cachingController);
        }
    }

    public static boolean isCachingPlaceholders() {
        PlaceholderCachingController controller = cachingControllerSnapshot.controller();
        return controller.shouldCachePlaceholders().get();
    }

    public static long getPlaceholderCachingDurationMs() {
        PlaceholderCachingController controller = cachingControllerSnapshot.controller();
        return controller.cachingDurationMillis().get();
    }

    /**
     * Registration mutates the ordered maps only while holding their lock, then publishes both processor phases as
     * one immutable volatile snapshot. A parser can therefore retain one coherent revision without holding a lock
     * while integration code executes.
     */
    private static void publishParsingProcessorSnapshotLocked() {
        List<RegisteredParsingProcessor> beforeReplacement = createProcessorList(PARSING_PROCESSORS_BEFORE_REPLACING_PLACEHOLDERS);
        List<RegisteredParsingProcessor> afterReplacement = createProcessorList(PARSING_PROCESSORS_AFTER_REPLACING_PLACEHOLDERS);
        long revision = Math.incrementExact(processorRevision);
        processorRevision = revision;
        parsingProcessorSnapshot = new ParsingProcessorSnapshot(revision, beforeReplacement, afterReplacement);
    }

    @NotNull
    private static List<RegisteredParsingProcessor> createProcessorList(@NotNull Map<Long, ConsumingSupplier<String, String>> processors) {
        List<RegisteredParsingProcessor> snapshot = new ArrayList<>(processors.size());
        for (Map.Entry<Long, ConsumingSupplier<String, String>> processor : processors.entrySet()) {
            snapshot.add(new RegisteredParsingProcessor(processor.getKey(), processor.getValue()));
        }
        return List.copyOf(snapshot);
    }

    @NotNull
    private static ParsingContext createParsingContext(boolean preserveFormattingCodes) {
        ParsingProcessorSnapshot processors = parsingProcessorSnapshot;
        CachingControllerSnapshot caching = cachingControllerSnapshot;
        boolean cachePlaceholders = false;
        long cachingDurationMillis = 0L;
        if (!preserveFormattingCodes && caching.controller().shouldCachePlaceholders().get()) {
            cachingDurationMillis = caching.controller().cachingDurationMillis().get();
            cachePlaceholders = cachingDurationMillis > 0L;
        }
        return new ParsingContext(processors, caching.revision(), cachePlaceholders, cachingDurationMillis, preserveFormattingCodes);
    }

    /**
     * Simple check if the given {@link String} contains placeholders.<br>
     * This basically only checks if the {@link String} contains <code>&#123;"placeholder"</code>, so it's not 100% safe, but performance-friendly.
     */
    public static boolean containsPlaceholders(@Nullable String in) {
        if (in == null) return false;
        if (in.length() < MIN_LENGTH_FOR_PARSING) return false;
        return StringUtils.contains(in, "{\"placeholder\"");
    }

    /**
     * Replaces all placeholders in the given {@link String}.<br>
     * Since placeholders can contain variables that are not always the same, this task can be performance-intensive,
     * so you should keep in mind to not parse/update long texts too often in a short time frame.
     *
     * @param in The {@link String} to replace placeholders in.
     * @return The given {@link String} with all placeholders replaced.
     */
    @NotNull
    public static String replacePlaceholders(@Nullable String in) {
        return replacePlaceholders(in, null, false);
    }

    /**
     * Replaces placeholders while preserving formatting-code prefixes for a context-aware text parser.
     *
     * <p>All registered before/after processors still run except the global ampersand-to-section-sign converter.
     * This variant deliberately bypasses the shared result cache because cached strings produced by the default
     * mode have already had their formatting codes converted.</p>
     */
    @NotNull
    public static String replacePlaceholdersPreservingFormattingCodes(@Nullable String in) {
        return replacePlaceholders(in, null, true);
    }

    /**
     * Replaces all placeholders in the given {@link String}.<br>
     * Since placeholders can contain variables that are not always the same, this task can be performance-intensive,
     * so you should keep in mind to not parse/update long texts too often in a short time frame.
     *
     * @param in The {@link String} to replace placeholders in.
     * @param parsed The placeholder replacement cache.
     * @return The given {@link String} with all placeholders replaced.
     */
    @NotNull
    private static String replacePlaceholders(@Nullable String in, @Nullable HashMap<String, String> parsed, boolean preserveFormattingCodes) {
        if (in == null) return EMPTY_STRING;
        if (in.length() >= MAX_TEXT_LENGTH) return TOO_LONG_TO_PARSE_ERROR_MESSAGE;
        return replacePlaceholders(in, parsed, createParsingContext(preserveFormattingCodes));
    }

    @NotNull
    private static String replacePlaceholders(@Nullable String in, @Nullable HashMap<String, String> parsed, @NotNull ParsingContext context) {

        if (in == null) return EMPTY_STRING;

        if (in.length() >= MAX_TEXT_LENGTH) return TOO_LONG_TO_PARSE_ERROR_MESSAGE;

        // Cache hits deliberately skip BEFORE processors, so insertion must use this exact pre-processor key too.
        String cacheKey = in;
        if (context.cachePlaceholders()) {
            CachedPlaceholder cached = PLACEHOLDER_CACHE.get(cacheKey);
            if ((cached != null) && cached.isUsableFor(context, System.currentTimeMillis())) return cached.replacement();
        }

        in = processBeforeReplacement(in, context.processors());

        // If "in" is too short, just return here, because there can't be placeholders in it
        if (in.length() < MIN_LENGTH_FOR_PARSING) return in;

        Boolean containsPlaceholders = CONTAINS_PLACEHOLDERS.get(in);
        if (containsPlaceholders == null) {
            containsPlaceholders = containsPlaceholders(in);
            CONTAINS_PLACEHOLDERS.put(in, containsPlaceholders);
        }
        if (!containsPlaceholders) return in;

        // Used to cache replacements for already parsed placeholders, so they can get reused to improve performance
        if (parsed == null) parsed = new HashMap<>();

        int hash = in.hashCode();
        while (true) {
            // Reverse the list to start replacing from the end of the String, so all nested placeholders get replaced first
            for (ParsedPlaceholder p : Lists.reverse(findPlaceholders(in, parsed, context))) {
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

        in = processAfterReplacement(in, context);

        if (context.cachePlaceholders()) PLACEHOLDER_CACHE.put(cacheKey, new CachedPlaceholder(in, System.currentTimeMillis(), context.processors().revision(), context.cachingControllerRevision()));

        return in;

    }

    private static String processBeforeReplacement(@NotNull String in, @NotNull ParsingProcessorSnapshot processors) {
        for (RegisteredParsingProcessor processor : processors.beforeReplacement()) {
            in = processor.processor().get(in);
        }
        return in;
    }

    private static String processAfterReplacement(@NotNull String in, @NotNull ParsingContext context) {
        for (RegisteredParsingProcessor processor : context.processors().afterReplacement()) {
            if (context.preserveFormattingCodes() && (processor.id() == FORMATTING_CODE_PROCESSOR_ID)) continue;
            in = processor.processor().get(in);
        }
        return in;
    }

    /**
     * Finds all placeholders in a {@link String}.<br>
     * Will not find placeholders that get added to the {@link String} by replacing other placeholders.
     *
     * @param in The {@link String} to get the placeholders from.
     */
    @NotNull
    public static List<ParsedPlaceholder> findPlaceholders(@Nullable String in, @NotNull HashMap<String, String> parsed) {
        return findPlaceholders(in, parsed, createParsingContext(false));
    }

    @NotNull
    private static List<ParsedPlaceholder> findPlaceholders(@Nullable String in, @NotNull HashMap<String, String> parsed, @NotNull ParsingContext context) {
        List<ParsedPlaceholder> placeholders = new ArrayList<>();
        if (in == null) return placeholders;

        for (int i = 0; i < in.length(); i++) {
            if (in.charAt(i) == OPEN_CURLY_BRACKETS_CHAR) {
                // Found a potential start of a JSON object.
                // Let's find its corresponding closing bracket.
                String sub = StringUtils.substring(in, i);
                int endIndex = findPlaceholderEndIndex(sub, i); // Returns absolute index in 'in'

                if (endIndex != -1) {
                    // We found a matching '}' for the '{' at 'i'.
                    // This block is a candidate for being a placeholder.
                    String candidate = StringUtils.substring(in, i, endIndex + 1);

                    // To check if it's a real placeholder, we normalize it and check the prefix.
                    // This handles multi-line and whitespace variations.
                    String normalized = normalizePlaceholderString(candidate);

                    if (normalized.startsWith(PLACEHOLDER_PREFIX)) {
                        // It's a valid placeholder. Add it to our list.
                        // Note: We use the original 'candidate' string for the object,
                        // as that's what exists in the input string 'in'.
                        placeholders.add(new ParsedPlaceholder(candidate, i, endIndex + 1, parsed, context));

                        // Advance the loop counter past this placeholder to avoid
                        // parsing its contents as separate, new placeholders.
                        i = endIndex;
                    }
                }
            }
        }
        return placeholders;
    }

    private static int findPlaceholderEndIndex(@NotNull String placeholderStartSubString, int startIndex) {
        int currentIndex = startIndex;
        int depth = 0;
        boolean escaped = false;
        boolean inQuotes = false;
        for (char c : placeholderStartSubString.toCharArray()) {
            if (currentIndex != startIndex) { //skip first char
                if (!escaped && (c == APOSTROPHE_CHAR)) { //toggle quoting so braces inside values stay ignored
                    inQuotes = !inQuotes;
                } else if (!inQuotes) {
                    if (!escaped && (c == OPEN_CURLY_BRACKETS_CHAR)) {
                        depth++;
                    } else if (!escaped && (c == CLOSE_CURLY_BRACKETS_CHAR)) {
                        if (depth <= 0) {
                            return currentIndex;
                        }
                        depth--;
                    }
                }
            }
            if (escaped) {
                escaped = false;
            } else {
                escaped = (c == BACKSLASH_CHAR);
            }
            currentIndex++;
        }
        return -1;
    }

    /**
     * Normalizes a placeholder string by removing unnecessary whitespace while preserving quoted content.
     * This allows multi-line placeholders to be parsed correctly.
     */
    @NotNull
    private static String normalizePlaceholderString(@NotNull String placeholderString) {
        StringBuilder result = new StringBuilder(placeholderString.length());
        boolean inQuotes = false;
        boolean isEscaped = false;

        for (int i = 0; i < placeholderString.length(); i++) {
            char c = placeholderString.charAt(i);

            // Handle %n% first, as it's a multi-char sequence that can appear inside or outside quotes
            if (!isEscaped && i + 2 < placeholderString.length() &&
                    c == PERCENT_CHAR &&
                    placeholderString.charAt(i+1) == LOWERCASE_N_CHAR &&
                    placeholderString.charAt(i+2) == PERCENT_CHAR) {

                if (inQuotes) {
                    // Inside quotes, the code should be preserved.
                    result.append(PERCENT_NEWLINE_CODE);
                }
                // If not in quotes, it's treated as whitespace, so we do nothing and just skip it.
                i += 2;
                continue;
            }

            if (isEscaped) {
                // The previous character was a backslash, so append both it and the current character.
                result.append(BACKSLASH_CHAR);
                result.append(c);
                isEscaped = false;
                continue;
            }

            if (c == BACKSLASH_CHAR) {
                // This is an escape character. Set the flag and wait for the next character.
                isEscaped = true;
                continue;
            }

            if (c == APOSTROPHE_CHAR) {
                // Toggles the in/out of quotes state.
                inQuotes = !inQuotes;
                result.append(c);
                continue;
            }

            if (inQuotes) {
                // If we are inside quotes, append every character as is.
                result.append(c);
            } else {
                // If we are outside quotes, only append non-whitespace characters.
                if (!isWhitespace(c)) {
                    result.append(c);
                }
            }
        }

        return result.toString();
    }

    private static boolean isWhitespace(char c) {
        return c == SPACE_CHAR || c == TAB_CHAR || c == NEWLINE_CHAR || c == CARRIAGE_RETURN_CHAR;
    }

    private static void logError(@NotNull String error, @Nullable Exception ex) {
        if (!LOG_COOLDOWN.tryAcquire(error, System.currentTimeMillis())) return;
        if (ex != null) {
            LOGGER.error(error, ex);
        } else {
            LOGGER.error(error);
        }
    }

    public static class ParsedPlaceholder {

        public final String placeholderString;
        public final int startIndex;
        public final int endIndex;
        private final HashMap<String, String> parsed;
        private final ParsingContext context;
        private Integer hashcode;
        private String identifier;
        private boolean identifierFailed = false;
        private Placeholder placeholder;
        private boolean placeholderFailed = false;
        private String normalizedString;

        protected ParsedPlaceholder(@NotNull String placeholderString, int startIndex, int endIndex, @NotNull HashMap<String, String> parsed, boolean preserveFormattingCodes) {
            this(placeholderString, startIndex, endIndex, parsed, createParsingContext(preserveFormattingCodes));
        }

        private ParsedPlaceholder(@NotNull String placeholderString, int startIndex, int endIndex, @NotNull HashMap<String, String> parsed, @NotNull ParsingContext context) {
            this.placeholderString = placeholderString;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.parsed = parsed;
            this.context = context;
        }

        /**
         * Gets the normalized version of the placeholder string with whitespace cleaned up.
         */
        @NotNull
        private String getNormalizedString() {
            if (this.normalizedString == null) {
                this.normalizedString = normalizePlaceholderString(this.placeholderString);
            }
            return this.normalizedString;
        }

        @Nullable
        public String getIdentifier() {
            if (this.identifierFailed) return null;
            if (this.identifier != null) return this.identifier;
            try {
                String normalized = this.getNormalizedString();
                // Remove the prefix to get the rest
                String afterPrefix = StringUtils.substring(normalized, PLACEHOLDER_PREFIX.length());
                // Find the closing quote for the identifier
                int endQuoteIndex = afterPrefix.indexOf(APOSTROPHE_CHAR);
                if (endQuoteIndex > 0) {
                    this.identifier = afterPrefix.substring(0, endQuoteIndex);
                    return this.identifier;
                }
            } catch (Exception ex) {
                logError("[FANCYMENU] Failed to parse identifier of placeholder: " + this.placeholderString, ex);
                this.identifierFailed = true;
            }
            return null;
        }

        @NotNull
        public String getReplacement() {
            String identifier = this.getIdentifier();
            if (identifier == null) return this.placeholderString;
            Placeholder p = this.getPlaceholder();
            if (p == null) return this.placeholderString;
            if (!p.checkAsync()) return this.placeholderString;
            HashMap<String, String> values = this.getValues();
            if (values == null) return this.placeholderString;
            DeserializedPlaceholderString deserialized = new DeserializedPlaceholderString(identifier, null, this.placeholderString);
            for (Map.Entry<String, String> value : values.entrySet()) {
                deserialized.values.put(value.getKey(), replacePlaceholders(value.getValue(), this.parsed, this.context));
            }
            return p.getReplacementFor(deserialized);
        }

        @Nullable
        public HashMap<String, String> getValues() {
            HashMap<String, String> values = new HashMap<>();
            try {
                Placeholder placeholder = this.getPlaceholder();
                if (placeholder == null) {
                    return null;
                }
                if (!this.hasValues()) return values;
                String normalized = this.getNormalizedString();
                // Check if there's actually a comma (indicating values section exists)
                if (!normalized.contains(COMMA)) {
                    return values;
                }
                String[] parts = StringUtils.split(normalized, COMMA, 2);
                if (parts.length < 2) {
                    return null;
                }
                String valueString = COMMA + parts[1];
                int currentIndex = 0;
                int inValueDepth = 0;
                String currentValueName = null;
                int currentValueStartIndex = 0;
                for (char c : valueString.toCharArray()) {
                    if (currentIndex >= currentValueStartIndex) {
                        if (c == APOSTROPHE_CHAR) {
                            if (currentValueName != null) {
                                if ((inValueDepth == 0) && !isEscaped(valueString, currentIndex) && isEndOfValueContent(placeholder, valueString, currentIndex)) {
                                    String valueContent = StringUtils.substring(valueString, currentValueStartIndex, currentIndex);
                                    values.put(currentValueName, valueContent);
                                    currentValueName = null;
                                    currentValueStartIndex = 0;
                                }
                            } else {
                                currentValueName = getValueNameIfStartingWithValue(placeholder, StringUtils.substring(valueString, currentIndex));
                                if (currentValueName != null) {
                                    currentValueStartIndex = currentIndex + currentValueName.length() + 4;
                                    inValueDepth = 0;
                                }
                            }
                        }
                        if ((c == OPEN_CURLY_BRACKETS_CHAR) && (currentValueName != null) && !isEscaped(valueString, currentIndex)) {
                            inValueDepth++;
                        }
                        if ((c == CLOSE_CURLY_BRACKETS_CHAR) && (currentValueName != null) && !isEscaped(valueString, currentIndex)) {
                            if (inValueDepth > 0) inValueDepth--;
                        }
                    }
                    currentIndex++;
                }
                return values;
            } catch (Exception ex) {
                logError("[FANCYMENU] Failed to parse values of placeholder: " + this.placeholderString, ex);
            }
            return null;
        }

        private static boolean isEndOfValueContent(@NotNull Placeholder placeholder, @NotNull String valueString, int currentIndex) {
            if (valueString.length() == currentIndex+3) return true;
            if (StringUtils.startsWith(StringUtils.substring(valueString, currentIndex), COMMA_WRAPPED_IN_APOSTROPHES)) {
                String nextValue = getValueNameIfStartingWithValue(placeholder, StringUtils.substring(valueString, currentIndex+2));
                return nextValue != null;
            }
            return false;
        }

        private static boolean isEscaped(@NotNull String valueString, int index) {
            int backslashCount = 0;
            for (int i = index - 1; i >= 0; i--) {
                if (valueString.charAt(i) != BACKSLASH_CHAR) {
                    break;
                }
                backslashCount++;
            }
            return (backslashCount % 2) == 1;
        }

        /**
         * Returns the value name if TRUE and NULL if FALSE.
         */
        @Nullable
        private static String getValueNameIfStartingWithValue(@NotNull Placeholder placeholder, @NotNull String s) {
            if ((placeholder.getValueNames() == null) || (placeholder.getValueNames().isEmpty())) return null;
            for (String name : placeholder.getValueNames()) {
                if (StringUtils.startsWith(s, APOSTROPHE + name + COLON_WRAPPED_IN_APOSTROPHES)) return name;
            }
            return null;
        }

        public boolean hasValues() {
            Placeholder p = this.getPlaceholder();
            if (p == null) return false;
            return ((p.getValueNames() != null) && !p.getValueNames().isEmpty());
        }

        @Nullable
        public Placeholder getPlaceholder() {
            if (this.placeholderFailed) return null;
            if (this.placeholder == null) this.placeholder = PlaceholderRegistry.getPlaceholder(this.getIdentifier());
            this.placeholderFailed = (this.placeholder == null);
            return this.placeholder;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof ParsedPlaceholder p) {
                return (this.placeholderString.equals(p.placeholderString) && (this.startIndex == p.startIndex) && (this.endIndex == p.endIndex));
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (this.hashcode == null) this.hashcode = Objects.hash(placeholderString, startIndex, endIndex);
            return this.hashcode;
        }

    }

    /**
     * Revisions are validity tokens, not just diagnostics. An old parse may finish after a processor/controller
     * change and overwrite the same cache slot; a later parse must reject that value instead of observing old logic.
     */
    private record CachedPlaceholder(@NotNull String replacement, long cachedAtMillis, long processorRevision, long cachingControllerRevision) {

        private boolean isUsableFor(@NotNull ParsingContext context, long nowMillis) {
            if ((this.processorRevision != context.processors().revision()) || (this.cachingControllerRevision != context.cachingControllerRevision())) return false;
            if (nowMillis < this.cachedAtMillis) return false;
            long elapsed = nowMillis - this.cachedAtMillis;
            return (elapsed >= 0L) && (elapsed < context.cachingDurationMillis());
        }

    }

    private record RegisteredParsingProcessor(long id, @NotNull ConsumingSupplier<String, String> processor) {
    }

    private record ParsingProcessorSnapshot(long revision, @NotNull List<RegisteredParsingProcessor> beforeReplacement, @NotNull List<RegisteredParsingProcessor> afterReplacement) {

        private ParsingProcessorSnapshot {
            beforeReplacement = List.copyOf(beforeReplacement);
            afterReplacement = List.copyOf(afterReplacement);
        }

    }

    private record CachingControllerSnapshot(long revision, @NotNull PlaceholderCachingController controller) {
    }

    private record ParsingContext(@NotNull ParsingProcessorSnapshot processors, long cachingControllerRevision, boolean cachePlaceholders, long cachingDurationMillis, boolean preserveFormattingCodes) {
    }

    public enum ParsingProcessorTiming {
        BEFORE_REPLACING_PLACEHOLDERS,
        AFTER_REPLACING_PLACEHOLDERS
    }

    public record PlaceholderCachingController(@NotNull Supplier<Boolean> shouldCachePlaceholders, @NotNull Supplier<Long> cachingDurationMillis) {

        public PlaceholderCachingController {
            Objects.requireNonNull(shouldCachePlaceholders);
            Objects.requireNonNull(cachingDurationMillis);
        }

    }

}
