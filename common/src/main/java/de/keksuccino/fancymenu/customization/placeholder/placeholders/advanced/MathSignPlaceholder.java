package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.MathUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MathSignPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public MathSignPlaceholder() {
        super("math_sign");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String num = dps.values.get("num");
        if (num != null) {
            try {
                if (MathUtils.isDouble(num)) {
                    double numD = Double.parseDouble(num);
                    if (numD > 0) return "1";
                    if (numD < 0) return "-1";
                    return "0";
                }
            } catch (Exception ex) {
                LOGGER.error("[FANCYMENU] Failed to parse 'Sign (Math)' placeholder: " + dps.placeholderString, ex);
            }
        }
        LOGGER.error("[FANCYMENU] Failed to parse 'Sign (Math)' placeholder: " + dps.placeholderString);
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("num");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.math_sign");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.math_sign.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("num", "-3.14");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}