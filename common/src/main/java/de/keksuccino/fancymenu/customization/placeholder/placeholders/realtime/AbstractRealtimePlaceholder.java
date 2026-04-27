package de.keksuccino.fancymenu.customization.placeholder.placeholders.realtime;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;

abstract class AbstractRealtimePlaceholder extends Placeholder {

    protected static final String TIMEZONE_VALUE_NAME = "timezone";
    protected static final String TIMEZONE_DEFAULT_VALUE = "system";

    protected AbstractRealtimePlaceholder(@NotNull String id) {
        super(id);
    }

    protected final @NotNull Calendar getCalendar(@NotNull DeserializedPlaceholderString dps) {
        return Calendar.getInstance(this.getTimeZone(dps));
    }

    protected final @NotNull DeserializedPlaceholderString buildDefaultPlaceholderString() {
        return this.buildDefaultPlaceholderString(new LinkedHashMap<>());
    }

    protected final @NotNull DeserializedPlaceholderString buildDefaultPlaceholderString(@NotNull LinkedHashMap<String, String> values) {
        LinkedHashMap<String, String> defaultValues = new LinkedHashMap<>(values);
        defaultValues.putIfAbsent(TIMEZONE_VALUE_NAME, TIMEZONE_DEFAULT_VALUE);
        return new DeserializedPlaceholderString(this.getIdentifier(), defaultValues, "");
    }

    protected static @NotNull List<String> getTimezoneValueNames() {
        return List.of(TIMEZONE_VALUE_NAME);
    }

    protected static @NotNull String formatToFancyDateTime(int in) {
        String s = Integer.toString(in);
        if (s.length() < 2) {
            s = "0" + s;
        }
        return s;
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.realtime");
    }

    private @NotNull TimeZone getTimeZone(@NotNull DeserializedPlaceholderString dps) {
        String timezoneId = dps.values.get(TIMEZONE_VALUE_NAME);
        if ((timezoneId == null) || timezoneId.isBlank() || TIMEZONE_DEFAULT_VALUE.equalsIgnoreCase(timezoneId)) {
            return TimeZone.getDefault();
        }
        try {
            return TimeZone.getTimeZone(ZoneId.of(timezoneId.trim()));
        } catch (Exception ignored) {
            return TimeZone.getDefault();
        }
    }

}
