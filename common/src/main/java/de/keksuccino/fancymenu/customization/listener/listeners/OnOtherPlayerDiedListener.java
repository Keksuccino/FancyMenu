package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class OnOtherPlayerDiedListener extends AbstractListener {

    @Nullable
    private String lastPlayerName;
    @Nullable
    private UUID lastPlayerUuid;
    @Nullable
    private String lastDeathPosX;
    @Nullable
    private String lastDeathPosY;
    @Nullable
    private String lastDeathPosZ;

    public OnOtherPlayerDiedListener() {
        super("other_player_died");
    }

    public void onOtherPlayerDied(@Nullable String playerName, @NotNull UUID playerUuid, @Nullable Vec3 deathPosition) {
        this.lastPlayerName = playerName;
        this.lastPlayerUuid = playerUuid;
        this.lastDeathPosX = this.formatCoordinate(deathPosition != null ? deathPosition.x : null);
        this.lastDeathPosY = this.formatCoordinate(deathPosition != null ? deathPosition.y : null);
        this.lastDeathPosZ = this.formatCoordinate(deathPosition != null ? deathPosition.z : null);
        this.notifyAllInstances();
    }

    @Nullable
    private String formatCoordinate(@Nullable Double coordinate) {
        if (coordinate == null || Double.isNaN(coordinate) || Double.isInfinite(coordinate)) {
            return null;
        }
        return Double.toString(coordinate);
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("player_name", () -> (this.lastPlayerName != null && !this.lastPlayerName.isBlank()) ? this.lastPlayerName : "ERROR"));
        list.add(new CustomVariable("player_uuid", () -> this.lastPlayerUuid != null ? this.lastPlayerUuid.toString() : "ERROR"));
        list.add(new CustomVariable("death_pos_x", () -> this.lastDeathPosX != null ? this.lastDeathPosX : "0"));
        list.add(new CustomVariable("death_pos_y", () -> this.lastDeathPosY != null ? this.lastDeathPosY : "0"));
        list.add(new CustomVariable("death_pos_z", () -> this.lastDeathPosZ != null ? this.lastDeathPosZ : "0"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_other_player_died");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_other_player_died.desc"));
    }
}