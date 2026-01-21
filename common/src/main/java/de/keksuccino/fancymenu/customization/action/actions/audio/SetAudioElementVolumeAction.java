package de.keksuccino.fancymenu.customization.action.actions.audio;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.element.elements.audio.AudioElementController;
import de.keksuccino.konkrete.math.MathUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SetAudioElementVolumeAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public SetAudioElementVolumeAction() {
        super("set_audio_element_volume");
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
                    AudioElementController.AudioElementMeta meta = AudioElementController.getMeta(id);
                    if (meta == null) {
                        meta = new AudioElementController.AudioElementMeta(id, 1.0F);
                        AudioElementController.putMeta(id, meta);
                    }
                    //Only update volume if needed, to not constantly write to the metas file
                    if (meta.volume != volume) {
                        meta.volume = volume;
                        AudioElementController.putMeta(id, meta);
                    }
                }
            }

        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to execute SetAudioElementVolumeAction!", ex);
        }

    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.audio.set_audio_element_volume");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.audio.set_audio_element_volume.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.audio.set_audio_element_volume.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "element_identifier:1.0";
    }

}
