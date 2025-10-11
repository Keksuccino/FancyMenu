package de.keksuccino.fancymenu.customization.placeholder.placeholders.player;

import de.keksuccino.fancymenu.customization.listener.listeners.Listeners;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class LastDeathMessagePlaceholder extends Placeholder {

    public LastDeathMessagePlaceholder() {
        super("lastdeathmessage");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        boolean asJsonComponent = SerializationUtils.deserializeBoolean(false, dps.values.get("as_json_component"));
        String cached = asJsonComponent ? Listeners.ON_DEATH.getLastDeathReasonComponent() : Listeners.ON_DEATH.getLastDeathReasonString();
        return cached != null ? cached : "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("as_json_component");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.last_death_message");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.last_death_message.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.player");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("as_json_component", "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
