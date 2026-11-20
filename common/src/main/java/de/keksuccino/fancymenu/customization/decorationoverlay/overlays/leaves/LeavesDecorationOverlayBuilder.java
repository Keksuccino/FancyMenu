package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.leaves;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LeavesDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<LeavesDecorationOverlay> {

    public LeavesDecorationOverlayBuilder() {
        super("leaves");
    }

    @Override
    public @NotNull LeavesDecorationOverlay buildDefaultInstance() {
        return new LeavesDecorationOverlay();
    }

    /**
     * Leaves saved before the overlay had the Colors option always used their start and end color. If they were switched on,
     * they keep their colors through the Custom Range. Leaves that were never switched on count as new.
     */
    @Override
    protected void deserialize(@NotNull LeavesDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
        if (deserializeFrom.getValue(instanceToWrite.leavesColors.getKey()) != null) return;
        if (!instanceToWrite.showOverlay.tryGetNonNullElse(false)) return;
        instanceToWrite.leavesColors.set(LeavesColorMode.CUSTOM_RANGE);
    }

    @Override
    protected void serialize(@NotNull LeavesDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.leaves");
    }

    @Override
    public @Nullable Component getDescription() {
        return null;
    }

}
