package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.firefly;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FireflyDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<FireflyDecorationOverlay> {

    public FireflyDecorationOverlayBuilder() {
        super("fireflies");
    }

    @Override
    public @NotNull FireflyDecorationOverlay buildDefaultInstance() {
        return new FireflyDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull FireflyDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull FireflyDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.fireflies");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
