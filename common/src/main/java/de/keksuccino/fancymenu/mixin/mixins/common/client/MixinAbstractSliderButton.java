package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.resource.PlayableResource;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
public abstract class MixinAbstractSliderButton extends AbstractWidget implements CustomizableSlider {

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

    @Override
    public void renderButton(@NotNull PoseStack $$0, int $$1, int $$2, float $$3) {

        if (!this.sliderInitializedFancyMenu) this.initializeSliderFancyMenu();
        this.sliderInitializedFancyMenu = true;

        super.renderButton($$0, $$1, $$2, $$3);

    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void beforeRenderHandleFancyMenu(PoseStack pose, Minecraft mc, int $$2, int $$3, CallbackInfo info) {
        CustomizableWidget cus = this.getAsCustomizableWidgetFancyMenu();
        boolean renderVanilla;
        int handleX = this.x + (int)(this.value * (double)(this.getWidth() - 8));
        //For sliders, the normal widget background is the slider handle texture
        renderVanilla = cus.renderCustomBackgroundFancyMenu(this, pose, handleX, this.y, 8, this.getHeight());
        //Re-bind default texture after rendering custom
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        if (renderVanilla) this.render119VanillaHandleFancyMenu(pose);
        info.cancel();
    }

    /**
     * This is to backport the 1.19+ slider handle rendering
     */
    @Unique
    private void render119VanillaHandleFancyMenu(PoseStack pose) {
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderingUtils.blitNineSliced_Vanilla(pose, this.getSliderHandleXFancyMenu(), this.y, 8, this.getHeight(), 20, 4, 200, 20, 0, this.getHandleTextureYFancyMenu());
        RenderingUtils.resetShaderColor();
    }

    @Unique
    private int getSliderHandleXFancyMenu() {
        return this.x + (int)(this.value * (double)(this.getWidth() - 8));
    }

    @Unique
    private int getHandleTextureYFancyMenu() {
        int i = 1;
        if (this.isHoveredOrFocused()) {
            i = 2;
        }
        return 46 + i * 20;
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
