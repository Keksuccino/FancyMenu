package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.snow;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
public class SnowDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<SnowDecorationOverlay> {

    public SnowDecorationOverlayBuilder() {
        super("snowfall");
    }

    @Override
    public @NotNull SnowDecorationOverlay buildDefaultInstance() {
        return new SnowDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull SnowDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull SnowDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.snow");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
