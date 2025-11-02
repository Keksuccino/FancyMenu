package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class OnStartTouchingFluidListener extends AbstractListener {

    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    private String cachedFluidKey;

    public OnStartTouchingFluidListener() {
        super("start_touching_fluid");
    }

    public void onStartTouchingFluid(@Nullable String fluidKey) {
        this.cachedFluidKey = fluidKey;
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
        list.add(new CustomVariable("fluid_type", () -> {
            if (this.cachedFluidKey == null) return "ERROR";
            return this.cachedFluidKey;
        }));
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_start_touching_fluid");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_start_touching_fluid.desc"));
    }

}
