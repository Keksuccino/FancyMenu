package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public class CurrentPlayerExpProgressPlaceholder extends AbstractWorldIntegerPlaceholder {

    public CurrentPlayerExpProgressPlaceholder() {
        super("current_player_exp_progress");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return (int)(player.experienceProgress * 100);
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.current_player_exp_progress";
    }

}
