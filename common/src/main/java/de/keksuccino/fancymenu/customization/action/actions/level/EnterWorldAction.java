package de.keksuccino.fancymenu.customization.action.actions.level;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnterWorldAction extends Action {

    public EnterWorldAction() {
        super("loadworld");
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
            if (Minecraft.getInstance().getLevelSource().levelExists(value)) {
                Screen current = (Minecraft.getInstance().screen != null) ? Minecraft.getInstance().screen : new TitleScreen();
                Minecraft.getInstance().forceSetScreen(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
                Minecraft.getInstance().createWorldOpenFlows().openWorld(value, () -> {
                    Minecraft.getInstance().setScreen(current);
                });
            }
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.loadworld");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.loadworld.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.loadworld.desc.value");
    }

    @Override
    public String getValueExample() {
        return "exampleworld";
    }

}
