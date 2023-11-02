
//Copyright (c) 2022-2023 Keksuccino.
//This code is licensed under DSMSLv2.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.customization.placeholder;

import de.keksuccino.fancymenu.customization.variables.Variable;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import de.keksuccino.fancymenu.util.rendering.text.TextFormattingUtils;
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

    private static final Map<String, Pair<String, Long>> PLACEHOLDER_CACHE = new HashMap<>();

    private static final String PLACEHOLDER_PREFIX = "{\"placeholder\"";
    private static final String EMPTY_STRING = "";
    private static final Character OPEN_BRACKET_CHAR = '{';
    private static final Character CLOSE_BRACKET_CHAR = '}';
    private static final String FORMATTING_PREFIX_AND = "&";
    private static final String FORMATTING_PREFIX_PARAGRAPH = "§";
    private static final String SHORT_VARIABLE_PLACEHOLDER_PREFIX = "$$";
    private static final Character DOLLAR_CHAR = '$';
    private static final String ESCAPED_APOSTROPHE = "\\\"";
    private static final String APOSTROPHE = "\"";
    private static final String ESCAPED_OPEN_BRACKET = "\\{";
    private static final String OPEN_BRACKET = "{";
    private static final String ESCAPED_CLOSE_BRACKET = "\\}";
    private static final String CLOSE_BRACKET = "}";
    private static final Character NEWLINE_CHAR = '\n';
    private static final Character BACKSLASH_CHAR = '\\';

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

    //TODO when replacing placeholders, use *HASH*map to cache already replaced placeholder Strings ( <Placeholder String, Replacement String> )
    // - if the hashmap contains the placeholder string, use the cached replacement instead of parsing the placeholder value again

    @NotNull
    public static List<IndexedPlaceholder> getTopLevelPlaceholders(@Nullable String in) {
        List<IndexedPlaceholder> placeholders = new ArrayList<>();
        if (in == null) return placeholders;
        int skipToIndex = -1;
        int index = -1;
        for (char c : in.toCharArray()) {
            index++;
            if ((skipToIndex != -1) && (skipToIndex > index)) continue;
            if (c == OPEN_BRACKET_CHAR) {
                String sub = StringUtils.substring(in, index);
                if (StringUtils.startsWith(sub, PLACEHOLDER_PREFIX)) {
                    int endIndex = findPlaceholderEndIndex(sub, index);
                    if (endIndex != -1) {
                        skipToIndex = endIndex;
                        endIndex++; //so the sub string ends AFTER the placeholder
                        placeholders.add(new IndexedPlaceholder(StringUtils.substring(in, index, endIndex), index, endIndex));
                    }
                }
            }
        }
        return placeholders;
    }

    private static int findPlaceholderEndIndex(@NotNull String placeholderStartSubString, int startIndex) {
        int currentIndex = startIndex;
        int depth = 0;
        boolean backslash = false;
        for (char c : placeholderStartSubString.toCharArray()) {
            if (currentIndex != startIndex) { //skip first char
                if (c == NEWLINE_CHAR) return -1;
                if ((c == OPEN_BRACKET_CHAR) && !backslash) {
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

        if (in == null) return EMPTY_STRING;
        if (in.length() <= 1) return in;
        if (in.trim().isEmpty()) return in;

        updateLogHandler();

        String original = in;
        if (PLACEHOLDER_CACHE.containsKey(in)) {
            Pair<String, Long> cache = PLACEHOLDER_CACHE.get(in);
            //30ms cooldown before parsing the same String again (thanks to danorris for the idea!)
            if ((cache.getValue() + 30) <= System.currentTimeMillis()) {
                PLACEHOLDER_CACHE.remove(in);
            } else {
                return cache.getKey();
            }
        }
        if (convertFormatCodes) {
            in = TextFormattingUtils.replaceFormattingCodes(in, FORMATTING_PREFIX_AND, FORMATTING_PREFIX_PARAGRAPH);
        }
        in = replaceVariableReferences(in);
        String beforeReplacing = in;
        String replaced = null;
        //Replace placeholders and cover placeholders added by other placeholders (like when getting content from elsewhere containing a placeholder that wasn't part of the original string)
        while ((replaced == null) || !replaced.equals(beforeReplacing)) {
            if (replaced != null) {
                beforeReplacing = replaced;
            }
            replaced = innerReplacePlaceholders(beforeReplacing);
            if (replaced == null) {
                break;
            }
        }
        if (replaced != null) {
            if (convertFormatCodes) {
                replaced = TextFormattingUtils.replaceFormattingCodes(replaced, FORMATTING_PREFIX_AND, FORMATTING_PREFIX_PARAGRAPH);
            }
            replaced = StringUtils.replace(replaced, ESCAPED_APOSTROPHE, APOSTROPHE);
            replaced = StringUtils.replace(replaced, ESCAPED_OPEN_BRACKET, OPEN_BRACKET);
            replaced = StringUtils.replace(replaced, ESCAPED_CLOSE_BRACKET, CLOSE_BRACKET);
            PLACEHOLDER_CACHE.put(original, Pair.of(replaced, System.currentTimeMillis()));
            return replaced;
        }
        PLACEHOLDER_CACHE.put(original, Pair.of(in, System.currentTimeMillis()));
        return in;
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

    @Nullable
    private static String innerReplacePlaceholders(String in) {
        try {
            String out = in;
            int skipToIndex = 0;
            int currentIndex = 0;
            for (char c : in.toCharArray()) {
                if (currentIndex >= skipToIndex) {
                    String s = String.valueOf(c);
                    if (s.equals("{") && in.substring(currentIndex).startsWith("{\"placeholder\":")) {
                        int endIndex = findEndIndex(in, currentIndex);
                        if (endIndex > currentIndex) {
                            skipToIndex = endIndex;
                            String ps = in.substring(currentIndex, endIndex);
                            String placeholderIdentifier = getPlaceholderIdentifier(ps);
                            if (placeholderIdentifier != null) {
                                Placeholder p = PlaceholderRegistry.getPlaceholder(placeholderIdentifier);
                                if (p != null) {
                                    int valueStartIndex = findValueStartIndex(ps);
                                    if (valueStartIndex != -1) {
                                        int valueEndIndex = findEndIndex(ps, valueStartIndex+10);
                                        if (valueEndIndex > valueStartIndex) {
                                            String valueString = ps.substring(valueStartIndex+10+1, valueEndIndex-1);

                                            //Deserialize raw placeholder string
                                            DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
                                            dps.values = getPlaceholderValues(p, valueString);
                                            dps.placeholder = p.id;
                                            dps.originalString = ps;

                                            //Replace nested placeholder strings
                                            Map<String, String> finalValues = new LinkedHashMap<>();
                                            for (Map.Entry<String, String> m : dps.values.entrySet()) {
                                                String inner = innerReplacePlaceholders(m.getValue());
                                                finalValues.put(m.getKey(), inner);
                                            }
                                            //Set new value map with replaced nested placeholders
                                            dps.values = finalValues;

                                            //There should be no nested placeholders anymore now, so convert PS to actual text now
                                            String actualTextOfPs = p.getReplacementFor(dps);
                                            if (actualTextOfPs != null) {
                                                out = out.replace(dps.originalString, actualTextOfPs);
                                            }
                                        } else {
                                            logError("[FANCYMENU] Unable to replace placeholder! ValueEndIndex <= ValueStartIndex: " + ps);
                                            return null;
                                        }
                                    } else {
                                        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
                                        dps.placeholder = placeholderIdentifier;
                                        dps.originalString = ps;
                                        String actualTextOfPs = p.getReplacementFor(dps);
                                        if (actualTextOfPs != null) {
                                            out = out.replace(dps.originalString, actualTextOfPs);
                                        }
                                    }
                                } else {
                                    logError("[FANCYMENU] Unknown placeholder: " + ps);
                                }
                            } else {
                                logError("[FANCYMENU] Unable to parse placeholder identifier: " + in);
                                return null;
                            }
                        } else {
                            logError("[FANCYMENU] Unable to replace placeholder! EndIndex <= CurrentIndex: " + in);
                            return null;
                        }
                    }
                }
                currentIndex++;
            }
            return out;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    private static String getPlaceholderIdentifier(String placeholderString) {
        if ((placeholderString != null) && placeholderString.startsWith("{\"placeholder\":\"")) {
            return placeholderString.split("\"")[3];
        }
        return null;
    }

    //{"placeholder":"calc","values":{"expression":"{"placeholder":"elementposy","values":{"id":"b6e86656-76a0-4e85-a744-bb441c5258dd1665959168373"}}+30"}}

    @NotNull
    private static Map<String, String> getPlaceholderValues(Placeholder placeholder, String valueString) {
        if ((placeholder == null) || (valueString == null) || (placeholder.getValueNames() == null) || (placeholder.getValueNames().isEmpty())) {
            return new HashMap<>();
        }
        Map<String, String> m = new LinkedHashMap<>();
        try {
            int currentIndex = 0;
            int inValueDepth = 0;
            String currentValueName = null;
            int currentValueStartIndex = 0;
            for (char c : valueString.toCharArray()) {
                if (currentIndex >= currentValueStartIndex) {
                    String s = String.valueOf(c);
                    if (s.equals("\"")) {
                        if (currentValueName != null) {
                            if ((inValueDepth == 0) && !valueString.substring(currentIndex-1).startsWith("\\") && isEndOfValueContent(placeholder, valueString, currentIndex)) {
                                String valueContent = valueString.substring(currentValueStartIndex, currentIndex);
                                m.put(currentValueName, valueContent);
                                currentValueName = null;
                                currentValueStartIndex = 0;
                            }
                        } else {
                            currentValueName = getValueNameIfStartingWithValue(placeholder, valueString.substring(currentIndex));
                            if (currentValueName != null) {
                                currentValueStartIndex = currentIndex + currentValueName.length() + 4;
                                inValueDepth = 0;
                            }
                        }
                    }
                    if (s.equals("{") && (currentValueName != null) && !valueString.substring(currentIndex-1).startsWith("\\")) {
                        inValueDepth++;
                    }
                    if (s.equals("}") && (currentValueName != null) && !valueString.substring(currentIndex-1).startsWith("\\")) {
                        if (inValueDepth > 0) {
                            inValueDepth--;
                        }
                    }
                }
                currentIndex++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return m;
    }

    private static boolean isEndOfValueContent(Placeholder placeholder, String valueString, int currentIndex) {
        if (valueString.length() == currentIndex+1) {
            return true;
        }
        String s1 = valueString.substring(currentIndex-2);
        if (s1.startsWith("\"}\"")) {
            return true;
        }
        String s2 = valueString.substring(currentIndex);
        if (s2.startsWith("\",\"")) {
            String s3 = valueString.substring(currentIndex+2);
            String nextValue = getValueNameIfStartingWithValue(placeholder, s3);
            return nextValue != null;
        }
        return false;
    }

    /**
     * Returns the value name if TRUE and NULL if FALSE.
     */
    private static String getValueNameIfStartingWithValue(Placeholder placeholder, String s) {
        if (placeholder.getValueNames() == null) {
            return null;
        }
        for (String name : placeholder.getValueNames()) {
            if (s.startsWith("\"" + name + "\":\"")) {
                return name;
            }
        }
        return null;
    }

    private static int findValueStartIndex(String placeholderString) {
        int currentIndex = 1;
        for (char c : placeholderString.substring(1).toCharArray()) {
            if (String.valueOf(c).startsWith(",") && placeholderString.substring(currentIndex).startsWith(",\"values\":")) {
                return currentIndex;
            }
            currentIndex++;
        }
        return -1;
    }

    private static int findEndIndex(String in, int startIndex) {
        if (in.substring(startIndex).startsWith("{") && ((startIndex == 0) || !in.substring(startIndex-1).startsWith("\\"))) {
            int currentIndex = startIndex+1;
            int depth = 0;
            for (char c : in.substring(startIndex+1).toCharArray()) {
                if (String.valueOf(c).equals("{") && !in.substring(currentIndex-1).startsWith("\\")) {
                    depth++;
                } else if (String.valueOf(c).equals("}") && !in.substring(currentIndex-1).startsWith("\\")) {
                    if (depth <= 0) {
                        return currentIndex+1;
                    } else {
                        depth--;
                    }
                }
                currentIndex++;
            }
        }
        return -1;
    }

    private static void logError(String error) {
        if (!LOG_COOLDOWN.containsKey(error)) {
            LOGGER.error(error);
            LOG_COOLDOWN.put(error, System.currentTimeMillis());
        }
    }

    private static void updateLogHandler() {
        long now = System.currentTimeMillis();
        List<String> remove = new ArrayList<>();
        for (Map.Entry<String, Long> m : LOG_COOLDOWN.entrySet()) {
            if ((m.getValue() + 10000) <= now) {
                remove.add(m.getKey());
            }
        }
        for (String s : remove) {
            LOG_COOLDOWN.remove(s);
        }
    }

    public static class IndexedPlaceholder {

        public final String placeholder;
        public final int startIndex;
        public final int endIndex;
        private Integer hashcode;

        protected IndexedPlaceholder(@NotNull String placeholder, int startIndex, int endIndex) {
            this.placeholder = placeholder;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof IndexedPlaceholder p) {
                return (this.placeholder.equals(p.placeholder) && (this.startIndex == p.startIndex) && (this.endIndex == p.endIndex));
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (this.hashcode == null) this.hashcode = Objects.hash(placeholder, startIndex, endIndex);
            return this.hashcode;
        }

    }

}
