package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnDimensionEnteredListener extends AbstractListener {

    @Nullable
    private String cachedDimensionKey;

    public OnDimensionEnteredListener() {
        super("enter_dimension");
    }

    public void onDimensionEntered(@NotNull ResourceKey<Level> dimensionKey) {
        this.cachedDimensionKey = dimensionKey.identifier().toString();
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("dimension_key", () -> this.cachedDimensionKey != null ? this.cachedDimensionKey : "ERROR"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_dimension_entered");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_dimension_entered.desc"));
    }
}
