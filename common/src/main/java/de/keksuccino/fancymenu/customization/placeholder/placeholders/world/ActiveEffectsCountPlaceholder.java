package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

//TODO übernehmen
public class ActiveEffectsCountPlaceholder extends AbstractWorldIntegerPlaceholder {

    public ActiveEffectsCountPlaceholder() {
        super("effects_count");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return player.getActiveEffects().size();
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.effects_count";
    }

}
