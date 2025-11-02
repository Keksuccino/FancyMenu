package de.keksuccino.fancymenu.customization.placeholder.placeholders.scoreboard;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.scores.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class ScoreboardHasScorePlaceholder extends Placeholder {

    public ScoreboardHasScorePlaceholder() {
        super("scoreboard_has_score");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        ClientLevel level = Minecraft.getInstance().level;
        String playerName = dps.values.get("player");
        String objectiveName = dps.values.get("objective");
        
        if ((level != null) && (playerName != null) && (objectiveName != null)) {
            Scoreboard scoreboard = level.getScoreboard();
            Objective objective = scoreboard.getObjective(objectiveName);
            
            if (objective != null) {
                return String.valueOf(scoreboard.hasPlayerScore(playerName, objective));
            }
        }
        return "false";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("player", "objective");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.scoreboard.has_score");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.scoreboard.has_score.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.scoreboard");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("player", "Player1");
        values.put("objective", "score");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
