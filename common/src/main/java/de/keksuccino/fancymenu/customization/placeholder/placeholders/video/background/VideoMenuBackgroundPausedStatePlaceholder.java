package de.keksuccino.fancymenu.customization.placeholder.placeholders.video.background;

import de.keksuccino.fancymenu.customization.element.elements.video.VideoElementController;
import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.customization.placeholder.Placeholder;
import de.keksuccino.fancymenu.util.LocalizationUtils;
import net.minecraft.client.resources.language.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class VideoMenuBackgroundPausedStatePlaceholder extends Placeholder {

    public VideoMenuBackgroundPausedStatePlaceholder() {
        super("video_background_paused_state");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        String s = dps.values.get("background_identifier");
        if (s != null) {
            VideoElementController.VideoElementMeta meta = VideoElementController.getMeta(s);
            if (meta != null) {
                return meta.paused ? "true" : "false";
            }
        }
        return "false";
    }

    @Override
    public @Nullable List<String> getValueNames() {
        List<String> l = new ArrayList<>();
        l.add("background_identifier");
        return l;
    }

    @Override
    public @NotNull String getDisplayName() {
        return I18n.get("fancymenu.placeholders.video_background_paused_state");
    }

    @Override
    public List<String> getDescription() {
        return List.of(LocalizationUtils.splitLocalizedStringLines("fancymenu.placeholders.video_background_paused_state.desc"));
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