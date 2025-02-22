package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public class MaxPlayerOxygenPlaceholder extends AbstractWorldFloatPlaceholder {

    public MaxPlayerOxygenPlaceholder() {
        super("max_player_oxygen");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getMaxAirSupply();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.max_player_oxygen";
    }

}
