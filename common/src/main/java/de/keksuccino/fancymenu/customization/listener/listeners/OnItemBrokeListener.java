package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnItemBrokeListener extends AbstractListener {

    @Nullable
    private String cachedItemKey;
    @Nullable
    private String cachedItemType;

    public OnItemBrokeListener() {
        super("item_broke");
    }

    public void onItemBroke(@Nullable String itemKey, @Nullable String itemType) {
        this.cachedItemKey = itemKey;
        this.cachedItemType = itemType;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("item_key", () -> this.cachedItemKey != null ? this.cachedItemKey : "ERROR"));
        list.add(new CustomVariable("item_type", () -> this.cachedItemType != null ? this.cachedItemType : "other"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_item_broke");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_item_broke.desc"));
    }
}
