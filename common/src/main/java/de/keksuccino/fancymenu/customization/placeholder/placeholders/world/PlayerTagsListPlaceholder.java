package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerTagsListPlaceholder extends Placeholder {

    public PlayerTagsListPlaceholder() {
        super("player_tags_list");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        ClientLevel level = Minecraft.getInstance().level;
        String playerName = dps.values.get("player_name");
        String separator = dps.values.get("separator");
        if (separator == null) separator = ", ";
        
        if ((level != null) && (playerName != null)) {
            // Search for player by name
            for (AbstractClientPlayer player : level.players()) {
                if (player.getName().getString().equals(playerName)) {
                    // Get all tags and join them with the separator
                    return player.getTags().stream()
                            .sorted()
                            .collect(Collectors.joining(separator));
                }
            }
        }
        return "";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("player_name", "separator");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.player_tags_list");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.player_tags_list.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("player_name", "Steve");
        values.put("separator", ", ");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
