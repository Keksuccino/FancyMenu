package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.cherryblossoms;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CherryBlossomsDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<CherryBlossomsDecorationOverlay> {

    public CherryBlossomsDecorationOverlayBuilder() {
        super("cherry_blossoms");
    }

    @Override
    public @NotNull CherryBlossomsDecorationOverlay buildDefaultInstance() {
        return new CherryBlossomsDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull CherryBlossomsDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull CherryBlossomsDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.cherry_blossoms");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
