package de.keksuccino.fancymenu.customization.action.actions.video;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToggleVideoElementPauseStateAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public ToggleVideoElementPauseStateAction() {
        super("toggle_video_element_pause_state");
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
            LOGGER.error("[FANCYMENU] Failed to execute ToggleVideoElementPauseStateAction!", ex);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.video.toggle_paused_state");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.video.toggle_paused_state.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.video.toggle_paused_state.value.desc");
    }

    @Override
    public String getValueExample() {
        return "element_identifier";
    }

}
