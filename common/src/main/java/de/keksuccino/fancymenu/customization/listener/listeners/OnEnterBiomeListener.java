package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class OnEnterBiomeListener extends AbstractListener {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    private ResourceKey<Biome> lastBiomeKey;
    @Nullable
    private String cachedBiomeKey;

    public OnEnterBiomeListener() {

        super("enter_biome");

    }

    public void onBiomeChanged(@Nullable ResourceKey<Biome> biomeKey) {

        if (Objects.equals(this.lastBiomeKey, biomeKey)) {
            return;
        }

        this.lastBiomeKey = biomeKey;
        this.cachedBiomeKey = (biomeKey != null) ? biomeKey.location().toString() : null;

        if (biomeKey != null) {
            this.notifyAllInstances();
        }

    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {

        list.add(new CustomVariable("biome_key", () -> {
            if (this.cachedBiomeKey == null) return "ERROR";
            return this.cachedBiomeKey;
        }));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_enter_biome");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_enter_biome.desc"));
    }

}
