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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MathRoundPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final int DEFAULT_DECIMALS = -1;

    public MathRoundPlaceholder() {
        super("math_round");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String numberValue = dps.values.get("num");
        if (numberValue == null) {
            LOGGER.error("[FANCYMENU] Missing 'num' value for 'Round (Math)' placeholder: {}", dps.placeholderString);
            return null;
        }
        numberValue = numberValue.trim();
        if (!MathUtils.isDouble(numberValue)) {
            LOGGER.error("[FANCYMENU] Invalid 'num' value for 'Round (Math)' placeholder: {}", dps.placeholderString);
            return null;
        }

        int decimals = parseDecimals(dps.values.get("decimals"), dps.placeholderString);
        try {
            if (decimals > 0) {
                BigDecimal rounded = new BigDecimal(numberValue).setScale(decimals, RoundingMode.HALF_UP);
                return rounded.stripTrailingZeros().toPlainString();
            }
            double parsedNumber = Double.parseDouble(numberValue);
            return Long.toString(Math.round(parsedNumber));
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to parse 'Round (Math)' placeholder: " + dps.placeholderString, ex);
            return null;
        }
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("num");
        l.add("decimals");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.math_round");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.math_round.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("num", "3.14");
        values.put("decimals", "-1");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

    private int parseDecimals(@Nullable String decimalsValue, @NotNull String placeholderString) {
        if (decimalsValue == null) {
            return DEFAULT_DECIMALS;
        }
        String trimmed = decimalsValue.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_DECIMALS;
        }
        if (MathUtils.isInteger(trimmed)) {
            return Integer.parseInt(trimmed);
        }
        LOGGER.error("[FANCYMENU] Invalid 'decimals' value for 'Round (Math)' placeholder: {} (value: {})", placeholderString, decimalsValue);
        return DEFAULT_DECIMALS;
    }

}
