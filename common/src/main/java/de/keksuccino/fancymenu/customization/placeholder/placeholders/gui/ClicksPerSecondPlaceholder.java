package de.keksuccino.fancymenu.customization.placeholder.placeholders.gui;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.input.ClicksPerSecondTracker;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ClicksPerSecondPlaceholder extends Placeholder {

    public ClicksPerSecondPlaceholder() {
        super("clicks_per_second");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String mouseButton = dps.values.get("mouse_button");
        if (mouseButton == null) {
            mouseButton = "left";
        }

        boolean rightMouseButton = mouseButton.equalsIgnoreCase("right");
        return Integer.toString(ClicksPerSecondTracker.getClicksPerSecond(rightMouseButton));
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("mouse_button");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.clicks_per_second");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.clicks_per_second.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.requirements.categories.gui");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new HashMap<>();
        values.put("mouse_button", "left");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
