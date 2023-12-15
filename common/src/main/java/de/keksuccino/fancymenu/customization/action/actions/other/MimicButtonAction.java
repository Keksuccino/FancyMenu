package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.widget.WidgetLocatorHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MimicButtonAction extends Action {

    public MimicButtonAction() {
        super("mimicbutton");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {
        if (value != null) {
            if (value.contains(":")) {
                WidgetLocatorHandler.invokeWidgetOnClick(value);
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Components.translatable("fancymenu.editor.custombutton.config.actiontype.mimicbutton");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.mimicbutton.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Components.translatable("fancymenu.editor.custombutton.config.actiontype.mimicbutton.desc.value");
    }

    @Override
    public String getValueExample() {
        return "example.menu.identifier:505280";
    }

}
