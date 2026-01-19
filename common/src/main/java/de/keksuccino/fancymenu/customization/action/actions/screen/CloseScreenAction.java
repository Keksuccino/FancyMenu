package de.keksuccino.fancymenu.customization.action.actions.screen;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.ScreenUtils;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CloseScreenAction extends Action {

    public CloseScreenAction() {
        super("closegui");
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
    public void execute(@Nullable String value) {
        ScreenUtils.setScreen(null);
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.close_screen");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.close_screen.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return null;
    }

    @Override
    public String getValueExample() {
        return null;
    }

}
