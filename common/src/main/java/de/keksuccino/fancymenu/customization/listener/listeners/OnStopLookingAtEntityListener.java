package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.customization.listener.listeners.OnStartLookingAtEntityListener.LookedEntityData;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnStopLookingAtEntityListener extends AbstractListener {

    @Nullable
    private LookedEntityData lastEntityData;

    public OnStopLookingAtEntityListener() {
        super("stop_looking_at_entity");
    }

    public void onStopLooking(@Nullable LookedEntityData data) {
        this.lastEntityData = data;
        if (data != null) {
            this.notifyAllInstances();
        }
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("entity_key", () -> {
            LookedEntityData data = this.lastEntityData;
            if (data == null || data.entityKey() == null) {
                return "ERROR";
            }
            return data.entityKey();
        }));
        list.add(new CustomVariable("distance_to_player", () -> {
            LookedEntityData data = this.lastEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.distanceToPlayer());
        }));
        list.add(new CustomVariable("entity_pos_x", () -> {
            LookedEntityData data = this.lastEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosX());
        }));
        list.add(new CustomVariable("entity_pos_y", () -> {
            LookedEntityData data = this.lastEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosY());
        }));
        list.add(new CustomVariable("entity_pos_z", () -> {
            LookedEntityData data = this.lastEntityData;
            if (data == null) {
                return "0";
            }
            return Double.toString(data.entityPosZ());
        }));
        list.add(new CustomVariable("entity_uuid", () -> {
            LookedEntityData data = this.lastEntityData;
            if (data == null) {
                return "ERROR";
            }
            return data.uuid().toString();
        }));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_stop_looking_at_entity");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_stop_looking_at_entity.desc"));
    }
}
