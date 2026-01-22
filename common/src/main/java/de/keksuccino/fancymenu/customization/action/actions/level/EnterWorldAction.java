package de.keksuccino.fancymenu.customization.action.actions.level;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.ScreenUtils;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.Dialogs;
import de.keksuccino.fancymenu.util.rendering.ui.dialog.message.MessageDialogStyle;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnterWorldAction extends Action {

    private static long lastJoinErrorTrigger = -1;

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
        if (Minecraft.getInstance().level != null) {
            long now = System.currentTimeMillis();
            if ((lastJoinErrorTrigger + 20000) < now) {
                lastJoinErrorTrigger = now;
                MainThreadTaskExecutor.executeInMainThread(() -> {
                    Dialogs.openMessage(Component.translatable("fancymenu.actions.errors.cannot_join_world_while_in_world"), MessageDialogStyle.ERROR);
                }, MainThreadTaskExecutor.ExecuteTiming.POST_CLIENT_TICK);
            }
            return;
        }
        if (value != null) {
            if (Minecraft.getInstance().getLevelSource().levelExists(value)) {
                Screen current = (Minecraft.getInstance().screen != null) ? Minecraft.getInstance().screen : new TitleScreen();
                Minecraft.getInstance().forceSetScreen(new GenericMessageScreen(Component.translatable("selectWorld.data_read")));
                Minecraft.getInstance().createWorldOpenFlows().openWorld(value, () -> {
                    ScreenUtils.setScreen(current);
                });
            }
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.loadworld");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.loadworld.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.loadworld.desc.value");
    }

    @Override
    public String getValuePreset() {
        return "exampleworld";
    }

}
