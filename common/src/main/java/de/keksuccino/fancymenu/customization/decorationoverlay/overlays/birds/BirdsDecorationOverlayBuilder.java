package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.birds;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BirdsDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<BirdsDecorationOverlay> {

    public BirdsDecorationOverlayBuilder() {
        super("birds");
    }

    @Override
    public @NotNull BirdsDecorationOverlay buildDefaultInstance() {
        return new BirdsDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull BirdsDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull BirdsDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.birds");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.birds.desc");
    }

}
