package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnEntityStartsBeingInSightListener.EntitySightData;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnEntityStopsBeingInSightListener extends AbstractListener {

    @Nullable
    private EntitySightData lastEntityData;

    public OnEntityStopsBeingInSightListener() {
        super("entity_stops_being_in_sight");
    }

    public void onEntityStopped(@Nullable EntitySightData data) {
        this.lastEntityData = data;
        if (data != null) {
            this.notifyAllInstances();
        }
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("entity_key", () -> {
            EntitySightData data = this.lastEntityData;
            if (data == null || data.entityKey() == null) {
                return "ERROR";
            }
            return data.entityKey();
        }));
        list.add(new CustomVariable("distance_to_player", () -> {
            EntitySightData data = this.lastEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.distanceToPlayer());
        }));
        list.add(new CustomVariable("entity_pos_x", () -> {
            EntitySightData data = this.lastEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosX());
        }));
        list.add(new CustomVariable("entity_pos_y", () -> {
            EntitySightData data = this.lastEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosY());
        }));
        list.add(new CustomVariable("entity_pos_z", () -> {
            EntitySightData data = this.lastEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosZ());
        }));
        list.add(new CustomVariable("entity_uuid", () -> {
            EntitySightData data = this.lastEntityData;
            if (data == null) {
                return "ERROR";
            }
            return data.entityUuid().toString();
        }));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_entity_stops_being_in_sight");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_entity_stops_being_in_sight.desc"));
    }
}
