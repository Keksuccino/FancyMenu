package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SwitchCasePlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public SwitchCasePlaceholder() {
        super("switch_case");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String value = dps.values.get("value");
        String cases = dps.values.get("cases");
        String defaultCase = dps.values.get("default");

        if (value != null && cases != null) {
            try {
                String[] casesPairs = cases.split(",");
                for (String pair : casesPairs) {
                    String[] keyValue = pair.trim().split(":");
                    if (keyValue.length == 2) {
                        String caseValue = keyValue[0].trim();
                        String result = keyValue[1].trim();
                        if (value.equals(caseValue)) {
                            return result;
                        }
                    }
                }
                // Return default case if provided, otherwise null
                return defaultCase;
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to parse 'Switch Case' placeholder: " + dps.placeholderString, ex);
            }
        }
        LOGGER.error("[FANCYMENU] Failed to parse 'Switch Case' placeholder: " + dps.placeholderString);
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("value");
        l.add("cases");
        l.add("default");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.switch_case");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.switch_case.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("value", "1");
        values.put("cases", "1:first case,2:second case,3:third case");
        values.put("default", "default case");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}