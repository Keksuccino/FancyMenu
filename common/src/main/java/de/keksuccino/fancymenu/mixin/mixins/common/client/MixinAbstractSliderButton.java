package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.resources.PlayableResource;
import de.keksuccino.fancymenu.util.resources.RenderableResource;
import net.minecraft.client.gui.GuiComponent;
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

    private static final ResourceLocation SLIDER_LOCATION_FANCYMENU = new ResourceLocation("textures/gui/slider.png");

    @Shadow private boolean canChangeValue;
    @Shadow protected double value;

    @Shadow public abstract void renderWidget(PoseStack p_275635_, int p_275335_, int p_275551_, float p_275511_);

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
    private void beforeRenderWidgetFancyMenu(PoseStack pose, int $$1, int $$2, float $$3, CallbackInfo info) {
        if (!this.sliderInitializedFancyMenu) this.initializeSliderFancyMenu();
        this.sliderInitializedFancyMenu = true;
    }

    @WrapWithCondition(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSliderButton;blitNineSliced(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIIIIIII)V"))
    private boolean wrapBlitNineSlicedInRenderWidgetFancyMenu(PoseStack pose, int x, int y, int width, int height, int i5, int i6, int i7, int i8, int i9, int i10) {
        CustomizableWidget cus = this.getAsCustomizableWidgetFancyMenu();
        boolean isHandle = (width == 8);
        boolean renderVanilla;
        if (isHandle) {
            int handleX = this.getX() + (int)(this.value * (double)(this.getWidth() - 8));
            //For sliders, the normal widget background is the slider handle texture
            renderVanilla = cus.renderCustomBackgroundFancyMenu(this, pose, handleX, this.getY(), 8, this.getHeight());
        } else {
            renderVanilla = this.renderSliderBackgroundFancyMenu(pose);
        }
        //Re-bind default texture after rendering custom
        RenderSystem.setShaderTexture(0, SLIDER_LOCATION_FANCYMENU);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
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

    /**
     * Returns if the slider should render its Vanilla background (true) or not (false).
     */
    @Unique
    private boolean renderSliderBackgroundFancyMenu(PoseStack pose) {
        ResourceLocation location = null;
        if (this.isFocused() && !this.canChangeValue) {
            if (this.customSliderBackgroundNormalFancyMenu instanceof PlayableResource p) p.pause();
            if (this.customSliderBackgroundHighlightedFancyMenu != null) {
                if (this.customSliderBackgroundHighlightedFancyMenu instanceof PlayableResource p) p.play();
                location = this.customSliderBackgroundHighlightedFancyMenu.getResourceLocation();
            }
        } else {
            if (this.customSliderBackgroundHighlightedFancyMenu instanceof PlayableResource p) p.pause();
            if (this.customSliderBackgroundNormalFancyMenu != null) {
                if (this.customSliderBackgroundNormalFancyMenu instanceof PlayableResource p) p.play();
                location = this.customSliderBackgroundNormalFancyMenu.getResourceLocation();
            }
        }
        if (location != null) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, ((IMixinAbstractWidget)this).getAlphaFancyMenu());
            RenderSystem.enableBlend();
            RenderingUtils.bindTexture(location);
            GuiComponent.blit(pose, this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            RenderingUtils.resetShaderColor();
            return false;
        }
        return true;
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
