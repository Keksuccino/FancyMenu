package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.NotNull;

public class PlayerArmorToughnessPlaceholder extends AbstractWorldFloatPlaceholder {

    public PlayerArmorToughnessPlaceholder() {
        super("player_armor_toughness");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        return (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.player_armor_toughness";
    }
}

