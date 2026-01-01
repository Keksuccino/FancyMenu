package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.rain;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RainDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<RainDecorationOverlay> {

    public RainDecorationOverlayBuilder() {
        super("rainfall");
    }

    @Override
    public @NotNull RainDecorationOverlay buildDefaultInstance() {
        return new RainDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull RainDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull RainDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.rain");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
