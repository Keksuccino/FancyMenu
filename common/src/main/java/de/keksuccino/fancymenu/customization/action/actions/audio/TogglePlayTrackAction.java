package de.keksuccino.fancymenu.customization.action.actions.audio;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.audio.AudioElement;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TogglePlayTrackAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public TogglePlayTrackAction() {
        super("audio_toggle_play");
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
                ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
                if (layer != null) {
                    AbstractElement element = layer.getElementByInstanceIdentifier(value);
                    if (element instanceof AudioElement audio) {
                        if (audio.currentAudio != null) {
                            if (audio.currentAudio.isPlaying()) {
                                audio.currentAudio.pause();
                            } else {
                                if (audio.currentAudio.isPaused()) {
                                    audio.currentAudio.play();
                                } else {
                                    audio.currentAudio.play();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to execute TogglePlayAudioAction!", ex);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.audio.toggle_play");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.audio.toggle_play.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.audio.toggle_play.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "audio_element_identifier";
    }

}