package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.widget.WidgetLocatorHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MimicButtonAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public MimicButtonAction() {
        super("mimicbutton");
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
            if (value.contains(":")) {
                if (!WidgetLocatorHandler.invokeWidgetOnClick(value)) {
                    LOGGER.error("[FANCYMENU] Failed to mimic button '" + value + "'!", new Exception());
                    Dialogs.openMessage(Component.translatable("fancymenu.actions.mimic_button.error"), MessageDialogStyle.ERROR);
                }
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.mimicbutton");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.mimicbutton.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.mimicbutton.desc.value");
    }

    @Override
    public String getValuePreset() {
        return "example.menu.identifier:505280";
    }

}
