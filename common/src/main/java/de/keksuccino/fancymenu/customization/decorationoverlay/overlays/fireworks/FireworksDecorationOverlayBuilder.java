package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.fireworks;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FireworksDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<FireworksDecorationOverlay> {

    public FireworksDecorationOverlayBuilder() {
        super("fireworks");
    }

    @Override
    public @NotNull FireworksDecorationOverlay buildDefaultInstance() {
        return new FireworksDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull FireworksDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull FireworksDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.fireworks");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
