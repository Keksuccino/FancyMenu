package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.buddy;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuddyDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<BuddyDecorationOverlay> {

    public BuddyDecorationOverlayBuilder() {
        super("buddy");
    }

    @Override
    public @NotNull BuddyDecorationOverlay buildDefaultInstance() {
        return new BuddyDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull BuddyDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull BuddyDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.buddy");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.buddy.desc");
    }

}
