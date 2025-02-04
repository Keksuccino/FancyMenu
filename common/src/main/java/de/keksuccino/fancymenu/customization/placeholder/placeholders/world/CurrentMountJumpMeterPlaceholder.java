package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.PlayerRideableJumping;
import org.jetbrains.annotations.NotNull;

public class CurrentMountJumpMeterPlaceholder extends AbstractWorldIntegerPlaceholder {

    public CurrentMountJumpMeterPlaceholder() {
        super("current_mount_jump_meter");
    }

    @Override
    protected int getIntegerValue(@NotNull LocalPlayer player, @NotNull ClientLevel level) {
        PlayerRideableJumping mount = player.jumpableVehicle();
        if (mount != null) return (int)(player.getJumpRidingScale() * 100.0F);
        return 0;
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.current_mount_jump_meter";
    }

}
