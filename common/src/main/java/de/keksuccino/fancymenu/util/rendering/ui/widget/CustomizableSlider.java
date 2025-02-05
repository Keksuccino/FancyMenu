package de.keksuccino.fancymenu.util.rendering.ui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractWidget;
import de.keksuccino.fancymenu.util.ClassExtender;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * This interface gets applied to the {@link AbstractSliderButton} class to add a bunch of helper methods for easier customization.
 */
@ClassExtender(AbstractSliderButton.class)
public interface CustomizableSlider {

    void setNineSliceCustomSliderBackground_FancyMenu(boolean nineSlice);

    boolean isNineSliceCustomSliderBackground_FancyMenu();

    void setNineSliceSliderBackgroundBorderX_FancyMenu(int borderX);

    int getNineSliceSliderBackgroundBorderX_FancyMenu();

    void setNineSliceSliderBackgroundBorderY_FancyMenu(int borderY);

    int getNineSliceSliderBackgroundBorderY_FancyMenu();

    void setNineSliceCustomSliderHandle_FancyMenu(boolean nineSlice);

    boolean isNineSliceCustomSliderHandle_FancyMenu();

    void setNineSliceSliderHandleBorderX_FancyMenu(int borderX);

    int getNineSliceSliderHandleBorderX_FancyMenu();

    void setNineSliceSliderHandleBorderY_FancyMenu(int borderY);

    int getNineSliceSliderHandleBorderY_FancyMenu();

    void setCustomSliderBackgroundNormalFancyMenu(@Nullable RenderableResource background);

    @Nullable
    RenderableResource getCustomSliderBackgroundNormalFancyMenu();

    void setCustomSliderBackgroundHighlightedFancyMenu(@Nullable RenderableResource background);

    @Nullable
    RenderableResource getCustomSliderBackgroundHighlightedFancyMenu();

    /**
     * Returns if the slider should render its Vanilla background (true) or not (false).
     */
    default boolean renderSliderBackgroundFancyMenu(GuiGraphics graphics, AbstractSliderButton widget, boolean canChangeValue) {
        ResourceLocation location = null;
        RenderableResource texture = null;
        if (widget.isFocused() && !canChangeValue) {
            if (this.getCustomSliderBackgroundNormalFancyMenu() instanceof PlayableResource p) p.pause();
            if (this.getCustomSliderBackgroundHighlightedFancyMenu() != null) {
                if (this.getCustomSliderBackgroundHighlightedFancyMenu() instanceof PlayableResource p) p.play();
                texture = this.getCustomSliderBackgroundHighlightedFancyMenu();
                location = this.getCustomSliderBackgroundHighlightedFancyMenu().getResourceLocation();
            }
        } else {
            if (this.getCustomSliderBackgroundHighlightedFancyMenu() instanceof PlayableResource p) p.pause();
            if (this.getCustomSliderBackgroundNormalFancyMenu() != null) {
                if (this.getCustomSliderBackgroundNormalFancyMenu() instanceof PlayableResource p) p.play();
                texture = this.getCustomSliderBackgroundNormalFancyMenu();
                location = this.getCustomSliderBackgroundNormalFancyMenu().getResourceLocation();
            }
        }
        if (location != null) {
            graphics.setColor(1.0F, 1.0F, 1.0F, ((IMixinAbstractWidget)this).getAlphaFancyMenu());
            RenderSystem.enableBlend();
            if (this.isNineSliceCustomSliderBackground_FancyMenu()) {
                RenderingUtils.blitNineSlicedTexture(graphics, location, widget.x, widget.y, widget.getWidth(), widget.getHeight(), texture.getWidth(), texture.getHeight(), this.getNineSliceSliderBackgroundBorderY_FancyMenu(), this.getNineSliceSliderBackgroundBorderX_FancyMenu(), this.getNineSliceSliderBackgroundBorderY_FancyMenu(), this.getNineSliceSliderBackgroundBorderX_FancyMenu());
            } else {
                graphics.blit(location, widget.x, widget.y, 0.0F, 0.0F, widget.getWidth(), widget.getHeight(), widget.getWidth(), widget.getHeight());
            }
            RenderingUtils.resetShaderColor(graphics);
            return false;
        }
        return true;
    }

}
