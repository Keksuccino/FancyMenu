package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnItemUsedListener extends AbstractListener {

    @Nullable
    private String cachedItemKey;
    @NotNull
    private String cachedUsedOnType = "none";
    @NotNull
    private String cachedUsedOnEntityKey = "";
    @NotNull
    private String cachedUsedOnBlockKey = "";
    @NotNull
    private String cachedTargetPosX = "-1";
    @NotNull
    private String cachedTargetPosY = "-1";
    @NotNull
    private String cachedTargetPosZ = "-1";

    public OnItemUsedListener() {
        super("item_used");
    }

    public void onItemUsed(@Nullable String itemKey, @NotNull String usedOnType,
                           @Nullable String entityKey, @Nullable String blockKey,
                           @NotNull String targetPosX, @NotNull String targetPosY, @NotNull String targetPosZ) {
        this.cachedItemKey = (itemKey != null && !itemKey.isBlank()) ? itemKey : null;
        this.cachedUsedOnType = this.normalizeUsedOnType_FancyMenu(usedOnType);
        this.cachedUsedOnEntityKey = (entityKey != null) ? entityKey : "";
        this.cachedUsedOnBlockKey = (blockKey != null) ? blockKey : "";
        this.cachedTargetPosX = targetPosX;
        this.cachedTargetPosY = targetPosY;
        this.cachedTargetPosZ = targetPosZ;
        this.notifyAllInstances();
    }

    @NotNull
    private String normalizeUsedOnType_FancyMenu(@NotNull String usedOnType) {
        return switch (usedOnType) {
            case "entity", "block", "self" -> usedOnType;
            default -> "none";
        };
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("item_key", () -> this.cachedItemKey != null ? this.cachedItemKey : "ERROR"));
        list.add(new CustomVariable("used_on_type", () -> this.cachedUsedOnType));
        list.add(new CustomVariable("used_on_entity_key", () -> this.cachedUsedOnEntityKey));
        list.add(new CustomVariable("used_on_block_key", () -> this.cachedUsedOnBlockKey));
        list.add(new CustomVariable("target_pos_x", () -> this.cachedTargetPosX));
        list.add(new CustomVariable("target_pos_y", () -> this.cachedTargetPosY));
        list.add(new CustomVariable("target_pos_z", () -> this.cachedTargetPosZ));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_item_used");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_item_used.desc"));
    }
}

