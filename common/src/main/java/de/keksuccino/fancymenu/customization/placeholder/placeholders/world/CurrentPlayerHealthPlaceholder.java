package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

//TODO übernehmen
public class CurrentPlayerHealthPlaceholder extends AbstractWorldFloatPlaceholder {

    public CurrentPlayerHealthPlaceholder() {
        super("current_player_health");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getHealth();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.current_player_health";
    }

}
