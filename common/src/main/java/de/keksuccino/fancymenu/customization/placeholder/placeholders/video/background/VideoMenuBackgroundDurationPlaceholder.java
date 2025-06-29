package de.keksuccino.fancymenu.customization.placeholder.placeholders.video.background;

import de.keksuccino.fancymenu.customization.background.MenuBackground;
import de.keksuccino.fancymenu.customization.background.backgrounds.video.IVideoMenuBackground;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayer;
import de.keksuccino.fancymenu.customization.layer.ScreenCustomizationLayerHandler;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class VideoMenuBackgroundDurationPlaceholder extends Placeholder {

    public VideoMenuBackgroundDurationPlaceholder() {
        super("video_background_duration");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String backId = dps.values.get("background_identifier");
        if (backId != null) {
            ScreenCustomizationLayer layer = ScreenCustomizationLayerHandler.getActiveLayer();
            if (layer != null) {
                MenuBackground back = layer.getMenuBackgroundByInstanceIdentifier(backId);
                if (back instanceof IVideoMenuBackground video) {
                    float durationSeconds = video.getDuration();
                    // Format duration as MM:SS
                    int minutes = (int)(durationSeconds / 60);
                    int seconds = (int)(durationSeconds % 60);
                    return String.format("%02d:%02d", minutes, seconds);
                }
            }
        }
        return "00:00";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("background_identifier");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.video_background_duration");
    }

    @Override
    public List<String> getDescription() {
        return Arrays.asList(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.video_background_duration.desc"));
    }

    @Override
    public String getCategory() {
        return I18n.get("fancymenu.placeholders.categories.video");
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        LinkedHashMap<String, String> m = new LinkedHashMap<>();
        m.put("background_identifier", "put_identifier_of_video_background_here");
        return DeserializedPlaceholderString.build(this.getIdentifier(), m);
    }

}