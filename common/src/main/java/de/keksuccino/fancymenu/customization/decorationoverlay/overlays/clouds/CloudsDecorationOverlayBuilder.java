package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.clouds;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CloudsDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<CloudsDecorationOverlay> {

    public CloudsDecorationOverlayBuilder() {
        super("clouds");
    }

    @Override
    public @NotNull CloudsDecorationOverlay buildDefaultInstance() {
        return new CloudsDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull CloudsDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull CloudsDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.clouds");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.clouds.desc");
    }

}
