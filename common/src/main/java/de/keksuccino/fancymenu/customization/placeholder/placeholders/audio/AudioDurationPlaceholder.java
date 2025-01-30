package de.keksuccino.fancymenu.customization.placeholder.placeholders.audio;

import de.keksuccino.fancymenu.customization.element.AbstractElement;
import de.keksuccino.fancymenu.customization.element.elements.audio.AudioElement;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

public class AudioDurationPlaceholder extends Placeholder {

    public AudioDurationPlaceholder() {
        super("audio_duration");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String elementId = dps.values.get("element_identifier");
        if (elementId != null) {
            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
            if (layer != null) {
                AbstractElement element = layer.getElementByInstanceIdentifier(elementId);
                if (element instanceof AudioElement audio) {
                    if (audio.currentAudio != null && audio.currentAudio.isReady()) {
                        float durationSeconds = audio.currentAudio.getDuration();
                        // Format duration as MM:SS
                        int minutes = (int)(durationSeconds / 60);
                        int seconds = (int)(durationSeconds % 60);
                        return String.format("%02d:%02d", minutes, seconds);
                    }
                }
            }
        }
        return "00:00";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("element_identifier");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.audio_duration");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.audio_duration.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.audio");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        Map<String, String> m = new HashMap<>();
        m.put("element_identifier", "put_identifier_of_audio_element_here");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}