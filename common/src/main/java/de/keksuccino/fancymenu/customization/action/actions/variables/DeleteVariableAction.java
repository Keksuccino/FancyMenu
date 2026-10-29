package de.keksuccino.fancymenu.customization.action.actions.variables;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.variables.VariableHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DeleteVariableAction extends Action {

    public DeleteVariableAction() {
        super("delete_variable");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value == null) return;
        String name = VariableHandler.normalizeVariableReferenceName(value);
        if (name.isEmpty()) return;
        VariableHandler.removeVariable(name);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.variables.delete");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.variables.delete.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.variables.delete.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "cool_variable_name";
    }

}
