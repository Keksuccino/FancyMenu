package de.keksuccino.fancymenu.customization.placeholder.placeholders.client;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.customization.world.LastWorldHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class LastWorldOrServerPlaceholder extends Placeholder {

    public LastWorldOrServerPlaceholder() {
        super("last_world_server");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String type = dps.values.get("type");
        if (type != null) {
            if (type.equals("both")) {
                return LastWorldHandler.getLastWorld();
            }
            if (type.equals("server")) {
                if (LastWorldHandler.isLastWorldServer()) {
                    return LastWorldHandler.getLastWorld();
                }
            }
            if (type.equals("world")) {
                if (!LastWorldHandler.isLastWorldServer()) {
                    return LastWorldHandler.getLastWorld();
                }
            }
        }
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("type");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.last_world_server");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.last_world_server.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.fancymenu.editor.dynamicvariabletextfield.categories.client");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("type", "both");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
