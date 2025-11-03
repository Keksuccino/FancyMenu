package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnStartedFreezingListener extends AbstractListener {

    @Nullable
    private String cachedIntensity;

    public OnStartedFreezingListener() {
        super("started_freezing");
    }

    public void onStartedFreezing(float intensity) {
        this.cachedIntensity = Float.toString(Math.max(intensity, 0.0F));
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("freezing_intensity", () -> this.cachedIntensity != null ? this.cachedIntensity : "0"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_started_freezing");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_started_freezing.desc"));
    }
}
