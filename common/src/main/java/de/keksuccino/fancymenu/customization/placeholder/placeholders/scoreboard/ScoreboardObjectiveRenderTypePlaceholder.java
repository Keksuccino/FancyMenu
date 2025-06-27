package de.keksuccino.fancymenu.customization.placeholder.placeholders.scoreboard;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class ScoreboardObjectiveRenderTypePlaceholder extends Placeholder {

    public ScoreboardObjectiveRenderTypePlaceholder() {
        super("scoreboard_objective_render_type");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        ClientLevel level = Minecraft.getInstance().level;
        String objectiveName = dps.values.get("objective");
        
        if ((level != null) && (objectiveName != null)) {
            Scoreboard scoreboard = level.getScoreboard();
            Objective objective = scoreboard.getObjective(objectiveName);
            
            if (objective != null) {
                return objective.getRenderType().getId();
            }
        }
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("objective");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.scoreboard.objective_render_type");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.scoreboard.objective_render_type.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.scoreboard");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("objective", "score");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
