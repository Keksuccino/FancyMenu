package de.keksuccino.fancymenu.customization.placeholder.placeholders.world;

import de.keksuccino.fancymenu.customization.placeholder.DeserializedPlaceholderString;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public class CameraRotationXFmPlaceholder extends AbstractWorldPlaceholder {

    public CameraRotationXFmPlaceholder() {
        super("camera_rotation_x_fm");
    }

    @Override
    public String getReplacementFor(DeserializedPlaceholderString dps) {
        Entity cameraEntity = Minecraft.getInstance().getCameraEntity();
        if (cameraEntity != null) {
            return String.valueOf(Mth.wrapDegrees(cameraEntity.getXRot()));
        }
        return "0";
    }

    @Override
    protected @NotNull String getLocalizationBase() {
        return "fancymenu.placeholders.world.camera_rotation_x_fm";
    }

    @Override
    public @NotNull DeserializedPlaceholderString getDefaultPlaceholderString() {
        return new DeserializedPlaceholderString(this.getIdentifier(), null, "");
    }

}
