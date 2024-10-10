package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public class PlayerYCoordinatePlaceholder extends AbstractWorldIntegerPlaceholder {

    public PlayerYCoordinatePlaceholder() {
        super("player_y_coordinate");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.blockPosition().getY();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.player_y_coordinate";
    }

}
