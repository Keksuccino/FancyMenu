package de.keksuccino.fancymenu.menu.fancy.placeholder;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//WIP! DON'T USE!
//WIP! DON'T USE!
//WIP! DON'T USE!
//WIP! DON'T USE!
//WIP! DON'T USE!
//WIP! DON'T USE!
public class PlaceholderHandler {

    private static final Logger LOGGER = LogManager.getLogger("fancymenu/PlaceholderHandler");

    public static Placeholder deserializePlaceholder(String placeholderString) {
        try {
            if ((placeholderString != null) && placeholderString.startsWith("%{") && placeholderString.endsWith("}%")) {
                String identifier = placeholderString.substring(2);
                LOGGER.info("################## 1: identifier: " + identifier);
                if (identifier.contains(":")) {
                    identifier = identifier.split("[:]", 2)[0];
                    LOGGER.info("################## 2: identifier: " + identifier);
                    String propsRaw = placeholderString.replace("%{" + identifier + ":", "");
                    LOGGER.info("################## 3: propsRaw: " + propsRaw);
                    propsRaw = new StringBuilder(new StringBuilder(propsRaw).reverse().substring(2)).reverse().toString();
                    LOGGER.info("################## 4: propsRaw: " + propsRaw);
                    propsRaw = "{\"properties\":" + propsRaw + "}";
                    LOGGER.info("################## 5: propsRaw: " + propsRaw);
                    Gson g = new Gson();
                    Placeholder p = g.fromJson(propsRaw, Placeholder.class);
                    if (p != null) {
                        LOGGER.info("################## 6: PROPS NOT NULL!");
                        p.raw = placeholderString;
                        p.identifier = identifier;
                    }
                    return p;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //Only works for one level of properties
    //TODO Make this work with placeholders nested in property (sub-placeholders)
    public static List<String> parseRawProperties(String placeholder, List<String> l) {
        if ((placeholder != null) && placeholder.startsWith("%{") && placeholder.endsWith("}%")) {
            boolean inProperty = false;
            boolean foundColon = false;
            int depth = 0;
            int startIndex = -1;
            int endIndex = -1;
            int index = 0;
            for (char c: placeholder.toCharArray()) {
                if (!inProperty) {
                    if (c == '"') {
                        inProperty = true;
                        startIndex = index;
                    }
                    //%{placeholder5:{"arg1":"some stuff";"arg2":"more stuff";}}%
                } else {
                    if (c == ':') {
                        String s = placeholder.substring(index);
                        if (s.startsWith(":\"")) {
                            foundColon = true;
                        }
                    }
                    if (c == '"') {
                        if (foundColon) {
                            String s = placeholder.substring(index);
                            if (s.startsWith("\";")) {
                                depth--;
                                if (depth <= 0) {
                                    endIndex = index+1;
                                }
                            } else {
                                depth++;
                            }
                        }
                    }
                }
                if ((startIndex != -1) && (endIndex != -1)) {
                    l.add(placeholder.substring(startIndex, endIndex));
                    inProperty = false;
                    foundColon = false;
                    depth = 0;
                    startIndex = -1;
                    endIndex = -1;
                }
                index++;
            }
        }
        return l;
    }

    public static List<String> parseRawPlaceholders(String text, List<String> l) {
        boolean inPlaceholder = false;
        int depth = 0;
        int startIndex = -1;
        int endIndex = -1;
        int index = 0;
        for (char c : text.toCharArray()) {
            if (!inPlaceholder) {
                if (c == '%') {
                    String s = text.substring(index);
                    if (s.startsWith("%{")) {
                        inPlaceholder = true;
                        startIndex = index;
                    }
                }
            } else {
                if (c == '{') {
                    String s = text.substring(index-1);
                    if (s.startsWith("%{")) {
                        depth++;
                    }
                }
                if (c == '}') {
                    String s = text.substring(index);
                    if (s.startsWith("}%")) {
                        depth--;
                        if (depth <= 0) {
                            inPlaceholder = false;
                            endIndex = index + 2;
                        }
                    }
                }
            }
            if ((startIndex > -1) && (endIndex > -1)) {
                l.add(text.substring(startIndex, endIndex));
                startIndex = -1;
                endIndex = -1;
                inPlaceholder = false;
                depth = 0;
            }
            index++;
        }
        for (String s : l) {
            Placeholder p = deserializePlaceholder(s);
            if (p != null) {
                for (String s2 : p.properties.values()) {
                    List<String> l2 = parseRawPlaceholders(s2, new ArrayList<>());
                    l.addAll(l2);
                }
            }
        }
        return l;
    }

    public static class Placeholder {

        public String identifier;
        public String raw;
        public Map<String, String> properties;

    }

}
