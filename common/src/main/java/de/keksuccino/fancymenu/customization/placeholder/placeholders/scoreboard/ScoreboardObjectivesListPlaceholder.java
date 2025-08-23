package de.keksuccino.fancymenu.customization.placeholder.placeholders.scoreboard;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class ScoreboardObjectivesListPlaceholder extends Placeholder {

    public ScoreboardObjectivesListPlaceholder() {
        super("scoreboard_objectives_list");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {

        ClientPacketListener connection = Minecraft.getInstance().getConnection();

        ClientLevel level = (connection != null) ? connection.getLevel() : null;
        Scoreboard scoreboard = (level != null) ? level.getScoreboard(): null;
        String separator = dps.values.get("separator");
        if (separator == null) separator = ", ";

        if (scoreboard != null) {
            Collection<Objective> objectives = scoreboard.getObjectives();
            return objectives.stream()
                    .map(Objective::getName)
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
        return I18n.get("fancymenu.placeholders.scoreboard.objectives_list");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.scoreboard.objectives_list.desc"));
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
