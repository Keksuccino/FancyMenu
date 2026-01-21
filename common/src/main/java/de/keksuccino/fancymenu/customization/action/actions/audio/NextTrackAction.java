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

public class NextTrackAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public NextTrackAction() {
        super("audio_next_track");
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
                        audio.goToNextAudio();
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to execute NextTrackAction!", ex);
        }
    }

    @Override
    public @NotNull Component getActionDisplayName() {
        return Component.translatable("fancymenu.actions.audio.next_track");
    }

    @Override
    public @NotNull Component[] getActionDescription() {
        return LocalizationUtils.splitLocalizedLines("fancymenu.actions.audio.next_track.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.audio.next_track.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "audio_element_identifier";
    }

}