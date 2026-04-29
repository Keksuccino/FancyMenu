package de.keksuccino.fancymenu.mixin.mixins.common.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2.AbstractExtendedSlider;
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

@SuppressWarnings("unused")
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
    private int nineSliceSliderBackgroundBorderTop_FancyMenu = 5;
    @Unique
    private int nineSliceSliderBackgroundBorderRight_FancyMenu = 5;
    @Unique
    private int nineSliceSliderBackgroundBorderBottom_FancyMenu = 5;
    @Unique
    private int nineSliceSliderBackgroundBorderLeft_FancyMenu = 5;
    @Unique
    private boolean nineSliceSliderHandle_FancyMenu = false;
    @Unique
    private int nineSliceSliderHandleBorderX_FancyMenu = 5;
    @Unique
    private int nineSliceSliderHandleBorderY_FancyMenu = 5;
    @Unique
    private int nineSliceSliderHandleBorderTop_FancyMenu = 5;
    @Unique
    private int nineSliceSliderHandleBorderRight_FancyMenu = 5;
    @Unique
    private int nineSliceSliderHandleBorderBottom_FancyMenu = 5;
    @Unique
    private int nineSliceSliderHandleBorderLeft_FancyMenu = 5;

    public MixinAbstractSliderButton(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    public void renderButton(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        if (!this.sliderInitializedFancyMenu) this.initializeSliderFancyMenu();
        this.sliderInitializedFancyMenu = true;
        super.renderButton(pose, mouseX, mouseY, partial);
    }

    /**
     * @reason Render custom FancyMenu slider handles on the 1.19.2 renderBg path.
     */
    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void beforeRenderHandleFancyMenu(PoseStack pose, Minecraft minecraft, int mouseX, int mouseY, CallbackInfo info) {
        CustomizableWidget customizable = this.getAsCustomizableWidgetFancyMenu();
        int handleX = this.getSliderHandleXFancyMenu();
        boolean renderVanilla = customizable.renderCustomBackgroundFancyMenu(this, GuiGraphics.currentGraphics(), handleX, this.y, 8, this.getHeight());
        RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        if (renderVanilla) this.render119VanillaHandleFancyMenu();
        info.cancel();
    }

    @Unique
    private void render119VanillaHandleFancyMenu() {
        GuiGraphics graphics = GuiGraphics.currentGraphics();
        graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        graphics.blitNineSliced(WIDGETS_LOCATION, this.getSliderHandleXFancyMenu(), this.y, 8, this.getHeight(), 20, 4, 200, 20, 0, this.getHandleTextureYFancyMenu());
        RenderingUtils.resetShaderColor(graphics);
    }

    @Unique
    private int getSliderHandleXFancyMenu() {
        return this.x + (int)(this.value * (double)(this.getWidth() - 8));
    }

    @Unique
    private int getHandleTextureYFancyMenu() {
        int state = 1;
        if (this.isHoveredOrFocused()) {
            state = 2;
        }
        return 46 + state * 20;
    }

    @Unique
    private void initializeSliderFancyMenu() {
        CustomizableWidget customizable = this.getAsCustomizableWidgetFancyMenu();
        customizable.addResetCustomizationsListenerFancyMenu(() -> {
            if (this.getCustomSliderBackgroundNormalFancyMenu() instanceof PlayableResource playable) playable.stop();
            if (this.getCustomSliderBackgroundHighlightedFancyMenu() instanceof PlayableResource playable) playable.stop();
            this.setCustomSliderBackgroundNormalFancyMenu(null);
            this.setCustomSliderBackgroundHighlightedFancyMenu(null);
        });

        customizable.addHoverOrFocusStateListenerFancyMenu(hoveredOrFocused -> {
            CustomizableWidget.CustomBackgroundResetBehavior behavior = customizable.getCustomBackgroundResetBehaviorFancyMenu();
            if (hoveredOrFocused && ((behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER) || (behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER_AND_UNHOVER))) {
                if (this.getCustomSliderBackgroundNormalFancyMenu() instanceof PlayableResource playable) playable.stop();
                if (this.getCustomSliderBackgroundHighlightedFancyMenu() instanceof PlayableResource playable) playable.stop();
            }
            if (!hoveredOrFocused && ((behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_UNHOVER) || (behavior == CustomizableWidget.CustomBackgroundResetBehavior.RESET_ON_HOVER_AND_UNHOVER))) {
                if (this.getCustomSliderBackgroundNormalFancyMenu() instanceof PlayableResource playable) playable.stop();
                if (this.getCustomSliderBackgroundHighlightedFancyMenu() instanceof PlayableResource playable) playable.stop();
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
    public void setNineSliceSliderBackgroundBorderX_FancyMenu(int borderX) {
        this.nineSliceSliderBackgroundBorderX_FancyMenu = borderX;
        this.nineSliceSliderBackgroundBorderLeft_FancyMenu = borderX;
        this.nineSliceSliderBackgroundBorderRight_FancyMenu = borderX;
    }

    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderX_FancyMenu() {
        return this.nineSliceSliderBackgroundBorderX_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderY_FancyMenu(int borderY) {
        this.nineSliceSliderBackgroundBorderY_FancyMenu = borderY;
        this.nineSliceSliderBackgroundBorderTop_FancyMenu = borderY;
        this.nineSliceSliderBackgroundBorderBottom_FancyMenu = borderY;
    }

    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderY_FancyMenu() {
        return this.nineSliceSliderBackgroundBorderY_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderTop_FancyMenu(int borderTop) {
        this.nineSliceSliderBackgroundBorderTop_FancyMenu = borderTop;
    }

    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderTop_FancyMenu() {
        return this.nineSliceSliderBackgroundBorderTop_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderRight_FancyMenu(int borderRight) {
        this.nineSliceSliderBackgroundBorderRight_FancyMenu = borderRight;
    }

    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderRight_FancyMenu() {
        return this.nineSliceSliderBackgroundBorderRight_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderBottom_FancyMenu(int borderBottom) {
        this.nineSliceSliderBackgroundBorderBottom_FancyMenu = borderBottom;
    }

    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderBottom_FancyMenu() {
        return this.nineSliceSliderBackgroundBorderBottom_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderBackgroundBorderLeft_FancyMenu(int borderLeft) {
        this.nineSliceSliderBackgroundBorderLeft_FancyMenu = borderLeft;
    }

    @Unique
    @Override
    public int getNineSliceSliderBackgroundBorderLeft_FancyMenu() {
        return this.nineSliceSliderBackgroundBorderLeft_FancyMenu;
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
    public void setNineSliceSliderHandleBorderX_FancyMenu(int borderX) {
        this.nineSliceSliderHandleBorderX_FancyMenu = borderX;
        this.nineSliceSliderHandleBorderLeft_FancyMenu = borderX;
        this.nineSliceSliderHandleBorderRight_FancyMenu = borderX;
    }

    @Unique
    @Override
    public int getNineSliceSliderHandleBorderX_FancyMenu() {
        return this.nineSliceSliderHandleBorderX_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderHandleBorderY_FancyMenu(int borderY) {
        this.nineSliceSliderHandleBorderY_FancyMenu = borderY;
        this.nineSliceSliderHandleBorderTop_FancyMenu = borderY;
        this.nineSliceSliderHandleBorderBottom_FancyMenu = borderY;
    }

    @Unique
    @Override
    public int getNineSliceSliderHandleBorderY_FancyMenu() {
        return this.nineSliceSliderHandleBorderY_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderHandleBorderTop_FancyMenu(int borderTop) {
        this.nineSliceSliderHandleBorderTop_FancyMenu = borderTop;
    }

    @Unique
    @Override
    public int getNineSliceSliderHandleBorderTop_FancyMenu() {
        return this.nineSliceSliderHandleBorderTop_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderHandleBorderRight_FancyMenu(int borderRight) {
        this.nineSliceSliderHandleBorderRight_FancyMenu = borderRight;
    }

    @Unique
    @Override
    public int getNineSliceSliderHandleBorderRight_FancyMenu() {
        return this.nineSliceSliderHandleBorderRight_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderHandleBorderBottom_FancyMenu(int borderBottom) {
        this.nineSliceSliderHandleBorderBottom_FancyMenu = borderBottom;
    }

    @Unique
    @Override
    public int getNineSliceSliderHandleBorderBottom_FancyMenu() {
        return this.nineSliceSliderHandleBorderBottom_FancyMenu;
    }

    @Unique
    @Override
    public void setNineSliceSliderHandleBorderLeft_FancyMenu(int borderLeft) {
        this.nineSliceSliderHandleBorderLeft_FancyMenu = borderLeft;
    }

    @Unique
    @Override
    public int getNineSliceSliderHandleBorderLeft_FancyMenu() {
        return this.nineSliceSliderHandleBorderLeft_FancyMenu;
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
        if ((Object)this instanceof AbstractExtendedSlider slider && slider.getSliderBackgroundColorNormal() != null) return null;
        RenderableResource resource = GlobalCustomizationHandler.getCustomSliderBackground();
        if (resource != null) this.applyGlobalSliderBackgroundNineSlice_FancyMenu();
        return resource;
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
        if ((Object)this instanceof AbstractExtendedSlider slider && slider.getSliderBackgroundColorHighlighted() != null) return null;
        RenderableResource resource = GlobalCustomizationHandler.getCustomSliderBackground();
        if (resource != null) this.applyGlobalSliderBackgroundNineSlice_FancyMenu();
        return resource;
    }

    @Unique
    private void applyGlobalSliderBackgroundNineSlice_FancyMenu() {
        this.setNineSliceCustomSliderBackground_FancyMenu(GlobalCustomizationHandler.isGlobalSliderBackgroundNineSliceEnabled());
        this.setNineSliceSliderBackgroundBorderTop_FancyMenu(GlobalCustomizationHandler.getGlobalSliderBackgroundNineSliceBorderTop());
        this.setNineSliceSliderBackgroundBorderRight_FancyMenu(GlobalCustomizationHandler.getGlobalSliderBackgroundNineSliceBorderRight());
        this.setNineSliceSliderBackgroundBorderBottom_FancyMenu(GlobalCustomizationHandler.getGlobalSliderBackgroundNineSliceBorderBottom());
        this.setNineSliceSliderBackgroundBorderLeft_FancyMenu(GlobalCustomizationHandler.getGlobalSliderBackgroundNineSliceBorderLeft());
    }

    @Unique
    private CustomizableWidget getAsCustomizableWidgetFancyMenu() {
        return (CustomizableWidget)this;
    }

}
