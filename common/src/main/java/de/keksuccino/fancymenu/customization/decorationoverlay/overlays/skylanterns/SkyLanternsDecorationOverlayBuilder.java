package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.skylanterns;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SkyLanternsDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<SkyLanternsDecorationOverlay> {

    public SkyLanternsDecorationOverlayBuilder() {
        super("sky_lanterns");
    }

    @Override
    public @NotNull SkyLanternsDecorationOverlay buildDefaultInstance() {
        return new SkyLanternsDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull SkyLanternsDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull SkyLanternsDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.sky_lanterns");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.sky_lanterns.desc");
    }

}
