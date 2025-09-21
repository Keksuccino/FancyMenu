package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnDeathListener extends AbstractListener {

    @Nullable
    private String cachedDaysSurvived;
    @Nullable
    private String cachedDeathReasonString;
    @Nullable
    private String cachedDeathReasonComponent;
    @Nullable
    private String cachedDeathPosX;
    @Nullable
    private String cachedDeathPosY;
    @Nullable
    private String cachedDeathPosZ;

    public OnDeathListener() {
        super("player_death");
    }

    public void onDeath(@Nullable Component deathReason, @Nullable Long daysSurvived, @Nullable Double posX, @Nullable Double posY, @Nullable Double posZ) {
        this.cachedDeathReasonString = (deathReason != null) ? deathReason.getString() : null;
        this.cachedDeathReasonComponent = (deathReason != null) ? this.serializeComponent(deathReason) : null;
        this.cachedDaysSurvived = (daysSurvived != null && daysSurvived >= 0L) ? Long.toString(daysSurvived) : null;
        this.cachedDeathPosX = this.formatCoordinate(posX);
        this.cachedDeathPosY = this.formatCoordinate(posY);
        this.cachedDeathPosZ = this.formatCoordinate(posZ);
        this.notifyAllInstances();
    }

    @Nullable
    private String serializeComponent(@NotNull Component component) {
        RegistryAccess registryAccess = RegistryAccess.EMPTY;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            registryAccess = minecraft.level.registryAccess();
        } else if ((minecraft.getConnection() != null) && (minecraft.getConnection().registryAccess() != null)) {
            registryAccess = minecraft.getConnection().registryAccess();
        }
        return Component.Serializer.toJson(component, registryAccess);
    }

    @Nullable
    private String formatCoordinate(@Nullable Double coordinate) {
        if (coordinate == null || Double.isNaN(coordinate) || Double.isInfinite(coordinate)) {
            return null;
        }
        return Double.toString(coordinate);
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("days_survived", () -> this.cachedDaysSurvived != null ? this.cachedDaysSurvived : "0"));
        list.add(new CustomVariable("death_reason_string", () -> this.cachedDeathReasonString != null ? this.cachedDeathReasonString : "ERROR"));
        list.add(new CustomVariable("death_reason_component", () -> this.cachedDeathReasonComponent != null ? this.cachedDeathReasonComponent : "ERROR"));
        list.add(new CustomVariable("death_pos_x", () -> this.cachedDeathPosX != null ? this.cachedDeathPosX : "0"));
        list.add(new CustomVariable("death_pos_y", () -> this.cachedDeathPosY != null ? this.cachedDeathPosY : "0"));
        list.add(new CustomVariable("death_pos_z", () -> this.cachedDeathPosZ != null ? this.cachedDeathPosZ : "0"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_death");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_death.desc"));
    }
}