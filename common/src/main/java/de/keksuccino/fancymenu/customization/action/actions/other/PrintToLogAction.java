package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrintToLogAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public PrintToLogAction() {
        super("print_to_log");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value != null) {
            LOGGER.info(value);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.print_to_log");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.print_to_log.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.print_to_log.value");
    }

    @Override
    public String getValuePreset() {
        return "This text will show up in the log!";
    }

}
