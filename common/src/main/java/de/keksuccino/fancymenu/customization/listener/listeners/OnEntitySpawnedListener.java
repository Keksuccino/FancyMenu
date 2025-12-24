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

public class OnEntitySpawnedListener extends AbstractListener {

    @Nullable
    private String cachedEntityKey;
    @Nullable
    private String cachedDistanceToPlayer;
    @Nullable
    private String cachedEntityPosX;
    @Nullable
    private String cachedEntityPosY;
    @Nullable
    private String cachedEntityPosZ;
    @Nullable
    private String cachedEntityUuid;
    @Nullable
    private String cachedDimensionKey;
    private boolean isSameDimensionAsPlayer;

    public OnEntitySpawnedListener() {
        super("entity_spawned");
    }

    public void onEntitySpawned(@Nullable String entityKey, @Nullable UUID entityUuid, double posX, double posY, double posZ, @Nullable String levelKey) {
        this.cachedEntityKey = entityKey;
        this.cachedEntityUuid = (entityUuid != null) ? entityUuid.toString() : null;
        this.cachedEntityPosX = Double.toString(posX);
        this.cachedEntityPosY = Double.toString(posY);
        this.cachedEntityPosZ = Double.toString(posZ);
        this.cachedDimensionKey = levelKey;
        this.cachedDistanceToPlayer = this.computeDistanceToPlayer(posX, posY, posZ, levelKey);
        this.notifyAllInstances();
    }

    @Nullable
    private String computeDistanceToPlayer(double posX, double posY, double posZ, @Nullable String levelKey) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            this.isSameDimensionAsPlayer = false;
            return null;
        }
        String playerLevelKey = minecraft.player.level() != null ? minecraft.player.level().dimension().identifier().toString() : null;
        boolean sameDimension = (levelKey != null && playerLevelKey != null && playerLevelKey.equals(levelKey)) || (levelKey == null && playerLevelKey == null);
        this.isSameDimensionAsPlayer = sameDimension;
        if (!sameDimension) {
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
        list.add(new CustomVariable("entity_pos_x", () -> this.cachedEntityPosX != null ? this.cachedEntityPosX : "0"));
        list.add(new CustomVariable("entity_pos_y", () -> this.cachedEntityPosY != null ? this.cachedEntityPosY : "0"));
        list.add(new CustomVariable("entity_pos_z", () -> this.cachedEntityPosZ != null ? this.cachedEntityPosZ : "0"));
        list.add(new CustomVariable("entity_uuid", () -> this.cachedEntityUuid != null ? this.cachedEntityUuid : "ERROR"));
        list.add(new CustomVariable("dimension_key", () -> this.cachedDimensionKey != null ? this.cachedDimensionKey : "UNKNOWN"));
        list.add(new CustomVariable("is_same_dimension_as_player", () -> Boolean.toString(this.isSameDimensionAsPlayer)));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_entity_spawned");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_entity_spawned.desc"));
    }
}
