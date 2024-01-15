package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
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

    public MixinAbstractSliderButton(int $$0, int $$1, int $$2, int $$3, Component $$4) {
        super($$0, $$1, $$2, $$3, $$4);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void beforeRenderWidgetFancyMenu(GuiGraphics graphics, int $$1, int $$2, float $$3, CallbackInfo ci) {
        if (!this.sliderInitializedFancyMenu) this.initializeSliderFancyMenu();
        this.sliderInitializedFancyMenu = true;
    }

    @WrapWithCondition(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private boolean wrapBlitNineSlicedInRenderWidgetFancyMenu(GuiGraphics graphics, ResourceLocation p_300860_, int p_298718_, int p_298541_, int blitWidth, int p_298426_) {
        CustomizableWidget cus = this.getAsCustomizableWidgetFancyMenu();
        boolean isHandle = (blitWidth == 8);
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
    public void setCustomSliderBackgroundNormalFancyMenu(@Nullable RenderableResource background) {
        this.customSliderBackgroundNormalFancyMenu = background;
    }

    @Unique
    @Override
    public @Nullable RenderableResource getCustomSliderBackgroundNormalFancyMenu() {
        return this.customSliderBackgroundNormalFancyMenu;
    }

    @Unique
    @Override
    public void setCustomSliderBackgroundHighlightedFancyMenu(@Nullable RenderableResource background) {
        this.customSliderBackgroundHighlightedFancyMenu = background;
    }

    @Unique
    @Override
    public @Nullable RenderableResource getCustomSliderBackgroundHighlightedFancyMenu() {
        return this.customSliderBackgroundHighlightedFancyMenu;
    }

    @Unique
    private CustomizableWidget getAsCustomizableWidgetFancyMenu() {
        return (CustomizableWidget) this;
    }

}
