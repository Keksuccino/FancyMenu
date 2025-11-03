package de.keksuccino.fancymenu.customization.listener.listeners;

import de.keksuccino.fancymenu.customization.listener.AbstractListener;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class OnVariableUpdatedListener extends AbstractListener {

    private static final Logger LOGGER = LogManager.getLogger();

    protected String varName;
    protected String oldValue;
    protected String newValue;

    public OnVariableUpdatedListener() {

        super("fm_variable_updated");

    }

    public void onVariableUpdated(@NotNull String varName, @NotNull String oldValue, @NotNull String newValue) {

        // Update cache before notifying instances, so they can use the up-to-date char
        this.varName = varName;
        this.oldValue = oldValue;
        this.newValue = newValue;

        this.notifyAllInstances();

    }

    @Override
    protected void buildCustomVariablesAndAddToList(List<CustomVariable> list) {

        // $$var_name
        list.add(new CustomVariable("var_name", () -> {
            if (this.varName == null) return "ERROR";
            return this.varName;
        }));

        // $$old_value
        list.add(new CustomVariable("old_value", () -> {
            if (this.oldValue == null) return "0";
            return this.oldValue;
        }));

        // $$new_value
        list.add(new CustomVariable("new_value", () -> {
            if (this.newValue == null) return "0";
            return this.newValue;
        }));

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.listeners.on_variable_updated");
    }

    @Override
    public @NotNull List<Component> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedLines("fancymenu.listeners.on_variable_updated.desc"));
    }

}
