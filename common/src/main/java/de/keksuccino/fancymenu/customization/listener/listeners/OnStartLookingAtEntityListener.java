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
import java.util.UUID;

public class OnStartLookingAtEntityListener extends AbstractListener {

    @Nullable
    private LookedEntityData currentEntityData;

    public OnStartLookingAtEntityListener() {
        super("start_looking_at_entity");
    }

    /**
     * @return {@code true} when a new entity was detected and the listener notified its instances.
     */
    public boolean onLookAtEntity(@NotNull Entity entity, double distanceToPlayer) {
        LookedEntityData newData = LookedEntityData.from(entity, distanceToPlayer);
        LookedEntityData existingData = this.currentEntityData;

        if ((existingData != null) && existingData.uuid().equals(newData.uuid())) {
            this.currentEntityData = newData;
            return false;
        }

        this.currentEntityData = newData;
        this.notifyAllInstances();
        return true;
    }

    public void clearCurrentEntity() {
        this.currentEntityData = null;
    }

    @Nullable
    public LookedEntityData getCurrentEntityData() {
        return this.currentEntityData;
    }

    @Nullable
    public UUID getCurrentEntityUuid() {
        return (this.currentEntityData != null) ? this.currentEntityData.uuid() : null;
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("entity_key", () -> {
            LookedEntityData data = this.currentEntityData;
            if (data == null || data.entityKey() == null) {
                return "ERROR";
            }
            return data.entityKey();
        }));
        list.add(new CustomVariable("distance_to_player", () -> {
            LookedEntityData data = this.currentEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.distanceToPlayer());
        }));
        list.add(new CustomVariable("entity_pos_x", () -> {
            LookedEntityData data = this.currentEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosX());
        }));
        list.add(new CustomVariable("entity_pos_y", () -> {
            LookedEntityData data = this.currentEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosY());
        }));
        list.add(new CustomVariable("entity_pos_z", () -> {
            LookedEntityData data = this.currentEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosZ());
        }));
        list.add(new CustomVariable("entity_uuid", () -> {
            LookedEntityData data = this.currentEntityData;
            if (data == null) {
                return "ERROR";
            }
            return data.uuid().toString();
        }));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_start_looking_at_entity");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_start_looking_at_entity.desc"));
    }

    public record LookedEntityData(@NotNull UUID uuid,
                                   @Nullable String entityKey,
                                   double distanceToPlayer,
                                   double entityPosX,
                                   double entityPosY,
                                   double entityPosZ) {

        public static @NotNull LookedEntityData from(@NotNull Entity entity, double distanceToPlayer) {
            ResourceLocation entityKeyLocation = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String entityKey = (entityKeyLocation != null) ? entityKeyLocation.toString() : null;
            return new LookedEntityData(
                entity.getUUID(),
                entityKey,
                distanceToPlayer,
                entity.getX(),
                entity.getY(),
                entity.getZ()
            );
        }
    }
}
