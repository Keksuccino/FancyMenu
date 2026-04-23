package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringLightsDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<StringLightsDecorationOverlay> {

    public StringLightsDecorationOverlayBuilder() {
        super("string_lights");
    }

    @Override
    public @NotNull StringLightsDecorationOverlay buildDefaultInstance() {
        return new StringLightsDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull StringLightsDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull StringLightsDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.string_lights");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
