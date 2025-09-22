package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class OnEntityDiedListener extends AbstractListener {

    @Nullable
    private String cachedEntityKey;
    @Nullable
    private String cachedDistanceToPlayer;
    @Nullable
    private String cachedDeathPosX;
    @Nullable
    private String cachedDeathPosY;
    @Nullable
    private String cachedDeathPosZ;
    @Nullable
    private String cachedEntityUuid;

    public OnEntityDiedListener() {
        super("entity_died");
    }

    public void onEntityDied(@Nullable String entityKey, @Nullable UUID entityUuid, double posX, double posY, double posZ, @Nullable String levelKey) {
        this.cachedEntityKey = entityKey;
        this.cachedEntityUuid = (entityUuid != null) ? entityUuid.toString() : null;
        this.cachedDeathPosX = Double.toString(posX);
        this.cachedDeathPosY = Double.toString(posY);
        this.cachedDeathPosZ = Double.toString(posZ);
        this.cachedDistanceToPlayer = this.computeDistanceToPlayer(posX, posY, posZ, levelKey);
        this.notifyAllInstances();
    }

    @Nullable
    private String computeDistanceToPlayer(double posX, double posY, double posZ, @Nullable String levelKey) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        String playerLevelKey = minecraft.player.level() != null ? minecraft.player.level().dimension().location().toString() : null;
        if (levelKey != null && playerLevelKey != null && !playerLevelKey.equals(levelKey)) {
            return "-1";
        }
        Vec3 playerPos = minecraft.player.position();
        double distance = playerPos.distanceTo(new Vec3(posX, posY, posZ));
        if (Double.isFinite(distance)) {
            return Double.toString(distance);
        }
        return null;
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("entity_key", () -> this.cachedEntityKey != null ? this.cachedEntityKey : "ERROR"));
        list.add(new CustomVariable("distance_to_player", () -> this.cachedDistanceToPlayer != null ? this.cachedDistanceToPlayer : "0"));
        list.add(new CustomVariable("death_pos_x", () -> this.cachedDeathPosX != null ? this.cachedDeathPosX : "0"));
        list.add(new CustomVariable("death_pos_y", () -> this.cachedDeathPosY != null ? this.cachedDeathPosY : "0"));
        list.add(new CustomVariable("death_pos_z", () -> this.cachedDeathPosZ != null ? this.cachedDeathPosZ : "0"));
        list.add(new CustomVariable("entity_uuid", () -> this.cachedEntityUuid != null ? this.cachedEntityUuid : "ERROR"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_entity_died");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_entity_died.desc"));
    }
}
