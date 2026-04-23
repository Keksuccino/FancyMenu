package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Base64EncodePlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, String> ENCODE_CACHE = new ConcurrentHashMap<>();

    public Base64EncodePlaceholder() {
        super("base64_encode");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        try {
            String text = dps.values.get("text");
            if (text == null) {
                text = dps.values.get("value");
            }
            if (text == null) {
                LOGGER.error("[FANCYMENU] Missing 'text' value for 'Encode To Base64' placeholder: {}", dps.placeholderString);
                return null;
            }
            String cached = ENCODE_CACHE.get(text);
            if (cached != null) {
                return cached;
            }
            String encoded = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
            ENCODE_CACHE.put(text, encoded);
            return encoded;
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to encode Base64 for placeholder: {}", dps.placeholderString, ex);
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> values = new ArrayList<>();
        values.add("text");
        values.add("value");
        return values;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.base64_encode");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.base64_encode.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> values = new HashMap<>();
        values.put("text", "text");
        return DeserializedPlaceholderString.build(this.getIdentifier(), values);
    }

}
