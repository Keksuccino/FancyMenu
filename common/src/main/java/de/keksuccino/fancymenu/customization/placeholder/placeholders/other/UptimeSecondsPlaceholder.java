package de.keksuccino.fancymenu.customization.placeholder.placeholders.other;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class UptimeSecondsPlaceholder extends Placeholder {

    private volatile long registeredAtMs;

    public UptimeSecondsPlaceholder() {
        super("uptime_seconds");
    }

    @Override
    public void onRegistered() {
        this.registeredAtMs = System.currentTimeMillis();
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        long timestamp = this.registeredAtMs;
        if (timestamp <= 0L) {
            return "0";
        }
        long uptimeMs = System.currentTimeMillis() - timestamp;
        if (uptimeMs < 0L) {
            uptimeMs = 0L;
        }
        return Long.toString(uptimeMs / 1000L);
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.uptime_seconds");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.uptime_seconds.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.other");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
