package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ClearVariablesAction extends Action {

    public ClearVariablesAction() {
        super("clear_variables");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public void execute(String value) {
        VariableHandler.clearVariables();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.variables.clearall");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.variables.clearall.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return null;
    }

    @Override
    public String getValuePreset() {
        return null;
    }

}
