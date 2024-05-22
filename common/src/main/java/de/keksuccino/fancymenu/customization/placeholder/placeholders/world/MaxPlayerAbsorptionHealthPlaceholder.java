package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

//TODO übernehmen
public class MaxPlayerAbsorptionHealthPlaceholder extends AbstractWorldFloatPlaceholder {

    public MaxPlayerAbsorptionHealthPlaceholder() {
        super("max_player_absorption_health");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getMaxAbsorption();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.max_player_absorption_health";
    }

}
