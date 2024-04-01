package de.keksuccino.fancymenu.customization.action.actions.other;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.widget.WidgetLocatorHandler;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.NotificationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
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
    public boolean hasValue() {
        return true;
    }

    //TODO übernehmen
    @Override
    public void execute(@Nullable String value) {
        if (value != null) {
            if (value.contains(":")) {
                if (!WidgetLocatorHandler.invokeWidgetOnClick(value)) {
                    LOGGER.error("[FANCYMENU] Failed to mimic button '" + value + "'!", new Exception());
                    Screen current = Minecraft.getInstance().screen;
                    Minecraft.getInstance().setScreen(NotificationScreen.error(aBoolean -> {
                        Minecraft.getInstance().setScreen(current);
                    }, LocalizationUtils.splitLocalizedLines("fancymenu.actions.mimic_button.error")));
                }
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.mimicbutton");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.editor.custombutton.config.actiontype.mimicbutton.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.editor.custombutton.config.actiontype.mimicbutton.desc.value");
    }

    @Override
    public String getValueExample() {
        return "example.menu.identifier:505280";
    }

}
