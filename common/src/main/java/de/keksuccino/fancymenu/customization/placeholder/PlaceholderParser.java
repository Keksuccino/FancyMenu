//Copyright (c) 2022-2025 Keksuccino.
//This code is licensed under DSMSLv3.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.FancyMenu;
import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.texteditor.TextEditorScreen;
import net.minecraft.client.resources.language.I18n;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

//PLACEHOLDER STRING EXAMPLES:
//{"placeholder":"ph_id","values":{"value_1":"{"placeholder":"ph_id_2"}","value_2":"content"}}
//{"placeholder":"ph_id_2"}

public class PlaceholderParser {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final HashMap<String, Long> LOG_COOLDOWN = new HashMap<>();
    private static final long LOG_COOLDOWN_MS = 10000;

    private static final HashSet<String> TOO_LONG_TO_PARSE = new HashSet<>();
    private static final HashMap<String, Boolean> CONTAINS_PLACEHOLDERS = new HashMap<>();
    private static final HashMap<String, Pair<String, Long>> PLACEHOLDER_CACHE = new HashMap<>();
    private static final Map<Long, ConsumingSupplier<String, String>> PARSING_PROCESSORS = new LinkedHashMap<>();

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
    private static final String TOO_LONG_TO_PARSE_LOCALIZATION = "fancymenu.placeholders.error.text_too_long";

    private static long processorId = 0;

    static {

        // Text editor multi-line placeholder compression
        // Can be done for multi-line strings too, because the method only replaces codes instead of actual spaces or newlines
        addParsingProcessor(TextEditorScreen::compileSingleLineString);

        // Minecraft Formatting Codes
        addParsingProcessor(in -> {
            return TextFormattingUtils.replaceFormattingCodes(in, FORMATTING_PREFIX_AND, FORMATTING_PREFIX_PARAGRAPH);
        });

        // Replace generic reference of FancyMenu's variables ($$variable_name instead of using the "Get Variable" placeholder)
        addParsingProcessor(in -> {
            String replaced = in;
            int index = 0;
            for (char c : in.toCharArray()) {
                if (c == DOLLAR_CHAR) {
                    String sub = StringUtils.substring(in, index);
                    if (StringUtils.startsWith(sub, SHORT_VARIABLE_PLACEHOLDER_PREFIX)) {
                        for (Variable variable : VariableHandler.getVariables()) {
                            if (StringUtils.startsWith(sub, SHORT_VARIABLE_PLACEHOLDER_PREFIX + variable.getName())) {
                                replaced = StringUtils.replace(replaced, SHORT_VARIABLE_PLACEHOLDER_PREFIX + variable.getName(), variable.getValue());
                                break;
                            }
                        }
                    }
                }
                index++;
            }
            return replaced;
        });

//        addParsingProcessor(in -> {
//            // Can be done for multi-line strings too, because the method only replaces codes instead of actual spaces or newlines
//            return TextEditorScreen.compileSingleLineString(in);
//        });

    }

    public static long addParsingProcessor(@NotNull ConsumingSupplier<String, String> processor) {
        processorId++;
        PARSING_PROCESSORS.put(processorId, Objects.requireNonNull(processor));
        return processorId;
    }

    public static void removeParsingProcessor(long id) {
        PARSING_PROCESSORS.remove(id);
    }

    public static boolean isCachingPlaceholders() {
        return FancyMenu.getOptions().placeholderCachingDurationMs.getValue() > 0;
    }

    public static long getPlaceholderCachingDurationMs() {
        return FancyMenu.getOptions().placeholderCachingDurationMs.getValue();
    }

    /**
     * Simple check if the given {@link String} contains placeholders.<br>
     * This basically only checks if the {@link String} contains ' {"placeholder" ', so it's not 100% safe, but performance-friendly.
     *
     * @param checkForVariableReferences If the method should check for the short version of variable placeholders ($$variable_name).
     * @param checkForFormattingCodes If the method should check for "&" formatting codes.
     */
    public static boolean containsPlaceholders(@Nullable String in, boolean checkForVariableReferences, boolean checkForFormattingCodes) {
        if (in == null) return false;
        if (in.length() <= 2) return false;
        if (StringUtils.contains(in, "{\"placeholder\"")) return true;
        if (checkForFormattingCodes && (in.hashCode() != TextFormattingUtils.replaceFormattingCodes(in, FORMATTING_PREFIX_AND, FORMATTING_PREFIX_PARAGRAPH).hashCode())) return true;
        if (checkForVariableReferences && (in.hashCode() != replaceVariableReferences(in).hashCode())) return true;
        return false;
    }

    /**
     * Simple check if the given {@link String} contains placeholders.<br>
     * This basically only checks if the {@link String} contains ' {"placeholder" ', so it's not 100% safe, but performance-friendly.
     *
     * @param checkForVariableReferences If the method should check for the short version of variable placeholders ($$variable_name).
     */
    public static boolean containsPlaceholders(@Nullable String in, boolean checkForVariableReferences) {
        return containsPlaceholders(in, checkForVariableReferences, true);
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
        return replacePlaceholders(in, true);
    }

    /**
     * Replaces all placeholders in the given {@link String}.<br>
     * Since placeholders can contain variables that are not always the same, this task can be performance-intensive,
     * so you should keep in mind to not parse/update long texts too often in a short time frame.
     *
     * @param in The {@link String} to replace placeholders in.
     * @param replaceFormattingCodes If Minecraft formatting codes should get replaced.
     * @return The given {@link String} with all placeholders replaced.
     */
    @NotNull
    public static String replacePlaceholders(@Nullable String in, boolean replaceFormattingCodes) {
        return replacePlaceholders(in, null, replaceFormattingCodes);
    }

    /**
     * Replaces all placeholders in the given {@link String}.<br>
     * Since placeholders can contain variables that are not always the same, this task can be performance-intensive,
     * so you should keep in mind to not parse/update long texts too often in a short time frame.
     *
     * @param in The {@link String} to replace placeholders in.
     * @param parsed The placeholder replacement cache.
     * @param replaceFormattingCodes If Minecraft formatting codes should get replaced.
     * @return The given {@link String} with all placeholders replaced.
     */
    @NotNull
    protected static String replacePlaceholders(@Nullable String in, @Nullable HashMap<String, String> parsed, boolean replaceFormattingCodes) {

        if (in == null) return EMPTY_STRING;

        if (in.length() <= 2) return in;

        Boolean containsPlaceholders = CONTAINS_PLACEHOLDERS.get(in);
        if (containsPlaceholders == null) {
            containsPlaceholders = containsPlaceholders(in, true, replaceFormattingCodes);
            CONTAINS_PLACEHOLDERS.put(in, containsPlaceholders);
        }
        if (!containsPlaceholders) return in;

        if (TOO_LONG_TO_PARSE.contains(in)) return I18n.get(TOO_LONG_TO_PARSE_LOCALIZATION);
        if (in.length() >= MAX_TEXT_LENGTH) {
            TOO_LONG_TO_PARSE.add(in);
            return I18n.get(TOO_LONG_TO_PARSE_LOCALIZATION);
        }

        if (isCachingPlaceholders()) {
            Pair<String, Long> cached = PLACEHOLDER_CACHE.get(in);
            if ((cached != null) && ((cached.getValue() + getPlaceholderCachingDurationMs()) > System.currentTimeMillis())) return cached.getKey();
        }

        String original = in;

        //Used to cache replacements for already parsed placeholders, so they can get reused to improve performance
        if (parsed == null) parsed = new HashMap<>();

        int hash = in.hashCode();
        while (true) {
            //Reverse the list to start replacing from the end of the String, so all nested placeholders get replaced first
            for (ParsedPlaceholder p : Lists.reverse(findPlaceholders(in, parsed, replaceFormattingCodes))) {
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

        if (isCachingPlaceholders()) {
            PLACEHOLDER_CACHE.put(original, Pair.of(in, System.currentTimeMillis()));
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
    public static List<ParsedPlaceholder> findPlaceholders(@Nullable String in, @NotNull HashMap<String, String> parsed, boolean replaceFormattingCodes) {
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
                        placeholders.add(new ParsedPlaceholder(candidate, i, endIndex + 1, parsed, replaceFormattingCodes));

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
        long now = System.currentTimeMillis();
        Long last = LOG_COOLDOWN.get(error);
        if ((last != null) && ((last + LOG_COOLDOWN_MS) < now)) {
            last = null;
            LOG_COOLDOWN.remove(error);
        }
        if (last == null) {
            if (ex != null) {
                LOGGER.error(error, ex);
            } else {
                LOGGER.error(error);
            }
            LOG_COOLDOWN.put(error, now);
        }
    }

    public static class ParsedPlaceholder {

        public final String placeholderString;
        public final int startIndex;
        public final int endIndex;
        private final HashMap<String, String> parsed;
        private final boolean replaceFormattingCodes;
        private Integer hashcode;
        private String identifier;
        private boolean identifierFailed = false;
        private Placeholder placeholder;
        private boolean placeholderFailed = false;
        private String normalizedString;

        protected ParsedPlaceholder(@NotNull String placeholderString, int startIndex, int endIndex, @NotNull HashMap<String, String> parsed, boolean replaceFormattingCodes) {
            this.placeholderString = placeholderString;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.parsed = parsed;
            this.replaceFormattingCodes = replaceFormattingCodes;
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
            if (this.hasValues() && ((values == null) || values.isEmpty())) return this.placeholderString;
            DeserializedPlaceholderString deserialized = new DeserializedPlaceholderString(identifier, null, this.placeholderString);
            if (values != null) {
                for (Map.Entry<String, String> value : values.entrySet()) {
                    deserialized.values.put(value.getKey(), replacePlaceholders(value.getValue(), this.parsed, this.replaceFormattingCodes));
                }
            }
            return p.getReplacementFor(deserialized);
        }

        @Nullable
        public HashMap<String, String> getValues() {
            HashMap<String, String> values = new HashMap<>();
            try {
                Placeholder placeholder = this.getPlaceholder();
                if ((placeholder == null) || !this.hasValues()) {
                    return null;
                }
                String normalized = this.getNormalizedString();
                // Check if there's actually a comma (indicating values section exists)
                if (!normalized.contains(COMMA)) {
                    return null;
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

}
