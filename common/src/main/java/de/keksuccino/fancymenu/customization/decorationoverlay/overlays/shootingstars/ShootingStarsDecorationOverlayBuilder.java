package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.shootingstars;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShootingStarsDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<ShootingStarsDecorationOverlay> {

    public ShootingStarsDecorationOverlayBuilder() {
        super("shooting_stars");
    }

    @Override
    public @NotNull ShootingStarsDecorationOverlay buildDefaultInstance() {
        return new ShootingStarsDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull ShootingStarsDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull ShootingStarsDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.shooting_stars");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.shooting_stars.desc");
    }

}
