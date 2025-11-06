package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.LevelData;
import de.keksuccino.fancymenu.util.WorldUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class WorldSaveNamesPlaceholder extends Placeholder {

    public WorldSaveNamesPlaceholder() {
        super("level_save_names");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String separator = dps.values.get("separator");
        if (separator == null) {
            separator = ", ";
        }
        
        List<LevelData> levelsData = WorldUtils.getLevelsAsData();
        
        return levelsData.stream()
                .map(data -> data.settings_level_name)
                .collect(Collectors.joining(separator));
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("separator");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.world.level_save_names");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.world.level_save_names.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("separator", ", ");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
