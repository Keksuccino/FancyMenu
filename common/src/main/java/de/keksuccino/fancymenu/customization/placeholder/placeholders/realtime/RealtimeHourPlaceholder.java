package de.keksuccino.fancymenu.customization.placeholder.placeholders.realtime;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationHelper;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;

public class RealtimeHourPlaceholder extends AbstractRealtimePlaceholder {

    public RealtimeHourPlaceholder() {
        super("realtimehour");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Calendar c = this.getCalendar(dps);
        boolean twelveHourFormat = SerializationHelper.INSTANCE.deserializeBoolean(false, dps.values.get("twelve_hour_format"));
        int hour = c.get(twelveHourFormat ? Calendar.HOUR : Calendar.HOUR_OF_DAY);
        if (twelveHourFormat && hour == 0) {
            hour = 12;
        }
        return formatToFancyDateTime(hour);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("twelve_hour_format", TIMEZONE_VALUE_NAME);
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.realtime_hour");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.realtime_hour.desc"));
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("twelve_hour_format", "false");
        return this.buildDefaultPlaceholderString(values);
    }

}
