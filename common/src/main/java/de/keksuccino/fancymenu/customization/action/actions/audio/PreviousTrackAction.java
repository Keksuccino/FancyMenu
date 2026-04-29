package de.keksuccino.fancymenu.customization.action.actions.audio;

import de.keksuccino.fancymenu.customization.action.Action;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.audio.AudioElement;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreviousTrackAction extends Action {

    private static final Logger LOGGER = LogManager.getLogger();

    public PreviousTrackAction() {
        super("audio_previous_track");
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
                        audio.goToPreviousAudio();
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[FANCYMENU] Failed to execute PreviousTrackAction!", ex);
        }
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.actions.audio.previous_track");
    }

    @Override
    public @NotNull Component getDescription() {
        return Component.translatable("fancymenu.actions.audio.previous_track.desc");
    }

    @Override
    public Component getValueDisplayName() {
        return Component.translatable("fancymenu.actions.audio.previous_track.value.desc");
    }

    @Override
    public String getValuePreset() {
        return "audio_element_identifier";
    }

}