package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public class CurrentPlayerAbsorptionHealthPlaceholder extends AbstractWorldFloatPlaceholder {

    public CurrentPlayerAbsorptionHealthPlaceholder() {
        super("current_player_absorption_health");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getAbsorptionAmount();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.current_player_absorption_health";
    }

}
