package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnEffectLostListener extends AbstractListener {

    @Nullable
    private String cachedEffectKey;
    @Nullable
    private String cachedEffectType;

    public OnEffectLostListener() {
        super("effect_lost");
    }

    public void onEffectLost(@Nullable String effectKey, @Nullable String effectType) {
        this.cachedEffectKey = effectKey;
        this.cachedEffectType = effectType;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("effect_key", () -> this.cachedEffectKey != null ? this.cachedEffectKey : "ERROR"));
        list.add(new CustomVariable("effect_type", () -> this.cachedEffectType != null ? this.cachedEffectType : "unknown"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_effect_lost");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_effect_lost.desc"));
    }
}
