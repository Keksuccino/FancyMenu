//TODO übernehmen
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.realtime;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.localization.Locals;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Calendar;
import java.util.List;

public class RealtimeHourPlaceholder extends Placeholder {

    public RealtimeHourPlaceholder() {
        super("realtimehour");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Calendar c = Calendar.getInstance();
        return formatToFancyDateTime(c.get(Calendar.HOUR_OF_DAY));
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
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.realtimehour");
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
    public @Nonnull DeserializedPlaceholderString getDefaultPlaceholderString() {
        DeserializedPlaceholderString dps = new DeserializedPlaceholderString();
        dps.placeholder = this.getIdentifier();
        return dps;
    }

}
