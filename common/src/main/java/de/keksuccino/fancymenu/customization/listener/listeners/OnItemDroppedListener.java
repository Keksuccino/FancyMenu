package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnItemDroppedListener extends AbstractListener {

    @Nullable
    private String cachedItemKey;

    public OnItemDroppedListener() {
        super("item_dropped");
    }

    public void onItemDropped(@Nullable String itemKey) {
        this.cachedItemKey = (itemKey != null && !itemKey.isBlank()) ? itemKey : null;
        if (this.cachedItemKey != null) {
            this.notifyAllInstances();
        }
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("item_key", () -> this.cachedItemKey != null ? this.cachedItemKey : "ERROR"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_item_dropped");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_item_dropped.desc"));
    }
}
