package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.bubbles;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BubblesDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<BubblesDecorationOverlay> {

    public BubblesDecorationOverlayBuilder() {
        super("bubbles");
    }

    @Override
    public @NotNull BubblesDecorationOverlay buildDefaultInstance() {
        return new BubblesDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull BubblesDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull BubblesDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.bubbles");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.bubbles.desc");
    }

}
