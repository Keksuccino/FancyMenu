package de.keksuccino.fancymenu.menu.placeholder.v2;

// {"placeholder":"some_placeholder","values":{"some_value_1":"content","some_value_2":"content"}}

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceholderHandler {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<String, Placeholder> placeholders = new HashMap<>();

    public static void registerPlaceholder(Placeholder placeholder) {
        if (placeholder != null) {
            if (!placeholders.containsKey(placeholder.getIdentifier())) {
                placeholders.put(placeholder.getIdentifier(), placeholder);
            } else {
                LOGGER.error("Unable to register placeholder! Placeholder ID already registered: " + placeholder.getIdentifier());
            }
        } else {
            LOGGER.error("Unable to register placeholder! Placeholder was NULL!");
        }
    }

    @Nullable
    public static Placeholder getPlaceholderForIdentifier(String identifier) {
        return placeholders.get(identifier);
    }

    public static Map<String, Placeholder> getPlaceholders() {
        return placeholders;
    }

    public static List<Placeholder> getPlaceholdersList() {
        List<Placeholder> l = new ArrayList<>();
        l.addAll(placeholders.values());
        return l;
    }

    @NotNull
    public static String replacePlaceholders(@NotNull String in) {
        List<String> l = innerReplacePlaceholders(in);
        if (l != null) {
            return l.get(1);
        }
        return in;
    }

    @Nullable
    private static List<String> innerReplacePlaceholders(String in) {
        try {
            String out = in;
            int skipToIndex = 0;
            int currentIndex = 0;
            for (char c : in.toCharArray()) {
                if (currentIndex >= skipToIndex) {
                    String s = String.valueOf(c);
                    if (s.equals("{") && isStartIndexOfPlaceholder(in, currentIndex)) {
                        int endIndex = findEndIndex(in, currentIndex);
                        if (endIndex > currentIndex) {
                            skipToIndex = endIndex;
                            String ps = in.substring(currentIndex, endIndex);
                            LOGGER.info("#################### PS: " + ps);
                            String finalPs = ps;
                            int valueStartIndex = findValueStartIndex(finalPs);
                            if (valueStartIndex != -1) {
                                int valueEndIndex = findEndIndex(finalPs, valueStartIndex+10);
                                LOGGER.info("################## VALUE START: " + finalPs.substring(valueStartIndex+10));
                                if (valueEndIndex > valueStartIndex) {
                                    String valueString = finalPs.substring(valueStartIndex+1, valueEndIndex-1);
                                    String valueStringNew = valueString.replace("{", "%json_start_section%").replace("}", "%json_end_section%");
                                    finalPs = finalPs.replace(valueString, valueStringNew);
                                    Gson gson = new Gson();
                                    DeserializedPlaceholderString dps = gson.fromJson(finalPs, DeserializedPlaceholderString.class);
                                    Map<String, String> fixedValues = new HashMap<>();
                                    for (Map.Entry<String, String> m : dps.values.entrySet()) {
                                        fixedValues.put(m.getKey(), m.getValue().replace("%json_start_section%", "{").replace("%json_end_section%", "}"));
                                    }
                                    dps.values = fixedValues;
                                    if (dps != null) {
                                        for (String value : dps.values.values()) {
                                            List<String> l2 = innerReplacePlaceholders(value);
                                            finalPs = finalPs.replace(l2.get(0), l2.get(1));
                                        }
                                    }
                                    //finalPs should be a single placeholder string now without nested PS's, so replace this single PS with the actual text now
                                    Placeholder placeholderInstance = getPlaceholderForIdentifier(dps.placeholder);
                                    if (placeholderInstance != null) {
                                        DeserializedPlaceholderString ps2 = gson.fromJson(finalPs, DeserializedPlaceholderString.class);
                                        if (ps2 != null) {
                                            out = placeholderInstance.getReplacementFor(ps2);
                                        }
                                    }
                                    out = out.replace(ps, finalPs);
                                } else {
                                    LOGGER.error("Unable to replace placeholders! ValueEndIndex <= ValueStartIndex!");
                                    return null;
                                }
                            } else {
                                LOGGER.error("Unable to replace placeholders! ValueStartIndex is -1!");
                                return null;
                            }
                        } else {
                            LOGGER.error("Unable to replace placeholders! EndIndex <= CurrentIndex!");
                            return null;
                        }
                    }
                }
                currentIndex++;
            }
            List<String> l = new ArrayList<>();
            l.add(in);
            l.add(out);
            return l;
        } catch (Exception e) {
            e.printStackTrace();
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
        if (in.substring(startIndex).startsWith("{")) {
            int currentIndex = startIndex+1;
            int depth = 0;
            for (char c : in.substring(startIndex+1).toCharArray()) {
                if (String.valueOf(c).equals("{")) {
                    depth++;
                } else if (String.valueOf(c).equals("}")) {
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

    private static boolean isStartIndexOfPlaceholder(String in, int startIndex) {
        return in.substring(startIndex).startsWith("{\"placeholder\":");
    }

    //    public static String replacePlaceholders(String in) {
//
//        try {
//            int skipToIndex = 0;
//            int currentIndex = 0;
//            for (char c : in.toCharArray()) {
//                if (currentIndex >= skipToIndex) {
//                    String s = String.valueOf(c);
//                    if (s.equals("{") && isStartOfPlaceholder(in, currentIndex)) {
//                        int endIndex = findEndIndexOfPlaceholder(in, currentIndex);
//                        if (endIndex >= currentIndex) {
//                            skipToIndex = endIndex;
//                            Gson gson = new Gson();
//                            DeserializedPlaceholderString sp = gson.fromJson(in.substring(currentIndex, endIndex), DeserializedPlaceholderString.class);
//                            if (sp != null) {
//                                Map<String, String> finalValues = new HashMap<>();
//                                for (Map.Entry<String, String> m : sp.values.entrySet()) {
//                                    finalValues.put(m.getKey(), replacePlaceholders(m.getValue()));
//                                }
//                                sp.values = finalValues;
//                                //TODO replace
//                            }
//                        } else {
//                            return in;
//                        }
//                    }
//                }
//                currentIndex++;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return in;
//
//    }

//    private void findAllPlaceholders(String in, List<String> list) {
//
//        String newIn = in;
//
//        int skipToIndex = 0;
//        int currentIndex = 0;
//        for (char c : in.toCharArray()) {
//            if (currentIndex >= skipToIndex) {
//                String s = String.valueOf(c);
//                if (s.equals("{") && isStartOfPlaceholder(in, currentIndex)) {
//                    int endIndex = findEndIndexOfPlaceholder(in, currentIndex);
//                    if (endIndex >= currentIndex) {
//                        String ph = in.substring(currentIndex, endIndex);
//                        list.add(ph);
//                        Gson gson = new Gson();
//                        DeserializedPlaceholderString sp = gson.fromJson(ph, DeserializedPlaceholderString.class);
//                        if (sp != null) {
//                            for (String value : sp.values.values()) {
//                                findAllPlaceholders(value, list);
//                            }
//                        }
//                    }
//                }
//            }
//            currentIndex++;
//        }
//
//    }

}
