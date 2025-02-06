package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public class CurrentPlayerLevelPlaceholder extends AbstractWorldIntegerPlaceholder {

    public CurrentPlayerLevelPlaceholder() {
        super("current_player_level");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.experienceLevel;
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.current_player_level";
    }

}
