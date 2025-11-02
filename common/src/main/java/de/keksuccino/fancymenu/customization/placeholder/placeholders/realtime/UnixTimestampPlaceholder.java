package de.keksuccino.fancymenu.customization.placeholder.placeholders.realtime;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.List;

public class UnixTimestampPlaceholder extends Placeholder {

    public UnixTimestampPlaceholder() {
        super("unix_time");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return "" + System.currentTimeMillis();
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return null;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.unix_time");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.unix_time"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.realtime");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
