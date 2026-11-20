package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cobwebs;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CobwebsDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<CobwebsDecorationOverlay> {

    public CobwebsDecorationOverlayBuilder() {
        super("cobwebs");
    }

    @Override
    public @NotNull CobwebsDecorationOverlay buildDefaultInstance() {
        return new CobwebsDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull CobwebsDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull CobwebsDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.cobwebs");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.cobwebs.desc");
    }

}
