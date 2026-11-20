package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.aurora;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AuroraDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<AuroraDecorationOverlay> {

    public AuroraDecorationOverlayBuilder() {
        super("aurora");
    }

    @Override
    public @NotNull AuroraDecorationOverlay buildDefaultInstance() {
        return new AuroraDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull AuroraDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull AuroraDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.aurora");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.aurora.desc");
    }

}
