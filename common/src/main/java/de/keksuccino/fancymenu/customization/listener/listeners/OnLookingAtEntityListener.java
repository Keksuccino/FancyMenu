package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class OnLookingAtEntityListener extends AbstractListener {

    @Nullable
    private UUID lastEntityUuid;
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
    private String cachedEntityUuidString;

    public OnLookingAtEntityListener() {
        super("looking_at_entity");
    }

    public void onLookAtEntity(@NotNull Entity entity, double distanceToPlayer) {
        UUID entityUuid = entity.getUUID();
        if (Objects.equals(this.lastEntityUuid, entityUuid)) {
            return;
        }

        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        this.cachedEntityKey = (entityKey != null) ? entityKey.toString() : null;
        this.cachedDistanceToPlayer = Double.toString(distanceToPlayer);
        this.cachedEntityPosX = Double.toString(entity.getX());
        this.cachedEntityPosY = Double.toString(entity.getY());
        this.cachedEntityPosZ = Double.toString(entity.getZ());
        this.cachedEntityUuidString = entityUuid.toString();
        this.lastEntityUuid = entityUuid;

        this.notifyAllInstances();
    }

    public void onStopLooking() {
        this.lastEntityUuid = null;
        this.cachedEntityKey = null;
        this.cachedDistanceToPlayer = null;
        this.cachedEntityPosX = null;
        this.cachedEntityPosY = null;
        this.cachedEntityPosZ = null;
        this.cachedEntityUuidString = null;
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("entity_key", () -> (this.cachedEntityKey != null) ? this.cachedEntityKey : "ERROR"));
        list.add(new CustomVariable("distance_to_player", () -> (this.cachedDistanceToPlayer != null) ? this.cachedDistanceToPlayer : "0"));
        list.add(new CustomVariable("entity_pos_x", () -> (this.cachedEntityPosX != null) ? this.cachedEntityPosX : "0"));
        list.add(new CustomVariable("entity_pos_y", () -> (this.cachedEntityPosY != null) ? this.cachedEntityPosY : "0"));
        list.add(new CustomVariable("entity_pos_z", () -> (this.cachedEntityPosZ != null) ? this.cachedEntityPosZ : "0"));
        list.add(new CustomVariable("entity_uuid", () -> (this.cachedEntityUuidString != null) ? this.cachedEntityUuidString : "ERROR"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_looking_at_entity");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_looking_at_entity.desc"));
    }
}