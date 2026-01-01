package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.confetti;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
public class ConfettiDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<ConfettiDecorationOverlay> {

    public ConfettiDecorationOverlayBuilder() {
        super("confetti");
    }

    @Override
    public @NotNull ConfettiDecorationOverlay buildDefaultInstance() {
        return new ConfettiDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull ConfettiDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull ConfettiDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.confetti");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
