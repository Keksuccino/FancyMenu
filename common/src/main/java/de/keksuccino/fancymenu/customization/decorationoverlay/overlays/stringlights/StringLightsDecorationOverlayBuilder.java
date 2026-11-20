package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.stringlights;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.customization.decorationoverlay.DecorationOverlayLook;
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

    /**
     * String lights saved before the overlay had a look were drawn flat, with gently flickering bulbs. If they were switched on,
     * they keep that familiar style: the simple look with the twinkle program. String lights that were never switched on count as new.
     */
    @Override
    protected void deserialize(@NotNull StringLightsDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
        if (deserializeFrom.getValue(instanceToWrite.look.property.getKey()) != null) return;
        if (!instanceToWrite.showOverlay.tryGetNonNullElse(false)) return;
        instanceToWrite.look.property.set(DecorationOverlayLook.SIMPLE);
        if (deserializeFrom.getValue(instanceToWrite.program.getKey()) == null) instanceToWrite.program.set(StringLightsProgram.TWINKLE);
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
