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

public class MinNumberPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public MinNumberPlaceholder() {
        super("minnum");
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
                    return (Math.min(first, second) == first) ? firstNumber : secondNumber;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        LOGGER.error("[FANCYMENU] Failed to parse 'Min Number' placeholder: " + dps.placeholderString);
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
        return I18n.get("fancymenu.placeholders.min_number");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.min_number.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("first", "10");
        values.put("second", "15");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
