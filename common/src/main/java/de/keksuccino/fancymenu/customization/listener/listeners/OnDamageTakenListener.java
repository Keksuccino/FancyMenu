package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnDamageTakenListener extends AbstractListener {

    @Nullable
    private String cachedDamageAmount;
    @Nullable
    private String cachedDamageType;
    @Nullable
    private String cachedIsFatal;
    @Nullable
    private String cachedDamageSource;

    public OnDamageTakenListener() {
        super("damage_taken");
    }

    public void onDamageTaken(float damageAmount, @Nullable String damageType, boolean isFatal, @Nullable String damageSource) {
        this.cachedDamageAmount = Float.toString(Math.max(damageAmount, 0.0F));
        this.cachedDamageType = damageType;
        this.cachedIsFatal = Boolean.toString(isFatal);
        this.cachedDamageSource = (damageSource != null && !damageSource.isBlank()) ? damageSource : null;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("damage_amount", () -> this.cachedDamageAmount != null ? this.cachedDamageAmount : "0"));
        list.add(new CustomVariable("damage_type", () -> this.cachedDamageType != null ? this.cachedDamageType : "unknown"));
        list.add(new CustomVariable("is_fatal_damage", () -> this.cachedIsFatal != null ? this.cachedIsFatal : "false"));
        list.add(new CustomVariable("damage_source", () -> this.cachedDamageSource != null ? this.cachedDamageSource : "NONE"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_damage_taken");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_damage_taken.desc"));
    }
}

