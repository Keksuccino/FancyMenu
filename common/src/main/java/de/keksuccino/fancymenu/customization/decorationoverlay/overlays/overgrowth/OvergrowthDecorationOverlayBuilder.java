package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.overgrowth;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OvergrowthDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<OvergrowthDecorationOverlay> {

    public OvergrowthDecorationOverlayBuilder() {
        super("overgrowth");
    }

    @Override
    public @NotNull OvergrowthDecorationOverlay buildDefaultInstance() {
        return new OvergrowthDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull OvergrowthDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull OvergrowthDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.overgrowth");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.overgrowth.desc");
    }

}
