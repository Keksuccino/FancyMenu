//TODO übernehmen

//Copyright (c) 2022 Keksuccino.
//This code is licensed under DSMSL.
//For more information about the license, see this: https://github.com/Keksuccino/FancyMenu/blob/master/LICENSE.md

package de.keksuccino.fancymenu.menu.placeholder.v2;

import de.keksuccino.fancymenu.menu.placeholder.v1.DynamicValueHelper;
import de.keksuccino.konkrete.input.StringUtils;
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

    private static Map<String, Long> logCooldown = new HashMap<>();

    @NotNull
    public static String replacePlaceholders(@NotNull String in) {
        updateLogHandler();
        //TODO remove in a later release
        in = DynamicValueHelper.convertFromRaw(in);
        in = StringUtils.convertFormatCodes(in, "&", "§");
        String ori = in;
        String rep = null;
        //Replace placeholders and cover placeholders added by other placeholders (like when getting content from elsewhere containing a placeholder that wasn't part of the original string)
        while ((rep == null) || !rep.equals(ori)) {
            if (rep != null) {
                ori = rep;
            }
            rep = innerReplacePlaceholders(ori);
            if (rep == null) {
                break;
            }
        }
        if (rep != null) {
            return rep.replace("\\\"", "\"").replace("\\{", "{").replace("\\}", "}");
        }
        return in;
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
                                Placeholder p = PlaceholderRegistry.getPlaceholderForIdentifier(placeholderIdentifier);
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
            return placeholderString.split("[\"]")[3];
        }
        return null;
    }

    //{"placeholder":"calc","values":{"expression":"{"placeholder":"elementposy","values":{"id":"b6e86656-76a0-4e85-a744-bb441c5258dd1665959168373"}}+30"}}

    @NotNull
    private static Map<String, String> getPlaceholderValues(Placeholder placeholder, String valueString) {
        if ((placeholder == null) || (valueString == null) || (placeholder.getValueNames() == null) || (placeholder.getValueNames().isEmpty())) {
//            LOGGER.info("################ PLACEHOLDER: " + placeholder);
//            LOGGER.info("################ VALUE STRING: " + valueString);
//            LOGGER.info("################ VALUE NAMES: " + placeholder.getValueNames());
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

//    private static boolean isStartOfExistingPlaceholder(String s, int currentIndex) {
//        String start = s.substring(currentIndex);
//        if (start.startsWith("{\"placeholder\":\"")) {
//            for (Placeholder p : PlaceholderRegistry.getPlaceholdersList()) {
//                if (start.startsWith("{\"placeholder\":\"" + p.id + "\"")) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

//    private static boolean isEndOfPlaceholder(String s, int currentIndex) {
//        try {
//            if (currentIndex < 2) {
//                return false;
//            }
//            if (s.substring(currentIndex-2).startsWith("\"}}")) {
//                return true;
//            }
//            String end = s.substring(0, currentIndex+1);
//            String endReversed = new StringBuilder(end).reverse().toString();
//            for (Placeholder p : PlaceholderRegistry.getPlaceholdersList()) {
//                if (endReversed.startsWith(new StringBuilder("{\"placeholder\":\"" + p.id + "\"}").reverse().toString())) {
//                    return true;
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;
//    }

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
            if (nextValue != null) {
                return true;
            }
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
        if (!logCooldown.containsKey(error)) {
            LOGGER.error(error);
            logCooldown.put(error, System.currentTimeMillis());
        }
    }

    private static void updateLogHandler() {
        long now = System.currentTimeMillis();
        List<String> remove = new ArrayList<>();
        for (Map.Entry<String, Long> m : logCooldown.entrySet()) {
            if ((m.getValue() + 10000) <= now) {
                remove.add(m.getKey());
            }
        }
        for (String s : remove) {
            logCooldown.remove(s);
        }
    }

}
