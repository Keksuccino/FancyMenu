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
import java.util.*;

public class MaxNumberPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public MaxNumberPlaceholder() {
        super("maxnum");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String firstNumber = dps.values.get("first");
        String secondNumber = dps.values.get("second");
        if ((firstNumber != null) && (secondNumber != null)) {
            try {
                if (MathUtils.isDouble(firstNumber) && MathUtils.isDouble(secondNumber)) {
                    double first = Double.parseDouble(firstNumber);
                    double second = Double.parseDouble(secondNumber);
                    return (Math.max(first, second) == first) ? firstNumber : secondNumber;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        LOGGER.error("[FANCYMENU] Failed to parse 'Max Number' placeholder: " + dps.placeholderString);
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("first");
        l.add("second");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.max_number");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.max_number.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("first", "10");
        values.put("second", "15");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
