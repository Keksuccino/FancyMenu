//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.variables.UserVariableSnapshot;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorWindowBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Supplier;

//PLACEHOLDER STRING EXAMPLES:
//{"placeholder":"ph_id","values":{"value_1":"{"placeholder":"ph_id_2"}","value_2":"content"}}
//{"placeholder":"ph_id_2"}

/**
 * Replaces placeholders and {@code $$} variable references in texts.
 *
 * <p>The work is split by responsibility: {@link PlaceholderScanner} finds placeholders, {@link PlaceholderValueParser} reads their values,
 * {@link PlaceholderTemplate} compiles texts once, {@link PlaceholderEvaluator} replaces their placeholders, {@link ParsingProcessorRegistry}
 * holds the parsing processors and {@link PlaceholderResultCache} caches results. This class is the public entry point.</p>
 */
public class PlaceholderParser {

    /** Texts of this length or longer get answered with {@link #TOO_LONG_TO_PARSE_ERROR_MESSAGE}, unless they get parsed without length limit. */
    static final int MAX_TEXT_LENGTH = 17000;
    private static final String EMPTY_STRING = "";
    private static final String FORMATTING_PREFIX_AND = "&";
    private static final String FORMATTING_PREFIX_PARAGRAPH = "§";
    private static final char APOSTROPHE_CHAR = '\"';
    static final String TOO_LONG_TO_PARSE_ERROR_MESSAGE = "ERROR: Text too long to parse placeholders! 17,000 characters at max!";

    static final long FORMATTING_CODE_PROCESSOR_ID;

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
        addParsingProcessor(ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS, PlaceholderParser::resolveVariableReferences);

    }

    /**
     * Replaces every {@code $$variable_name} reference to one of FancyMenu's variables with the value of the variable.
     * At every {@code $$}, the variable with the longest name that starts there wins (see {@link ShortReferenceResolver}).
     */
    @NotNull
    public static String resolveVariableReferences(@NotNull String text) {
        if (!text.contains(ShortReferenceResolver.PREFIX)) return text;
        return resolveVariableReferences(text, VariableHandler.getVariableSnapshots());
    }

    @NotNull
    static String resolveVariableReferences(@NotNull String text, @NotNull List<UserVariableSnapshot> variables) {
        if (variables.isEmpty()) return text;
        return ShortReferenceResolver.resolve(text, (in, nameStart) -> {
            UserVariableSnapshot variable = ShortReferenceResolver.findLongestName(in, nameStart, variables, UserVariableSnapshot::name);
            return (variable != null) ? new ShortReferenceResolver.Reference(variable.name().length(), variable.value()) : null;
        });
    }

    /**
     * Adds a parsing processor. The timing controls if the processor should get executed before or after replacing placeholders in the string.
     *
     * <p>{@code ParsingProcessorTiming.BEFORE_REPLACING_PLACEHOLDERS} processors get executed if the input text is not null, is not too long to get parsed and is not currently in the cache.
     * They also get executed on the values of placeholders, before the placeholders in these values get replaced.
     *
     * <p>{@code ParsingProcessorTiming.AFTER_REPLACING_PLACEHOLDERS} processors only get executed if the input text contains placeholders or {@code $$} variable references (see {@link #containsPlaceholders(String)}) and the system successfully replaced them.
     * They also get executed on the values of placeholders that contain placeholders or {@code $$} variable references.
     *
     * @return The unique ID of the processor.
     */
    public static long addParsingProcessor(@NotNull ParsingProcessorTiming timing, @NotNull ConsumingSupplier<String, String> processor) {
        return ParsingProcessorRegistry.add(timing, processor);
    }

    public static void removeParsingProcessor(long id) {
        ParsingProcessorRegistry.remove(id);
    }

    @NotNull
    public static PlaceholderCachingController getPlaceholderCachingController() {
        return PlaceholderResultCache.cachingControllerSnapshot().controller();
    }

    public static void setPlaceholderCachingController(@NotNull PlaceholderCachingController cachingController) {
        PlaceholderResultCache.setCachingController(cachingController);
    }

    public static boolean isCachingPlaceholders() {
        PlaceholderCachingController controller = PlaceholderResultCache.cachingControllerSnapshot().controller();
        return controller.shouldCachePlaceholders().get();
    }

    public static long getPlaceholderCachingDurationMs() {
        PlaceholderCachingController controller = PlaceholderResultCache.cachingControllerSnapshot().controller();
        return controller.cachingDurationMillis().get();
    }

    /**
     * Simple check if the given {@link String} contains placeholders or {@code $$} references to FancyMenu variables.<br>
     * This only checks if the {@link String} contains the start of a placeholder (<code>&#123;"placeholder":"</code>, with the same whitespace
     * between its tokens that placeholders allow) or {@code $$} followed by at least one character, so it's not 100% safe, but performance-friendly.
     * It does not check if the placeholder has an end, or if a placeholder or variable with that name exists.
     */
    public static boolean containsPlaceholders(@Nullable String in) {
        return PlaceholderEvaluator.containsPlaceholders(in);
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
        return replacePlaceholders(in, false);
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
        return replacePlaceholders(in, true);
    }

    /**
     * Replaces all placeholders in a text of any length.
     *
     * <p>{@link #replacePlaceholders(String)} and {@link #replacePlaceholdersPreservingFormattingCodes(String)} answer every text that reaches
     * their length limit with an error text, because replacing placeholders takes longer the longer a text is.
     * This method is for callers that limit the length of their text themselves and tell the user about their limit in their own way.</p>
     *
     * @param preserveFormattingCodes If formatting-code prefixes should stay as they are, like {@link #replacePlaceholdersPreservingFormattingCodes(String)} does it.
     */
    @NotNull
    public static String replacePlaceholdersWithoutLengthLimit(@Nullable String in, boolean preserveFormattingCodes) {
        if (in == null) return EMPTY_STRING;
        return PlaceholderEvaluator.replacePlaceholders(in, null, PlaceholderEvaluator.createParsingContext(preserveFormattingCodes, false), null);
    }

    @NotNull
    private static String replacePlaceholders(@Nullable String in, boolean preserveFormattingCodes) {
        if (in == null) return EMPTY_STRING;
        if (in.length() >= MAX_TEXT_LENGTH) return TOO_LONG_TO_PARSE_ERROR_MESSAGE;
        return PlaceholderEvaluator.replacePlaceholders(in, null, PlaceholderEvaluator.createParsingContext(preserveFormattingCodes, true), null);
    }

    /**
     * Finds all placeholders in a {@link String}.<br>
     * Will not find placeholders that get added to the {@link String} by replacing other placeholders.
     *
     * @param in The {@link String} to get the placeholders from.
     * @param parsed Replacements of placeholders by their raw text, shared by the placeholders in the values of the found placeholders when they get replaced.
     */
    @NotNull
    public static List<ParsedPlaceholder> findPlaceholders(@Nullable String in, @NotNull HashMap<String, String> parsed) {
        List<ParsedPlaceholder> placeholders = new ArrayList<>();
        if (in == null) return placeholders;
        PlaceholderEvaluator.ParsingContext context = PlaceholderEvaluator.createParsingContext(false, true);
        PlaceholderScanner scanner = new PlaceholderScanner(in);
        while (scanner.next()) {
            int start = scanner.start();
            placeholders.add(new ParsedPlaceholder(in.substring(start, scanner.end()), start, scanner.end(), scanner.identifierStart() - start, parsed, context));
        }
        return placeholders;
    }

    public static class ParsedPlaceholder {

        private static final int UNKNOWN_IDENTIFIER_START = -2;

        public final String placeholderString;
        public final int startIndex;
        public final int endIndex;
        private final HashMap<String, String> parsed;
        private final PlaceholderEvaluator.ParsingContext context;
        private Integer hashcode;
        /** Relative to the placeholder string, {@link #UNKNOWN_IDENTIFIER_START} until it gets searched, -1 if the string is no placeholder. */
        private int identifierStart;
        private String identifier;
        private boolean identifierFailed = false;
        private Placeholder placeholder;
        private boolean placeholderFailed = false;

        protected ParsedPlaceholder(@NotNull String placeholderString, int startIndex, int endIndex, @NotNull HashMap<String, String> parsed, boolean preserveFormattingCodes) {
            this(placeholderString, startIndex, endIndex, UNKNOWN_IDENTIFIER_START, parsed, PlaceholderEvaluator.createParsingContext(preserveFormattingCodes, true));
        }

        private ParsedPlaceholder(@NotNull String placeholderString, int startIndex, int endIndex, int identifierStart, @NotNull HashMap<String, String> parsed, @NotNull PlaceholderEvaluator.ParsingContext context) {
            this.placeholderString = placeholderString;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.identifierStart = identifierStart;
            this.parsed = parsed;
            this.context = context;
        }

        private int getIdentifierStart() {
            if (this.identifierStart == UNKNOWN_IDENTIFIER_START) this.identifierStart = PlaceholderScanner.matchPlaceholderStart(this.placeholderString, 0);
            return this.identifierStart;
        }

        /**
         * The identifier is everything between the opening quote of the identifier and the next quote.
         *
         * @return The identifier, or null if it is empty or the placeholder string is no placeholder.
         */
        @Nullable
        public String getIdentifier() {
            if (this.identifierFailed) return null;
            if (this.identifier != null) return this.identifier;
            int start = this.getIdentifierStart();
            int end = (start >= 0) ? this.placeholderString.indexOf(APOSTROPHE_CHAR, start) : -1;
            if (end > start) {
                this.identifier = this.placeholderString.substring(start, end);
                return this.identifier;
            }
            this.identifierFailed = true;
            return null;
        }

        /**
         * Replaces the placeholder the same way parsing a text replaces it. Its values come from {@link #getValues()}.
         * It keeps its raw text if it is unknown, can't run on this thread, has values that can't be read, fails or returns null.
         */
        @NotNull
        public String getReplacement() {
            String identifier = this.getIdentifier();
            if (identifier == null) return this.placeholderString;
            Placeholder p = this.getPlaceholder();
            if (p == null) return this.placeholderString;
            return PlaceholderEvaluator.replaceParsedPlaceholder(this, p, identifier, this.parsed, this.context);
        }

        /**
         * Reads the values of the placeholder, see {@link PlaceholderValueParser}.
         *
         * @return The values by name, or null if the placeholder is unknown or its values could not be read.
         */
        @Nullable
        public HashMap<String, String> getValues() {
            try {
                Placeholder placeholder = this.getPlaceholder();
                if (placeholder == null) return null;
                return PlaceholderValueParser.parseValues(this.placeholderString, this.getIdentifierStart(), PlaceholderRegistry.getValueNames(placeholder));
            } catch (Exception ex) {
                PlaceholderEvaluator.logError("[FANCYMENU] Failed to parse values of placeholder: " + this.placeholderString, ex);
            }
            return null;
        }

        public boolean hasValues() {
            Placeholder p = this.getPlaceholder();
            if (p == null) return false;
            return !PlaceholderRegistry.getValueNames(p).isEmpty();
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
