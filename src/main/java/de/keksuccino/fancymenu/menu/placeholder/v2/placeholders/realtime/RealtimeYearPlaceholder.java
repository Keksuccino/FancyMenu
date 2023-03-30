
package de.keksuccino.fancymenu.menu.placeholder.v2.placeholders.realtime;

import de.keksuccino.fancymenu.menu.placeholder.v2.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.menu.placeholder.v2.Placeholder;
import de.keksuccino.konkrete.localization.Locals;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Calendar;
import java.util.List;

public class RealtimeYearPlaceholder extends Placeholder {

    public RealtimeYearPlaceholder() {
        super("realtimeyear");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Calendar c = Calendar.getInstance();
        return "" + c.get(Calendar.YEAR);
    }

    @Override
    public  List<String> getValueNames() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return Locals.localize("fancymenu.helper.ui.dynamicvariabletextfield.variables.realtimeyear");
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
