package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.sunbeams;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SunbeamsDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<SunbeamsDecorationOverlay> {

    public SunbeamsDecorationOverlayBuilder() {
        super("sunbeams");
    }

    @Override
    public @NotNull SunbeamsDecorationOverlay buildDefaultInstance() {
        return new SunbeamsDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull SunbeamsDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull SunbeamsDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.sunbeams");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.sunbeams.desc");
    }

}
