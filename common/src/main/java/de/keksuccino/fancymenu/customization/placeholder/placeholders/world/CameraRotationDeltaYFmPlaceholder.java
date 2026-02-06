package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import de.keksuccino.fancymenu.util.player.CameraRotationObserver;
import org.jetbrains.annotations.NotNull;

public class CameraRotationDeltaYFmPlaceholder extends AbstractWorldPlaceholder {

    public CameraRotationDeltaYFmPlaceholder() {
        super("camera_rotation_delta_y_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        return String.valueOf(CameraRotationObserver.getCurrentRotationDeltaY());
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.camera_rotation_delta_y_fm";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
