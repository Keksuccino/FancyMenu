package de.keksuccino.fancymenu.customization.action.actions.layout;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.text.Components;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DisableLayoutAction extends Action {

    public DisableLayoutAction() {
        super("disable_layout");
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {

        if (value != null) {

            Layout l = LayoutHandler.getLayout(value);
            if ((l != null) && l.isEnabled()) {
                l.setEnabled(false, true);
            }

        }

    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Components.translatable("fancymenu.helper.buttonaction.disable_layout");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.helper.buttonaction.disable_layout.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Components.translatable("fancymenu.helper.buttonaction.disable_layout.value.desc");
    }

    @Override
    public String getValueExample() {
        return "my_cool_main_menu_layout";
    }

}
