package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class PlayerTeamPlaceholder extends Placeholder {

    public PlayerTeamPlaceholder() {
        super("player_team");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientLevel level = Minecraft.getInstance().level;
        String playerName = dps.values.get("player_name");
        if ((player != null) && (level != null) && (playerName != null)) {
            PlayerTeam team = level.getScoreboard().getPlayersTeam(playerName);
            if (team != null) return team.getName();
        }
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("player_name");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.player_team");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.player_team.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("player_name", "LadyAgnes");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
