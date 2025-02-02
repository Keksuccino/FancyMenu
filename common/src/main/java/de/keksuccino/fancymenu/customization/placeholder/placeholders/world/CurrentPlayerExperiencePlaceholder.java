package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public class CurrentPlayerExperiencePlaceholder extends AbstractWorldFloatPlaceholder {

    public CurrentPlayerExperiencePlaceholder() {
        super("current_player_exp");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.totalExperience;
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.current_player_experience";
    }

}
