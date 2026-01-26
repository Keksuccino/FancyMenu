package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.TextCaseUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AlternatingCaseTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public AlternatingCaseTextPlaceholder() {
        super("alternating_case_text");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        try {
            String input = dps.values.get("text");
            if (input != null) {
                return TextCaseUtils.toAlternatingCase(input);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to parse 'Alternating Case Text' placeholder!", ex);
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("text");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.alternating_case_text");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.alternating_case_text.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("text", "alternating case");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
