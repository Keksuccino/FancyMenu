package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.net.objecthunter.exp4j.Expression;
import de.keksuccino.net.objecthunter.exp4j.ExpressionBuilder;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class CalculatorPlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public CalculatorPlaceholder() {
        super("calc");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String decimalString = dps.values.get("decimal");
        boolean decimal = true;
        if ((decimalString != null) && decimalString.equalsIgnoreCase("false")) {
            decimal = false;
        }
        String ex = dps.values.get("expression");
        if (ex != null) {
            try {
                Expression expression = new ExpressionBuilder(ex).build();
                if (expression.validate().isValid()) {
                    if (decimal) {
                        return "" + expression.evaluate();
                    }
                    return "" + Math.round(expression.evaluate());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        LOGGER.error("[FANCYMENU] Failed to parse Calculator placeholder: " + dps.placeholderString);
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("expression");
        l.add("decimal");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.calc");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.calc.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("decimal", "true");
        m.put("expression", "2 + 1 - 10");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
