package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

public class PlayerAttackStrengthPercentagePlaceholder extends AbstractWorldIntegerPlaceholder {

    public PlayerAttackStrengthPercentagePlaceholder() {
        super("player_attack_strength");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return (int)(player.getAttackStrengthScale(0.0F) * 100.0F);
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.player_attack_strength";
    }

}
