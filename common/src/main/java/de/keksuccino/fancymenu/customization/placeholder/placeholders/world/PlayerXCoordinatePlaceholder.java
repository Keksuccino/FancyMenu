package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public class PlayerXCoordinatePlaceholder extends AbstractWorldIntegerPlaceholder {

    public PlayerXCoordinatePlaceholder() {
        super("player_x_coordinate");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.blockPosition().getX();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.player_x_coordinate";
    }

}
