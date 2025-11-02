package de.keksuccino.fancymenu.customization.action.actions.level;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableNotificationScreen;
import de.keksuccino.fancymenu.util.rendering.ui.screen.queueable.QueueableScreenHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
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
                QueueableScreenHandler.addToQueue(new QueueableNotificationScreen(Component.translatable("fancymenu.actions.errors.cannot_join_world_while_in_world")));
            }
            return;
        }
        if (value != null) {
            if (Minecraft.getInstance().getLevelSource().levelExists(value) && (Minecraft.getInstance().screen != null)) {
                Minecraft.getInstance().forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));
                Minecraft.getInstance().createWorldOpenFlows().loadLevel(Minecraft.getInstance().screen, value);
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
