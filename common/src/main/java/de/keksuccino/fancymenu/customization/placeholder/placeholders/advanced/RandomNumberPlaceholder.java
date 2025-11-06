package de.keksuccino.fancymenu.customization.placeholder.placeholders.advanced;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class RandomNumberPlaceholder extends Placeholder {

    public RandomNumberPlaceholder() {
        super("random_number");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String min = dps.values.get("min");
        String max = dps.values.get("max");
        if ((min != null) && (max != null) && MathUtils.isInteger(min) && MathUtils.isInteger(max)) {
            int minInt = Integer.parseInt(min);
            int maxInt = Integer.parseInt(max);
            if (minInt == maxInt) return "" + minInt;
            return "" + MathUtils.getRandomNumberInRange(Math.min(minInt, maxInt), Math.max(minInt, maxInt));
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("min");
        l.add("max");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.random_number");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.random_number.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.advanced");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("min", "1");
        m.put("max", "30");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}
