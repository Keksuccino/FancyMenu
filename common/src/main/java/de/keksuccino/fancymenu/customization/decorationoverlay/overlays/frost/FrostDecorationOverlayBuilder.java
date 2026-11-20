package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.frost;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FrostDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<FrostDecorationOverlay> {

    public FrostDecorationOverlayBuilder() {
        super("frost");
    }

    @Override
    public @NotNull FrostDecorationOverlay buildDefaultInstance() {
        return new FrostDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull FrostDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull FrostDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.frost");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.frost.desc");
    }

}
