
//Copyright (c) 2022-2023 Keksuccino.
//This code is licensed under DSMSLv2.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import com.google.common.collect.Lists;
import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
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
    private static final Map<String, Long> LOG_COOLDOWN = new HashMap<>();
    private static final long LOG_COOLDOWN_MS = 10000;

    private static final HashSet<String> TOO_LONG_TO_PARSE = new HashSet<>();
    private static final HashMap<String, Boolean> CONTAINS_PLACEHOLDERS = new HashMap<>();
    private static final Map<String, Pair<String, Long>> PLACEHOLDER_CACHE = new HashMap<>();
    private static final Long PLACEHOLDER_CACHE_DURATION_MS = 30L;

    private static final String PLACEHOLDER_PREFIX = "{\"placeholder\":\"";
    private static final String EMPTY_STRING = "";
    private static final Character OPEN_BRACKET_CHAR = '{';
    private static final Character CLOSE_BRACKET_CHAR = '}';
    private static final String FORMATTING_PREFIX_AND = "&";
    private static final String FORMATTING_PREFIX_PARAGRAPH = "§";
    private static final String SHORT_VARIABLE_PLACEHOLDER_PREFIX = "$$";
    private static final Character DOLLAR_CHAR = '$';
    private static final String APOSTROPHE = "\"";
    private static final Character APOSTROPHE_CHAR = '\"';
    private static final Character NEWLINE_CHAR = '\n';
    private static final Character BACKSLASH_CHAR = '\\';
    private static final String BACKSLASH = "\\";
    private static final String COMMA = ",";
    private static final String COMMA_WRAPPED_IN_APOSTROPHES = "\",\"";
    private static final String COLON_WRAPPED_IN_APOSTROPHES = "\":\"";
    private static final String TOO_LONG_TO_PARSE_LOCALIZATION = "fancymenu.placeholders.error.text_too_long";

    /**
     * Simple check if the given {@link String} contains placeholders.<br>
     * This basically only checks if the {@link String} contains ' {"placeholder" ', so it's not 100% safe, but performance-friendly.
     *
     * @param checkForVariableReferences If the method should check for the short version of variable placeholders ($$variable_name).
     */
    public static boolean containsPlaceholders(@Nullable String in, boolean checkForVariableReferences) {
        if (in == null) return false;
        if (in.length() <= 2) return false;
        if (StringUtils.contains(in, PLACEHOLDER_PREFIX)) return true;
        if (checkForVariableReferences && !Objects.equals(in, replaceVariableReferences(in))) return true;
        return false;
    }

    @NotNull
    public static String replacePlaceholdersIn(@Nullable String in, @Nullable HashMap<String, String> parsed, boolean replaceFormattingCodes) {

        if (in == null) return EMPTY_STRING;
        if (in.length() <= 2) return in;

        //TODO zu langes parsing markdown text element etwas kürzen = führt zu stack overdlow in markdown???? FIXEN!

        Boolean containsPlaceholders = CONTAINS_PLACEHOLDERS.get(in);
        if (containsPlaceholders == null) {
            containsPlaceholders = containsPlaceholders(in, true);
            CONTAINS_PLACEHOLDERS.put(in, containsPlaceholders);
        }
        if (!containsPlaceholders) return in;

        if (TOO_LONG_TO_PARSE.contains(in)) return I18n.get(TOO_LONG_TO_PARSE_LOCALIZATION);
        if (in.length() >= 20000) {
            TOO_LONG_TO_PARSE.add(in);
            return I18n.get(TOO_LONG_TO_PARSE_LOCALIZATION);
        }

        Pair<String, Long> cached = PLACEHOLDER_CACHE.get(in);
        if ((cached != null) && ((cached.getValue() + PLACEHOLDER_CACHE_DURATION_MS) > System.currentTimeMillis())) return cached.getKey();

        String original = in;

        if (parsed == null) parsed = new HashMap<>();

        int hash = in.hashCode();
        while (true) {
            //Reverse the list to start replacing from the end of the String, so all nested placeholders get replaced first
            for (ParsedPlaceholder p : Lists.reverse(findPlaceholders(in, false, parsed, replaceFormattingCodes))) {
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

        if (replaceFormattingCodes) in = TextFormattingUtils.replaceFormattingCodes(in, FORMATTING_PREFIX_AND, FORMATTING_PREFIX_PARAGRAPH);

        PLACEHOLDER_CACHE.put(original, Pair.of(in, System.currentTimeMillis()));

        return in;

    }

    /**
     * Finds all placeholders in a {@link String}.<br>
     * Will not find placeholders that get added to the {@link String} by replacing placeholders.
     *
     * @param in The {@link String} to get the placeholders from.
     * @param excludeWithNested If placeholders with nested placeholders should get excluded.
     */
    @NotNull
    public static List<ParsedPlaceholder> findPlaceholders(@Nullable String in, boolean excludeWithNested, @NotNull HashMap<String, String> parsed, boolean replaceFormattingCodes) {
        List<ParsedPlaceholder> placeholders = new ArrayList<>();
        if (in == null) return placeholders;
        int index = -1;
        for (char c : in.toCharArray()) {
            index++;
            if (c == OPEN_BRACKET_CHAR) {
                String sub = StringUtils.substring(in, index);
                if (StringUtils.startsWith(sub, PLACEHOLDER_PREFIX)) {
                    int endIndex = findPlaceholderEndIndex(sub, index, excludeWithNested);
                    if (endIndex != -1) {
                        endIndex++; //so the sub string ends AFTER the placeholder
                        placeholders.add(new ParsedPlaceholder(StringUtils.substring(in, index, endIndex), index, endIndex, parsed, replaceFormattingCodes));
                    }
                }
            }
        }
        return placeholders;
    }

    private static int findPlaceholderEndIndex(@NotNull String placeholderStartSubString, int startIndex, boolean excludeWithNested) {
        int currentIndex = startIndex;
        int depth = 0;
        int subStringIndex = 0;
        boolean backslash = false;
        for (char c : placeholderStartSubString.toCharArray()) {
            if (currentIndex != startIndex) { //skip first char
                if (c == NEWLINE_CHAR) return -1;
                if ((c == OPEN_BRACKET_CHAR) && !backslash) {
                    //If nested placeholder found and excludeWithNested, return -1 to ignore placeholder
                    if (excludeWithNested && StringUtils.startsWith(StringUtils.substring(placeholderStartSubString, subStringIndex), PLACEHOLDER_PREFIX)) {
                        return -1;
                    }
                    depth++;
                } else if ((c == CLOSE_BRACKET_CHAR) && !backslash) {
                    if (depth <= 0) {
                        return currentIndex;
                    } else {
                        depth--;
                    }
                }
                backslash = (c == BACKSLASH_CHAR);
            }
            subStringIndex++;
            currentIndex++;
        }
        return -1;
    }

    @NotNull
    public static String replacePlaceholders(@Nullable String in) {
        return replacePlaceholders(in, true);
    }

    @NotNull
    public static String replacePlaceholders(@Nullable String in, boolean convertFormatCodes) {

        return replacePlaceholdersIn(in, null, convertFormatCodes);

    }

    @NotNull
    public static String replaceVariableReferences(@NotNull String in) {
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

        protected ParsedPlaceholder(@NotNull String placeholderString, int startIndex, int endIndex, @NotNull HashMap<String, String> parsed, boolean replaceFormattingCodes) {
            this.placeholderString = placeholderString;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.parsed = parsed;
            this.replaceFormattingCodes = replaceFormattingCodes;
        }

        @Nullable
        public String getIdentifier() {
            if (this.identifierFailed) return null;
            if (this.identifier != null) return this.identifier;
            try {
                this.identifier = StringUtils.split(StringUtils.substring(this.placeholderString, PLACEHOLDER_PREFIX.length()), APOSTROPHE, 2)[0];
                return this.identifier;
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
            HashMap<String, String> values = this.getValues();
            if (this.hasValues() && ((values == null) || values.isEmpty())) return this.placeholderString;
            DeserializedPlaceholderString deserialized = new DeserializedPlaceholderString(identifier, null, this.placeholderString);
            if (values != null) {
                for (Map.Entry<String, String> value : values.entrySet()) {
                    deserialized.values.put(value.getKey(), replacePlaceholdersIn(value.getValue(), this.parsed, this.replaceFormattingCodes));
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
                String valueString = COMMA + StringUtils.split(this.placeholderString, COMMA, 2)[1];
                int currentIndex = 0;
                int inValueDepth = 0;
                String currentValueName = null;
                int currentValueStartIndex = 0;
                for (char c : valueString.toCharArray()) {
                    if (currentIndex >= currentValueStartIndex) {
                        if (c == APOSTROPHE_CHAR) {
                            if (currentValueName != null) {
                                if ((inValueDepth == 0) && !StringUtils.startsWith(StringUtils.substring(valueString, currentIndex-1), BACKSLASH) && isEndOfValueContent(placeholder, valueString, currentIndex)) {
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
                        if ((c == OPEN_BRACKET_CHAR) && (currentValueName != null) && !StringUtils.startsWith(StringUtils.substring(valueString, currentIndex-1), BACKSLASH)) {
                            inValueDepth++;
                        }
                        if ((c == CLOSE_BRACKET_CHAR) && (currentValueName != null) && !StringUtils.startsWith(StringUtils.substring(valueString, currentIndex-1), BACKSLASH)) {
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
