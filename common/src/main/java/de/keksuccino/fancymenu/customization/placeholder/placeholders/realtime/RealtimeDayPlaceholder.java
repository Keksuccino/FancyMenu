package de.keksuccino.fancymenu.customization.placeholder.placeholders.realtime;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.List;

public class RealtimeDayPlaceholder extends AbstractRealtimePlaceholder {

    public RealtimeDayPlaceholder() {
        super("realtimeday");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Calendar c = this.getCalendar(dps);
        return formatToFancyDateTime(c.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return getTimezoneValueNames();
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.realtime_day");
    }

    @Override
    public List<String> getDescription() {
        return null;
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return this.buildDefaultPlaceholderString();
    }

}
