package de.keksuccino.fancymenu.customization.action.actions.video.background;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SetVideoMenuBackgroundPlayTimeAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public SetVideoMenuBackgroundPlayTimeAction() {
        super("set_video_menu_background_play_time");
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

            if ((value != null) && value.contains(":")) {
                String id = value.split(":", 2)[0].trim();
                String timeMsString = value.split(":", 2)[1].trim();
                if (!id.isEmpty() && MathUtils.isLong(timeMsString)) {
                    long seekTimeMs = Math.max(0L, Long.parseLong(timeMsString));
                    VideoElementController.queueSeekTimeMs(id, seekTimeMs);
                }
            }

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to execute SetVideoMenuBackgroundPlayTimeAction!", ex);
        }

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.video.set_video_background_play_time");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.video.set_video_background_play_time.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.video.set_video_background_play_time.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "background_identifier:0";
    }

}
