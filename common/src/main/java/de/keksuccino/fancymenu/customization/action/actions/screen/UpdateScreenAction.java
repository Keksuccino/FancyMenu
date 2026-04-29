package de.keksuccino.fancymenu.customization.action.actions.screen;

import de.keksuccino.fancymenu.customization.ScreenCustomization;
import de.keksuccino.fancymenu.customization.action.Action;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UpdateScreenAction extends Action {

    public UpdateScreenAction() {
        super("update_screen");
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
        ScreenCustomization.reInitCurrentScreen();
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.update_screen");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.update_screen.desc");
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
