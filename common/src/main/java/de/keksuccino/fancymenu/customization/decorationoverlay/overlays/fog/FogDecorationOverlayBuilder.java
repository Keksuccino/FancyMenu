package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fog;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FogDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<FogDecorationOverlay> {

    public FogDecorationOverlayBuilder() {
        super("fog");
    }

    @Override
    public @NotNull FogDecorationOverlay buildDefaultInstance() {
        return new FogDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull FogDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull FogDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.fog");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.fog.desc");
    }

}
