package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OnWeatherChangedListener extends AbstractListener {

    @Nullable
    private String cachedWeatherType;
    @Nullable
    private String cachedWeatherCanSnow;
    @Nullable
    private String cachedWeatherCanRain;

    public OnWeatherChangedListener() {
        super("weather_changed");
    }

    public void onWeatherChanged(@NotNull String weatherType, boolean canSnow, boolean canRain) {
        this.cachedWeatherType = weatherType;
        this.cachedWeatherCanSnow = Boolean.toString(canSnow);
        this.cachedWeatherCanRain = Boolean.toString(canRain);
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("weather_type", () -> this.cachedWeatherType != null ? this.cachedWeatherType : "clear"));
        list.add(new CustomVariable("weather_can_snow", () -> this.cachedWeatherCanSnow != null ? this.cachedWeatherCanSnow : "false"));
        list.add(new CustomVariable("weather_can_rain", () -> this.cachedWeatherCanRain != null ? this.cachedWeatherCanRain : "false"));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_weather_changed");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_weather_changed.desc"));
    }
}
