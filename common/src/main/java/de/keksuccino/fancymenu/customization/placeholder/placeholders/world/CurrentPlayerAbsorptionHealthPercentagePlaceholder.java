package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

//TODO übernehmen
public class CurrentPlayerAbsorptionHealthPercentagePlaceholder extends AbstractWorldPercentagePlaceholder {

    public CurrentPlayerAbsorptionHealthPercentagePlaceholder() {
        super("current_player_absorption_health_percent");
    }

    @Override
    protected float getCurrentFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getAbsorptionAmount();
    }

    @Override
    protected float getMaxFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getAbsorptionAmount(); //TODO fix this !!!
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.current_player_absorption_health";
    }

}
