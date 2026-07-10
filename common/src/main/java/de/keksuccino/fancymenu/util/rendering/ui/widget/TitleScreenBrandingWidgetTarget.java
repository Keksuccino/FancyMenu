package de.keksuccino.fancymenu.util.rendering.ui.widget;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Adapts a title-screen branding renderer and its widget to the Minecraft-independent capture controller.
 */
public final class TitleScreenBrandingWidgetTarget implements TitleScreenBrandingCaptureController.Target {

    private final BrandingRenderer renderer;
    private final RendererWidget widget;

    public TitleScreenBrandingWidgetTarget(@NotNull BrandingRenderer renderer, @NotNull RendererWidget widget) {
        this.renderer = renderer;
        this.widget = widget;
    }

    @Override
    public void setBrandingText(@NotNull String brandingText) {
        this.renderer.setLines(List.of(Component.literal(brandingText)));
    }

    @Override
    public boolean hasCustomWidth() {
        return ((CustomizableWidget)this.widget).getCustomWidthFancyMenu() != null;
    }

    @Override
    public void resizeWidthToContent() {
        this.widget.setWidth(this.renderer.getTotalWidth());
    }

    @Override
    public boolean hasCustomHeight() {
        return ((CustomizableWidget)this.widget).getCustomHeightFancyMenu() != null;
    }

    @Override
    public void resizeHeightToContent() {
        this.widget.setHeight(this.renderer.getTotalHeight());
    }
}
