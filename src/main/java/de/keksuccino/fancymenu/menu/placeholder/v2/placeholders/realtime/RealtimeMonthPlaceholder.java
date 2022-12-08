//TODO übernehmen
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.realtime;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.localization.Locals;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.List;

public class RealtimeMonthPlaceholder extends Placeholder {

    public RealtimeMonthPlaceholder() {
        super("realtimemonth");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Calendar c = Calendar.getInstance();
        return formatToFancyDateTime(c.get(Calendar.MONTH) + 1);
    }

    private static String formatToFancyDateTime(int in) {
        String s = "" + in;
        if (s.length() < 2) {
            s = "0" + s;
        }
        return s;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.realtimemonth");
    }

    @Override
    public List<String> getDescription() {
        return null;
    }

    @Override
    public String getCategory() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.categories.realtime");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        return dps;
    }

}
