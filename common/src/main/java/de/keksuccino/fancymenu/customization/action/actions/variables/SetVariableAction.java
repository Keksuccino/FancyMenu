package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class SetVariableAction extends Action {

    public SetVariableAction() {
        super("set_variable");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(String value) {
        if (value != null) {
            if (value.contains(":")) {
                String name = value.split(":", 2)[0];
                String val = value.split(":", 2)[1];
                VariableHandler.setVariable(name, val);
            }
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.variables.set");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.variables.set.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.variables.set.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "cool_variable_name:some_value";
    }

}
