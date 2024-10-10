package de.keksuccino.fancymenu.customization.placeholder.placeholders.audio;

import de.keksuccino.fancymenu.customization.element.elements.audio.AudioElementController;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

//TODO übernehmen
public class AudioElementVolumePlaceholder extends Placeholder {

    private static final Logger LOGGER = LogManager.getLogger();

    public AudioElementVolumePlaceholder() {
        super("audio_element_vol");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String s = dps.values.get("element_identifier");
        if (s != null) {
            AudioElementController.AudioElementMeta meta = AudioElementController.getMeta(s);
            if (meta != null) {
                return "" + meta.volume;
            }
        }
        return "0.0";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("element_identifier");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.audio_element_volume");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.audio_element_volume.desc"));
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
