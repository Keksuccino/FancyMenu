package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OnEntityStartsBeingInSightListener extends AbstractListener {

    private final OnEntityStopsBeingInSightListener stopListener;
    private final Map<UUID, EntitySightData> trackedEntities = new HashMap<>();
    private final Set<UUID> seenThisFrame = new HashSet<>();

    @Nullable
    private EntitySightData cachedEntityData;

    public OnEntityStartsBeingInSightListener(@NotNull OnEntityStopsBeingInSightListener stopListener) {
        super("entity_starts_being_in_sight");
        this.stopListener = stopListener;
    }

    public void onRenderFrameStart() {
        this.seenThisFrame.clear();
    }

    public void onEntityVisible(@NotNull Entity entity, double distanceToPlayer) {
        UUID entityUuid = entity.getUUID();
        EntitySightData sightData = EntitySightData.from(entity, distanceToPlayer);
        this.seenThisFrame.add(entityUuid);

        EntitySightData previousData = this.trackedEntities.put(entityUuid, sightData);
        if (previousData != null) {
            return;
        }

        this.cachedEntityData = sightData;
        this.notifyAllInstances();
    }

    public void onRenderFrameEnd() {
        if (this.trackedEntities.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, EntitySightData>> iterator = this.trackedEntities.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, EntitySightData> entry = iterator.next();
            if (!this.seenThisFrame.contains(entry.getKey())) {
                iterator.remove();
                this.stopListener.onEntityStopped(entry.getValue());
            }
        }
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("entity_key", () -> {
            EntitySightData data = this.cachedEntityData;
            if (data == null || data.entityKey() == null) {
                return "ERROR";
            }
            return data.entityKey();
        }));
        list.add(new CustomVariable("distance_to_player", () -> {
            EntitySightData data = this.cachedEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.distanceToPlayer());
        }));
        list.add(new CustomVariable("entity_pos_x", () -> {
            EntitySightData data = this.cachedEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosX());
        }));
        list.add(new CustomVariable("entity_pos_y", () -> {
            EntitySightData data = this.cachedEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosY());
        }));
        list.add(new CustomVariable("entity_pos_z", () -> {
            EntitySightData data = this.cachedEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosZ());
        }));
        list.add(new CustomVariable("entity_uuid", () -> {
            EntitySightData data = this.cachedEntityData;
            if (data == null) {
                return "ERROR";
            }
            return data.entityUuid().toString();
        }));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_entity_starts_being_in_sight");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_entity_starts_being_in_sight.desc"));
    }

    public record EntitySightData(@Nullable String entityKey,
                                  double distanceToPlayer,
                                  double entityPosX,
                                  double entityPosY,
                                  double entityPosZ,
                                  @NotNull UUID entityUuid) {

        public static @NotNull EntitySightData from(@NotNull Entity entity, double distanceToPlayer) {
            Identifier entityTypeKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String keyString = (entityTypeKey != null) ? entityTypeKey.toString() : null;
            return new EntitySightData(
                keyString,
                distanceToPlayer,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                entity.getUUID()
            );
        }
    }
}
