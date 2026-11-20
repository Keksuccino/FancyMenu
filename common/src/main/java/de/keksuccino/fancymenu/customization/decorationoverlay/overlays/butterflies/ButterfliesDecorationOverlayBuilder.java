package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.butterflies;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ButterfliesDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<ButterfliesDecorationOverlay> {

    public ButterfliesDecorationOverlayBuilder() {
        super("butterflies");
    }

    @Override
    public @NotNull ButterfliesDecorationOverlay buildDefaultInstance() {
        return new ButterfliesDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull ButterfliesDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull ButterfliesDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.butterflies");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.butterflies.desc");
    }

}
