package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.embers;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmbersDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<EmbersDecorationOverlay> {

    public EmbersDecorationOverlayBuilder() {
        super("embers");
    }

    @Override
    public @NotNull EmbersDecorationOverlay buildDefaultInstance() {
        return new EmbersDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull EmbersDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull EmbersDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.embers");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.embers.desc");
    }

}
