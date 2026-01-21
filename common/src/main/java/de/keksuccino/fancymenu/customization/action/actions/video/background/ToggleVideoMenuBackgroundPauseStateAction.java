package de.keksuccino.fancymenu.customization.action.actions.video.background;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToggleVideoMenuBackgroundPauseStateAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public ToggleVideoMenuBackgroundPauseStateAction() {
        super("toggle_video_menu_background_pause_state");
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
        try {
            if (value != null) {
                String id = value;
                VideoElementController.VideoElementMeta meta = VideoElementController.getMeta(id);
                if (meta == null) {
                    meta = new VideoElementController.VideoElementMeta(id, 1.0F, false);
                    VideoElementController.putMeta(id, meta);
                }
                meta.paused = !meta.paused;
                VideoElementController.putMeta(id, meta);
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to execute ToggleVideoMenuBackgroundPauseStateAction!", ex);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.video.background.toggle_paused_state");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.video.background.toggle_paused_state.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.video.background.toggle_paused_state.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "background_identifier";
    }

}
