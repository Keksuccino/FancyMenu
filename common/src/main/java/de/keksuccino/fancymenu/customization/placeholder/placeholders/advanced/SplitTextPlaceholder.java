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
import java.util.*;

public class SplitTextPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public SplitTextPlaceholder() {
        super("split_text");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        try {

            String input = dps.values.get("input");
            String regex = dps.values.get("regex");
            int maxParts = SerializationUtils.deserializeNumber(Integer.class, -1, dps.values.get("max_parts"));
            int splitIndex = SerializationUtils.deserializeNumber(Integer.class, -1, dps.values.get("split_index"));

            if ((maxParts > 0) && (maxParts-1 < splitIndex)) {
                LOGGER.error("[FANCYMENU] Failed to parse 'Split Text' placeholder! Max_parts is smaller than split_index!");
                return "Failed to parse! Max_parts is smaller than split_index!";
            }

            if ((input != null) && (regex != null)) {
                String[] inSplit;
                if (maxParts <= 0) {
                    inSplit = input.split(regex);
                } else {
                    inSplit = input.split(regex, maxParts);
                }
                if (inSplit.length-1 < splitIndex) {
                    LOGGER.error("[FANCYMENU] Failed to parse 'Split Text' placeholder! There is no part with index " + splitIndex + "! Only " + inSplit.length + " parts found! Keep in mind first part index is 0!");
                    return "Failed to parse! There is no part with index " + splitIndex + "! Only " + inSplit.length + " parts found! Keep in mind first part index is 0!";
                }
                return inSplit[splitIndex];
            }

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to parse 'Split Text' placeholder!", ex);
        }

        return null;

    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("input");
        l.add("regex");
        l.add("max_parts");
        l.add("split_index");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.split_text");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.split_text.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("input", "some text");
        m.put("regex", "e");
        m.put("max_parts", "2");
        m.put("split_index", "0");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
