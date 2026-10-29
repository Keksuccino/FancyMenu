package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OnLeaveActiveRaidAreaListener extends AbstractListener {

    public OnLeaveActiveRaidAreaListener() {
        super("leave_active_raid_area");
    }

    public void onActiveRaidAreaLeft() {
        if (!this.hasInstancesListening()) {
            return;
        }
        this.notifyAllInstances();
    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_leave_active_raid_area");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_leave_active_raid_area.desc"));
    }

}
