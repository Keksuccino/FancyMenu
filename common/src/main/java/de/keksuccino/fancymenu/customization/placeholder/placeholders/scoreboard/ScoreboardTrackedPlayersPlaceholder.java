package de.keksuccino.fancymenu.customization.placeholder.placeholders.scoreboard;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardTrackedPlayersPlaceholder extends Placeholder {

    public ScoreboardTrackedPlayersPlaceholder() {
        super("scoreboard_tracked_players");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        ClientLevel level = Minecraft.getInstance().level;
        String separator = dps.values.get("separator");
        if (separator == null) separator = ", ";
        
        if (level != null) {
            Scoreboard scoreboard = level.getScoreboard();
            Collection<ScoreHolder> trackedPlayers = scoreboard.getTrackedPlayers();
            
            return trackedPlayers.stream()
                    .map(ScoreHolder::getScoreboardName)
                    .collect(Collectors.joining(separator));
        }
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("separator");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.scoreboard.tracked_players");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.scoreboard.tracked_players.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.scoreboard");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("separator", ", ");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }
}
