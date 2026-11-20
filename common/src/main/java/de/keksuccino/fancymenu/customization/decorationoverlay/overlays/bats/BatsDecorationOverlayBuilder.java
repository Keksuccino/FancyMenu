package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.bats;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BatsDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<BatsDecorationOverlay> {

    public BatsDecorationOverlayBuilder() {
        super("bats");
    }

    @Override
    public @NotNull BatsDecorationOverlay buildDefaultInstance() {
        return new BatsDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull BatsDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull BatsDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.bats");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.bats.desc");
    }

}
