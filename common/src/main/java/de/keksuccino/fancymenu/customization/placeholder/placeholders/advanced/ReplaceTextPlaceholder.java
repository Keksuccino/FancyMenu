package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ReplaceTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public ReplaceTextPlaceholder() {
        super("replace_text");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String text = dps.values.get("text");
        String search = dps.values.get("search");
        String replacement = dps.values.get("replacement");
        boolean useRegex = SerializationUtils.deserializeBoolean(false, dps.values.get("use_regex"));
        boolean replaceAll = SerializationUtils.deserializeBoolean(true, dps.values.get("replace_all"));
        
        if ((text == null) || (search == null)) {
            return "";
        }
        
        if (replacement == null) {
            replacement = "";
        }
        
        try {
            if (useRegex) {
                Pattern pattern = Pattern.compile(search);
                if (replaceAll) {
                    return pattern.matcher(text).replaceAll(replacement);
                } else {
                    return pattern.matcher(text).replaceFirst(replacement);
                }
            } else {
                String resolvedSearch = unescapeLiteralValue(search);
                String resolvedReplacement = unescapeLiteralValue(replacement);
                if (replaceAll) {
                    return text.replace(resolvedSearch, resolvedReplacement);
                } else {
                    int index = text.indexOf(resolvedSearch);
                    if (index < 0) {
                        return text;
                    }
                    return text.substring(0, index) + resolvedReplacement + text.substring(index + resolvedSearch.length());
                }
            }
        } catch (PatternSyntaxException e) {
            LOGGER.error("[FANCYMENU] Invalid regex pattern in 'Replace Text' placeholder: " + search, e);
            return text;
        } catch (Exception e) {
            LOGGER.error("[FANCYMENU] Error in 'Replace Text' placeholder: " + dps.placeholderString, e);
            return text;
        }
    }

    @NotNull
    private static String unescapeLiteralValue(@NotNull String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean escaped = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                if ((c == '\\') || (c == '\"') || (c == '{') || (c == '}')) {
                    result.append(c);
                } else {
                    result.append('\\').append(c);
                }
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            result.append(c);
        }

        if (escaped) {
            result.append('\\');
        }

        return result.toString();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("text", "search", "replacement", "use_regex", "replace_all");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.replace_text");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.replace_text.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("text", "Hello World! This is a test.");
        values.put("search", "World");
        values.put("replacement", "FancyMenu");
        values.put("use_regex", "false");
        values.put("replace_all", "true");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
