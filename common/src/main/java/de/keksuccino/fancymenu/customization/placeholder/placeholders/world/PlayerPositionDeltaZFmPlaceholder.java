package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.util.player.PlayerPositionObserver;
import org.jetbrains.annotations.NotNull;

public class PlayerPositionDeltaZFmPlaceholder extends AbstractWorldPlaceholder {

    public PlayerPositionDeltaZFmPlaceholder() {
        super("player_position_delta_z_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return String.valueOf(PlayerPositionObserver.getCurrentPositionDeltaZ());
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.player_position_delta_z_fm";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
