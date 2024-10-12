package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

//TODO übernehmen
public class MaxPlayerHungerPlaceholder extends AbstractWorldFloatPlaceholder {

    public MaxPlayerHungerPlaceholder() {
        super("max_player_hunger");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return 20; //20 is the hardcoded max food level for players
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.max_player_hunger";
    }

}
