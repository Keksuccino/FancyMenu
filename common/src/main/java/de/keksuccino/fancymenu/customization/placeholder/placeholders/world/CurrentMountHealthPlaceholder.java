package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class CurrentMountHealthPlaceholder extends AbstractWorldFloatPlaceholder {

    public CurrentMountHealthPlaceholder() {
        super("current_mount_health");
    }

    @Override
    protected float getFloatValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        Entity mount = player.getVehicle();
        if (mount instanceof LivingEntity l) return l.getHealth();
        return 0.0F;
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.current_mount_health";
    }

}
