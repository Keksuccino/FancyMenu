package de.keksuccino.fancymenu.customization.action.actions.layout;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.layout.Layout;
import de.keksuccino.fancymenu.customization.layout.LayoutHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnableLayoutAction extends Action {

    public EnableLayoutAction() {
        super("enable_layout");
    }

    @Override
    public boolean canRunAsync() {
        return false;
    }

    @Override
    public boolean hasValue() {
        return true;
    }

    @Override
    public void execute(@Nullable String value) {

        if (value != null) {

            Layout l = LayoutHandler.getLayout(value);
            if ((l != null) && !l.isEnabled()) {
                l.setEnabled(true, true);
            }

        }

    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.helper.buttonaction.enable_layout");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.helper.buttonaction.enable_layout.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.helper.buttonaction.enable_layout.value.desc");
    }

    @Override
    public String getValueExample() {
        return "my_cool_main_menu_layout";
    }

}
