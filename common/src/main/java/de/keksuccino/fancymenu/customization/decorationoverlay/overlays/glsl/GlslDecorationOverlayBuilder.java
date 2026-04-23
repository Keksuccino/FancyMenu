package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.glsl;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GlslDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<GlslDecorationOverlay> {

    public GlslDecorationOverlayBuilder() {
        super("glsl");
    }

    @Override
    public @NotNull GlslDecorationOverlay buildDefaultInstance() {
        return new GlslDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull GlslDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull GlslDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.glsl");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.glsl.desc");
    }
}
