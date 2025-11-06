package de.keksuccino.fancymenu.customization.placeholder.placeholders.scoreboard;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.SerializationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class ScoreboardObjectiveDisplayNamePlaceholder extends Placeholder {

    public ScoreboardObjectiveDisplayNamePlaceholder() {
        super("scoreboard_objective_display_name");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        ClientLevel level = Minecraft.getInstance().level;
        String objectiveName = dps.values.get("objective");
        boolean asJson = SerializationUtils.deserializeBoolean(false, dps.values.get("as_json"));
        if ((level != null) && (objectiveName != null)) {
            Scoreboard scoreboard = level.getScoreboard();
            Objective objective = scoreboard.getObjective(objectiveName);
            if (objective != null) {
                if (asJson) {
                    String name = ComponentSerialization.CODEC.encodeStart(level.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), objective.getDisplayName()).getOrThrow().toString();
                    if (name.startsWith("\"") && name.endsWith("\"")) name = name.substring(1, name.length() - 1);
                    return name;
                } else {
                    return objective.getDisplayName().getString();
                }
            }
        }
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("objective", "as_json");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.scoreboard.objective_display_name");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.scoreboard.objective_display_name.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.scoreboard");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("objective", "score");
        values.put("as_json", "false");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
