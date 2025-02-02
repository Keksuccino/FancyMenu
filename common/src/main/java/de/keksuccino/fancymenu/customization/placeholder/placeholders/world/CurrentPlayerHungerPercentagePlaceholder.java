package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public class CurrentPlayerHungerPercentagePlaceholder extends AbstractWorldPercentagePlaceholder {

    public CurrentPlayerHungerPercentagePlaceholder() {
        super("current_player_hunger_percent");
    }

    @Override
    protected float getCurrentFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getFoodData().getFoodLevel();
    }

    @Override
    protected float getMaxFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return 20; //20 is the hardcoded max food level for players
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.current_player_hunger_percent";
    }

}
