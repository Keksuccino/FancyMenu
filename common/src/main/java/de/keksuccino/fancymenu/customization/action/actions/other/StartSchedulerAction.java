package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.scheduler.SchedulerHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StartSchedulerAction extends Action {

    public StartSchedulerAction() {
        super("start_scheduler");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value == null) return;
        String identifier = value.trim();
        if (!identifier.isBlank()) {
            SchedulerHandler.startScheduler(identifier);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.start_scheduler");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.start_scheduler.desc");
    }

    @Override
    public @Nullable Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.start_scheduler.value");
    }

    @Override
    public @Nullable String getValuePreset() {
        return "my_scheduler";
    }

}
