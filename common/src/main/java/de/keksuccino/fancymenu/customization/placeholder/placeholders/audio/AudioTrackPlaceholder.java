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

public class AudioTrackPlaceholder extends Placeholder {

    private static final String DISPLAY_NAME_MAPPING_SEPARATOR = "%:%";
    private static final String DISPLAY_NAME_MAPPING_ARROW = "=>";

    public AudioTrackPlaceholder() {
        super("audio_element_current_track");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String elementId = dps.values.get("element_identifier");
        String displayNameMappings = dps.values.get("display_name_mappings");

        if (elementId != null) {
            ScreenCustomizationLayer activeLayer = ScreenCustomizationLayerHandler.getActiveLayer();
            if (activeLayer != null) {
                AbstractElement element = activeLayer.getElementByInstanceIdentifier(elementId);
                if (element instanceof AudioElement audio) {
                    // Get the current audio track
                    AudioElement.AudioInstance currentTrack = audio.currentAudioInstance;
                    if (currentTrack != null) {
                        String source = currentTrack.supplier.getSourceWithoutPrefix();
                        // Get just the filename without path
                        int lastSlash = source.lastIndexOf('/');
                        if (lastSlash != -1 && lastSlash < source.length() - 1) {
                            source = source.substring(lastSlash + 1);
                        }

                        // Check if we should use a display name mapping
                        if (displayNameMappings != null && !displayNameMappings.isEmpty()) {
                            String displayName = getDisplayNameForTrack(source, displayNameMappings);
                            if (displayName != null) {
                                return displayName;
                            }
                        }

                        return source;
                    }
                }
            }
        }
        return "---";
    }

    /**
     * Parse the display name mappings and try to find a match for the given track name.
     * Format: "track1.ogg=>Cool Track Name%:%track2.wav=>Another Track Name"
     */
    @Nullable
    private String getDisplayNameForTrack(String trackName, String mappings) {
        String[] mappingEntries = mappings.split(DISPLAY_NAME_MAPPING_SEPARATOR);
        for (String mapping : mappingEntries) {
            String[] parts = mapping.trim().split(DISPLAY_NAME_MAPPING_ARROW, 2);
            if (parts.length == 2) {
                String fileName = parts[0].trim();
                String displayName = parts[1].trim();
                if (trackName.equals(fileName)) {
                    return displayName;
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("element_identifier");
        l.add("display_name_mappings");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.audio_element_current_track");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.audio_element_current_track.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.audio");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put("element_identifier", "put_identifier_of_audio_element_here");
        m.put("display_name_mappings", "track1.ogg=>Cool Track Name%:%track2.wav=>Another Name");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}