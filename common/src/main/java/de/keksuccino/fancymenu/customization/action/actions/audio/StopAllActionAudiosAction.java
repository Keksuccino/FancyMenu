package de.keksuccino.fancymenu.customization.action.actions.audio;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.util.threading.MainThreadTaskExecutor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StopAllActionAudiosAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public StopAllActionAudiosAction() {
        super("stop_all_action_audios");
    }

    @Override
    public boolean canRunAsync() {
        return true;
    }

    @Override
    public boolean hasValue() {
        return false;
    }

    @Override
    public void execute(@Nullable String value) {
        if (!Minecraft.getInstance().isSameThread()) {
            MainThreadTaskExecutor.executeInMainThread(() -> this.execute(null), MainThreadTaskExecutor.ExecuteTiming.PRE_CLIENT_TICK);
            return;
        }
        try {
            PlayAudioAction.stopAllTrackedActionAudios();
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] StopAllActionAudiosAction: Failed to stop tracked action audios!", ex);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.stop_all_action_audios");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.stop_all_action_audios.desc");
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
