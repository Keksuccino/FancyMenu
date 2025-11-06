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

public class PlayerHasTagPlaceholder extends Placeholder {

    public PlayerHasTagPlaceholder() {
        super("player_has_tag");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        ClientLevel level = Minecraft.getInstance().level;
        String playerName = dps.values.get("player_name");
        String tagName = dps.values.get("tag_name");
        
        if ((level != null) && (playerName != null) && (tagName != null)) {
            // Search for player by name
            for (AbstractClientPlayer player : level.players()) {
                if (player.getName().getString().equals(playerName)) {
                    // Check if player has the specified tag
                    return String.valueOf(player.getTags().contains(tagName));
                }
            }
        }
        return "false";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        return List.of("player_name", "tag_name");
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.player_has_tag");
    }

    @Override
    public @Nullable List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.player_has_tag.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.world");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        HashMap<String, String> values = new LinkedHashMap<>();
        values.put("player_name", "Steve");
        values.put("tag_name", "my_tag");
        return new DeserializedPlaceholderString(this.getIdentifier(), values, "");
    }

}
