package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnEffectGainedListener extends AbstractListener {

    @Nullable
    private String cachedEffectKey;
    @Nullable
    private String cachedEffectType;
    @Nullable
    private String cachedEffectDuration;

    public OnEffectGainedListener() {
        super("effect_gained");
    }

    public void onEffectGained(@Nullable String effectKey, @Nullable String effectType, int effectDurationTicks) {
        this.cachedEffectKey = effectKey;
        this.cachedEffectType = effectType;
        this.cachedEffectDuration = Integer.toString(Math.max(effectDurationTicks, 0));
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("effect_key", () -> this.cachedEffectKey != null ? this.cachedEffectKey : "ERROR"));
        list.add(new CustomVariable("effect_type", () -> this.cachedEffectType != null ? this.cachedEffectType : "unknown"));
        list.add(new CustomVariable("effect_duration", () -> this.cachedEffectDuration != null ? this.cachedEffectDuration : "0"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_effect_gained");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_effect_gained.desc"));
    }
}
