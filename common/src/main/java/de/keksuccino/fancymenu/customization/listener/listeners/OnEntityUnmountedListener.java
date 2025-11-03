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

public class OnEntityUnmountedListener extends AbstractListener {

    @Nullable
    private String cachedEntityKey;
    @Nullable
    private String cachedEntityPosX;
    @Nullable
    private String cachedEntityPosY;
    @Nullable
    private String cachedEntityPosZ;
    @Nullable
    private String cachedEntityUuid;

    public OnEntityUnmountedListener() {
        super("entity_unmounted");
    }

    public void onEntityUnmounted(@Nullable Entity entity) {
        if (entity != null) {
            this.cacheEntityData(entity);
        } else {
            this.cachedEntityKey = null;
            this.cachedEntityPosX = null;
            this.cachedEntityPosY = null;
            this.cachedEntityPosZ = null;
            this.cachedEntityUuid = null;
        }
        this.notifyAllInstances();
    }

    private void cacheEntityData(@NotNull Entity entity) {
        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        this.cachedEntityKey = (entityKey != null) ? entityKey.toString() : null;
        this.cachedEntityPosX = Double.toString(entity.getX());
        this.cachedEntityPosY = Double.toString(entity.getY());
        this.cachedEntityPosZ = Double.toString(entity.getZ());
        this.cachedEntityUuid = entity.getUUID().toString();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("entity_key", () -> this.cachedEntityKey != null ? this.cachedEntityKey : "ERROR"));
        list.add(new CustomVariable("entity_pos_x", () -> this.cachedEntityPosX != null ? this.cachedEntityPosX : "0"));
        list.add(new CustomVariable("entity_pos_y", () -> this.cachedEntityPosY != null ? this.cachedEntityPosY : "0"));
        list.add(new CustomVariable("entity_pos_z", () -> this.cachedEntityPosZ != null ? this.cachedEntityPosZ : "0"));
        list.add(new CustomVariable("entity_uuid", () -> this.cachedEntityUuid != null ? this.cachedEntityUuid : "ERROR"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_entity_unmounted");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_entity_unmounted.desc"));
    }
}
