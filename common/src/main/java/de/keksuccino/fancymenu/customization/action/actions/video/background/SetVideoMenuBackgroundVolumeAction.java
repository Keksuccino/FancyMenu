package de.keksuccino.fancymenu.customization.action.actions.video.background;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SetVideoMenuBackgroundVolumeAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public SetVideoMenuBackgroundVolumeAction() {
        super("set_video_menu_background_volume");
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
                String id = value.split(":", 2)[0];
                String volString = value.split(":", 2)[1];
                if (MathUtils.isFloat(volString)) {
                    float volume = Math.min(1.0F, Math.max(0.0F, Float.parseFloat(volString)));
                    VideoElementController.VideoElementMeta meta = VideoElementController.getMeta(id);
                    if (meta == null) {
                        meta = new VideoElementController.VideoElementMeta(id, 1.0F, false);
                        VideoElementController.putMeta(id, meta);
                    }
                    //Only update volume if needed, to not constantly write to the metas file
                    if (meta.volume != volume) {
                        meta.volume = volume;
                        VideoElementController.putMeta(id, meta);
                    }
                }
            }

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to execute SetVideoMenuBackgroundVolumeAction!", ex);
        }

    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.video.set_video_background_volume");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.video.set_video_background_volume.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.video.set_video_background_volume.value.desc");
    }

    @Override
    public String getValueExample() {
        return "background_identifier:1.0";
    }

}
