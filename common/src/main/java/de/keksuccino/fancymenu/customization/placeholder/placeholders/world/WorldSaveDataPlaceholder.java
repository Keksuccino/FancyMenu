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

public class WorldSaveDataPlaceholder extends Placeholder {

    public WorldSaveDataPlaceholder() {
        super("level_save_data");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String levelName = dps.values.get("level_name");
        if (levelName == null) {
            return "";
        }
        
        List<LevelData> levelsData = WorldUtils.getLevelsAsData();
        
        for (LevelData data : levelsData) {
            if (data.settings_level_name.equals(levelName)) {
                return data.serialize();
            }
        }
        
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("level_name");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.world.level_save_data");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.world.level_save_data.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("level_name", "World");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
