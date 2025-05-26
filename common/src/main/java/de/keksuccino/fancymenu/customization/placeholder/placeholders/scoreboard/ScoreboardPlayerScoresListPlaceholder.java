package de.keksuccino.fancymenu.customization.placeholder.placeholders.scoreboard;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardPlayerScoresListPlaceholder extends Placeholder {

    public ScoreboardPlayerScoresListPlaceholder() {
        super("scoreboard_player_scores_list");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        ClientLevel level = Minecraft.getInstance().level;
        String playerName = dps.values.get("player");
        String separator = dps.values.get("separator");
        String format = dps.values.get("format");
        
        if (separator == null) separator = ", ";
        if (format == null) format = "%objective%: %score%";
        
        if ((level != null) && (playerName != null)) {
            Scoreboard scoreboard = level.getScoreboard();
            ScoreHolder scoreHolder = ScoreHolder.forNameOnly(playerName);
            Object2IntMap<Objective> scores = scoreboard.listPlayerScores(scoreHolder);
            
            final String finalFormat = format;
            return scores.object2IntEntrySet().stream()
                    .map(entry -> finalFormat
                            .replace("%objective%", entry.getKey().getName())
                            .replace("%score%", String.valueOf(entry.getIntValue())))
                    .collect(Collectors.joining(separator));
        }
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("player", "separator", "format");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.scoreboard.player_scores_list");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.scoreboard.player_scores_list.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.scoreboard");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("player", "Player1");
        values.put("separator", ", ");
        values.put("format", "%objective%: %score%");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
