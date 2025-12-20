package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("unused")
@Mixin(AbstractSliderButton.class)
public abstract class MixinAbstractSliderButton extends AbstractWidget implements CustomizableSlider {

    @Shadow private boolean canChangeValue;
    @Shadow protected double value;

    @Unique @Nullable
    private RenderableResource customSliderBackgroundNormalFancyMenu;
    @Unique @Nullable
    private RenderableResource customSliderBackgroundHighlightedFancyMenu;
    @Unique
    private boolean sliderInitializedFancyMenu = false;
    @Unique
    private boolean nineSliceSliderBackground_FancyMenu = false;
    @Unique
    private int nineSliceSliderBackgroundBorderX_FancyMenu = 5;
    @Unique
    private int nineSliceSliderBackgroundBorderY_FancyMenu = 5;
    @Unique
    private boolean nineSliceSliderHandle_FancyMenu = false;
    @Unique
    private int nineSliceSliderHandleBorderX_FancyMenu = 5;
    @Unique
    private int nineSliceSliderHandleBorderY_FancyMenu = 5;

    public MixinAbstractSliderButton(int $$0, int $$1, int $$2, int $$3, Component $$4) {
        super($$0, $$1, $$2, $$3, $$4);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void beforeRenderWidgetFancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo ci) {
        if (!this.sliderInitializedFancyMenu) this.initializeSliderFancyMenu();
        this.sliderInitializedFancyMenu = true;
    }

    /**
     * @reason This is to add support for custom textures to the slider.
     */
    @WrapWithCondition(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private boolean wrap_blitSprite_FancyMenu(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
        CustomizableWidget cus = this.getAsCustomizableWidgetFancyMenu();
        boolean isHandle = (width == 8);
        boolean renderVanilla;
        if (isHandle) {
            int handleX = this.getX() + (int)(this.value * (double)(this.getWidth() - 8));
            //For sliders, the normal widget background is the slider handle texture
            renderVanilla = cus.renderCustomBackgroundFancyMenu(this, graphics, handleX, this.getY(), 8, this.getHeight());
        } else {
            renderVanilla = this.renderSliderBackgroundFancyMenu(graphics, (AbstractSliderButton)((Object)this), this.canChangeValue);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        return renderVanilla;
    }

    @Unique
    private void initializeSliderFancyMenu() {

        CustomizableWidget cus = this.getAsCustomizableWidgetFancyMenu();

        cus.addResetCustomizationsListenerFancyMenu(() -> {
            if (this.getCustomSliderBackgroundNormalFancyMenu() instanceof PlayableResource p) p.stop();
            if (this.getCustomSliderBackgroundHighlightedFancyMenu() instanceof PlayableResource p) p.stop();
            this.setCustomSliderBackgroundNormalFancyMenu(null);
            this.setCustomSliderBackgroundHighlightedFancyMenu(null);
        });

        cus.addHoverOrFocusStateListenerFancyMenu(hoveredOrFocused -> {
            CustomizableWidget.CustomBackgroundResetBehavior behavior = cus.getCustomBackgroundResetBehaviorFancyMenu();
            if (hoveredOrFocused && ((behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER) || (behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER_AND_UNHOVER))) {
                if (this.getCustomSliderBackgroundNormalFancyMenu() instanceof PlayableResource p) p.stop();
                if (this.getCustomSliderBackgroundHighlightedFancyMenu() instanceof PlayableResource p) p.stop();
            }
            if (!hoveredOrFocused && ((behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_UNHOVER) || (behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER_AND_UNHOVER))) {
                if (this.getCustomSliderBackgroundNormalFancyMenu() instanceof PlayableResource p) p.stop();
                if (this.getCustomSliderBackgroundHighlightedFancyMenu() instanceof PlayableResource p) p.stop();
            }
        });

    }

    @Unique
    @Override
    public void setNineSliceCustomSliderBackground_FancyMenu(boolean nineSlice) {
        this.nineSliceSliderBackground_FancyMenu = nineSlice;
    }

    @Unique
    @Override
    public boolean isNineSliceCustomSliderBackground_FancyMenu() {
        return this.nineSliceSliderBackground_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderX_FancyMenu(int nineSliceSliderBorderX_FancyMenu) {
        this.nineSliceSliderBackgroundBorderX_FancyMenu = nineSliceSliderBorderX_FancyMenu;
    }

    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderX_FancyMenu() {
        return nineSliceSliderBackgroundBorderX_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderY_FancyMenu(int nineSliceSliderBorderY_FancyMenu) {
        this.nineSliceSliderBackgroundBorderY_FancyMenu = nineSliceSliderBorderY_FancyMenu;
    }

    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderY_FancyMenu() {
        return nineSliceSliderBackgroundBorderY_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceCustomSliderHandle_FancyMenu(boolean nineSlice) {
        this.nineSliceSliderHandle_FancyMenu = nineSlice;
    }

    @Unique
    @Override
    public boolean isNineSliceCustomSliderHandle_FancyMenu() {
        return this.nineSliceSliderHandle_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderHandleBorderX_FancyMenu(int nineSliceSliderHandleBorderX_FancyMenu) {
        this.nineSliceSliderHandleBorderX_FancyMenu = nineSliceSliderHandleBorderX_FancyMenu;
    }

    @Unique
    @Override
    public int getNineSliceSliderHandleBorderX_FancyMenu() {
        return nineSliceSliderHandleBorderX_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderHandleBorderY_FancyMenu(int nineSliceSliderHandleBorderY_FancyMenu) {
        this.nineSliceSliderHandleBorderY_FancyMenu = nineSliceSliderHandleBorderY_FancyMenu;
    }

    @Unique
    @Override
    public int getNineSliceSliderHandleBorderY_FancyMenu() {
        return nineSliceSliderHandleBorderY_FancyMenu;
    }

    @Unique
    @Override
    public void setCustomSliderBackgroundNormalFancyMenu(@Nullable RenderableResource background) {
        this.customSliderBackgroundNormalFancyMenu = background;
    }

    @Unique
    @Override
    public @Nullable RenderableResource getCustomSliderBackgroundNormalFancyMenu() {
        if (this.customSliderBackgroundNormalFancyMenu != null) return this.customSliderBackgroundNormalFancyMenu;
        if ((Object)this instanceof AbstractExtendedSlider slider) {
            if (slider.getSliderBackgroundColorNormal() != null) return null;
        }
        return GlobalCustomizationHandler.getCustomSliderBackground();
    }

    @Unique
    @Override
    public void setCustomSliderBackgroundHighlightedFancyMenu(@Nullable RenderableResource background) {
        this.customSliderBackgroundHighlightedFancyMenu = background;
    }

    @Unique
    @Override
    public @Nullable RenderableResource getCustomSliderBackgroundHighlightedFancyMenu() {
        if (this.customSliderBackgroundHighlightedFancyMenu != null) return this.customSliderBackgroundHighlightedFancyMenu;
        if ((Object)this instanceof AbstractExtendedSlider slider) {
            if (slider.getSliderBackgroundColorHighlighted() != null) return null;
        }
        return GlobalCustomizationHandler.getCustomSliderBackground();
    }

    @Unique
    private CustomizableWidget getAsCustomizableWidgetFancyMenu() {
        return (CustomizableWidget) this;
    }

}
