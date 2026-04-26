package de.keksuccino.fancymenu.customization.decorationoverlay.overlays.browser;

import de.keksuccino.fancymenu.customization.decorationoverlay.AbstractDecorationOverlayBuilder;
import de.keksuccino.fancymenu.util.properties.PropertyContainer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BrowserDecorationOverlayBuilder extends AbstractDecorationOverlayBuilder<BrowserDecorationOverlay> {

    public BrowserDecorationOverlayBuilder() {
        super("browser");
    }

    @Override
    public @NotNull BrowserDecorationOverlay buildDefaultInstance() {
        return new BrowserDecorationOverlay();
    }

    @Override
    protected void deserialize(@NotNull BrowserDecorationOverlay instanceToWrite, @NotNull PropertyContainer deserializeFrom) {
    }

    @Override
    protected void serialize(@NotNull BrowserDecorationOverlay instanceToSerialize, @NotNull PropertyContainer serializeTo) {
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("fancymenu.decoration_overlays.browser");
    }

    @Override
    public @Nullable Component getDescription() {
        return Component.translatable("fancymenu.decoration_overlays.browser.desc");
    }
}
