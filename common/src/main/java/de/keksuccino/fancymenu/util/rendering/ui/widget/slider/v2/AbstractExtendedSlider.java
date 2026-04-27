package de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v2;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.customization.global.GlobalCustomizationHandler;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractSliderButton;
import de.keksuccino.fancymenu.util.ConsumingSupplier;
import de.keksuccino.fancymenu.util.VanillaEvents;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.SmoothRectangleRenderer;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableSlider;
import de.keksuccino.fancymenu.util.rendering.ui.widget.CustomizableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.IExtendedWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import de.keksuccino.fancymenu.util.resource.RenderableResource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;

@SuppressWarnings("unused")
public abstract class AbstractExtendedSlider extends AbstractSliderButton implements IExtendedWidget, NavigatableWidget, FancyMenuWidget {

    public static final Identifier SLIDER_SPRITE = Identifier.parse("widget/slider");
    public static final Identifier HIGHLIGHTED_SPRITE = Identifier.parse("widget/slider_highlighted");
    public static final Identifier SLIDER_HANDLE_SPRITE = Identifier.parse("widget/slider_handle");
    public static final Identifier SLIDER_HANDLE_HIGHLIGHTED_SPRITE = Identifier.parse("widget/slider_handle_highlighted");

    @Nullable
    protected DrawableColor sliderBackgroundColorNormal;
    @Nullable
    protected DrawableColor sliderBackgroundColorHighlighted;
    @Nullable
    protected DrawableColor sliderBorderColorNormal;
    @Nullable
    protected DrawableColor sliderBorderColorHighlighted;
    @Nullable
    protected DrawableColor sliderHandleColorNormal;
    @Nullable
    protected DrawableColor sliderHandleColorHover;
    @Nullable
    protected DrawableColor sliderHandleColorInactive;
    @NotNull
    protected DrawableColor labelColorNormal = DrawableColor.of(new Color(16777215));
    @NotNull
    protected DrawableColor labelColorInactive = DrawableColor.of(new Color(10526880));
    protected boolean labelShadow = true;
    protected boolean renderLabelWithUiBase = false;
    @Nullable
    protected SliderValueUpdateListener sliderValueUpdateListener;
    @NotNull
    protected ConsumingSupplier<AbstractExtendedSlider, Component> labelSupplier = slider -> Component.literal(slider.getValueDisplayText());
    protected boolean focusable = true;
    protected boolean navigatable = true;
    protected boolean roundedColorBackground = false;
    @Nullable
    protected ConsumingSupplier<AbstractExtendedSlider, Boolean> isActiveSupplier = null;
    protected boolean leftMouseDown = false;

    public AbstractExtendedSlider(int x, int y, int width, int height, Component label, double value) {
        super(x, y, width, height, label, value);
    }

    public Identifier getSprite() {
        return this.isFocused() && !((IMixinAbstractSliderButton)this).getCanChangeValueFancyMenu() ? HIGHLIGHTED_SPRITE : SLIDER_SPRITE;
    }

    public Identifier getHandleSprite() {
        return !this.isHovered && !((IMixinAbstractSliderButton)this).getCanChangeValueFancyMenu() ? SLIDER_HANDLE_SPRITE : SLIDER_HANDLE_HIGHLIGHTED_SPRITE;
    }

    protected void renderSliderWidget(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {

        this.extractBackground(graphics, mouseX, mouseY, partial);
        RenderingUtils.resetShaderColor(graphics);

        this.renderHandle(graphics, mouseX, mouseY, partial);
        RenderingUtils.resetShaderColor(graphics);

        this.renderLabel(graphics, mouseX, mouseY, partial);
        RenderingUtils.resetShaderColor(graphics);

    }

    @Override
    public void extractWidgetRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        if (this.isActiveSupplier != null) this.active = this.isActiveSupplier.get(this);
        this.renderSliderWidget(graphics, mouseX, mouseY, partial);
        this.handleCursor(graphics);
    }

    protected void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        boolean renderVanilla = this.renderColorBackground(graphics, mouseX, mouseY, partial);
        if (renderVanilla) renderVanilla = this.getAsCustomizableSlider().renderSliderBackgroundFancyMenu(graphics, this, this.getAccessor().getCanChangeValueFancyMenu());
        if (renderVanilla) this.renderVanillaBackground(graphics, mouseX, mouseY, partial);
    }

    /**
     * Returns if the slider should render its Vanilla background (true) or not (false).
     */
    protected boolean renderColorBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
        RenderingUtils.resetShaderColor(graphics);
        if ((this.isFocused() && !this.getAccessor().getCanChangeValueFancyMenu()) && (this.sliderBackgroundColorHighlighted != null)) {
            if (this.roundedColorBackground) {
                float radius = UIBase.getWidgetCornerRoundingRadius();
                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                        graphics,
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight(),
                        radius,
                        radius,
                        radius,
                        radius,
                        this.sliderBackgroundColorHighlighted.getColorInt(),
                        partial
                );
            } else {
                graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.sliderBackgroundColorHighlighted.getColorInt());
            }
            if (this.sliderBorderColorHighlighted != null) {
                if (this.roundedColorBackground) {
                    float radius = UIBase.getWidgetCornerRoundingRadius();
                    float borderThickness = 1.0F;
                    float borderRadius = radius > 0.0F ? radius + borderThickness : 0.0F;
                    SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(
                            graphics,
                            this.getX(),
                            this.getY(),
                            this.getWidth(),
                            this.getHeight(),
                            borderThickness,
                            borderRadius,
                            borderRadius,
                            borderRadius,
                            borderRadius,
                            this.sliderBorderColorHighlighted.getColorInt(),
                            partial
                    );
                } else {
                    UIBase.renderBorder(graphics, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 1, this.sliderBorderColorHighlighted.getColorInt(), true, true, true, true);
                }
            }
            return false;
        } else if (this.sliderBackgroundColorNormal != null) {
            if (this.roundedColorBackground) {
                float radius = UIBase.getWidgetCornerRoundingRadius();
                SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                        graphics,
                        this.getX(),
                        this.getY(),
                        this.getWidth(),
                        this.getHeight(),
                        radius,
                        radius,
                        radius,
                        radius,
                        this.sliderBackgroundColorNormal.getColorInt(),
                        partial
                );
            } else {
                graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.sliderBackgroundColorNormal.getColorInt());
            }
            if (this.sliderBorderColorNormal != null) {
                if (this.roundedColorBackground) {
                    float radius = UIBase.getWidgetCornerRoundingRadius();
                    float borderThickness = 1.0F;
                    float borderRadius = radius > 0.0F ? radius + borderThickness : 0.0F;
                    SmoothRectangleRenderer.renderSmoothBorderRoundAllCornersScaled(
                            graphics,
                            this.getX(),
                            this.getY(),
                            this.getWidth(),
                            this.getHeight(),
                            borderThickness,
                            borderRadius,
                            borderRadius,
                            borderRadius,
                            borderRadius,
                            this.sliderBorderColorNormal.getColorInt(),
                            partial
                    );
                } else {
                    UIBase.renderBorder(graphics, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 1, this.sliderBorderColorNormal.getColorInt(), true, true, true, true);
                }
            }
            return false;
        }
        return true;
    }

    protected void renderVanillaBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        de.keksuccino.fancymenu.util.rendering.RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, this.alpha);
        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
        de.keksuccino.fancymenu.util.rendering.RenderingUtils.defaultBlendFunc();
        com.mojang.blaze3d.opengl.GlStateManager._enableDepthTest();
        graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.getSprite(), this.getX(), this.getY(), this.getWidth(), this.getHeight());
        RenderingUtils.resetShaderColor(graphics);
    }

    protected void renderHandle(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        boolean renderVanilla = this.renderColorHandle(graphics, mouseX, mouseY, partial);
        if (renderVanilla) renderVanilla = this.getAsCustomizableWidget().renderCustomBackgroundFancyMenu(this, graphics, this.getHandleX(), this.getY(), this.getHandleWidth(), this.getHeight());
        if (renderVanilla) this.renderVanillaHandle(graphics, mouseX, mouseY, partial);
    }

    /**
     * Returns if the slider should render its Vanilla handle (true) or not (false).
     */
    protected boolean renderColorHandle(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
        int handleX = this.getHandleX();
        int handleWidth = this.getHandleWidth();
        if (this.active) {
            if (this.isHoveredOrFocused()) {
                if (this.sliderHandleColorHover != null) {
                    if (this.roundedColorBackground) {
                        float radius = UIBase.getWidgetCornerRoundingRadius();
                        SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                                graphics,
                                handleX,
                                this.getY(),
                                handleWidth,
                                this.getHeight(),
                                radius,
                                radius,
                                radius,
                                radius,
                                this.sliderHandleColorHover.getColorInt(),
                                partial
                        );
                    } else {
                        graphics.fill(handleX, this.getY(), handleX + handleWidth, this.getY() + this.getHeight(), this.sliderHandleColorHover.getColorInt());
                    }
                    return false;
                }
            } else {
                if (this.sliderHandleColorNormal != null) {
                    if (this.roundedColorBackground) {
                        float radius = UIBase.getWidgetCornerRoundingRadius();
                        SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                                graphics,
                                handleX,
                                this.getY(),
                                handleWidth,
                                this.getHeight(),
                                radius,
                                radius,
                                radius,
                                radius,
                                this.sliderHandleColorNormal.getColorInt(),
                                partial
                        );
                    } else {
                        graphics.fill(handleX, this.getY(), handleX + handleWidth, this.getY() + this.getHeight(), this.sliderHandleColorNormal.getColorInt());
                    }
                    return false;
                }
            }
        } else {
            if (this.sliderHandleColorInactive != null) {
                if (this.roundedColorBackground) {
                    float radius = UIBase.getWidgetCornerRoundingRadius();
                    SmoothRectangleRenderer.renderSmoothRectRoundAllCornersScaled(
                            graphics,
                            handleX,
                            this.getY(),
                            handleWidth,
                            this.getHeight(),
                            radius,
                            radius,
                            radius,
                            radius,
                            this.sliderHandleColorInactive.getColorInt(),
                            partial
                    );
                } else {
                    graphics.fill(handleX, this.getY(), handleX + handleWidth, this.getY() + this.getHeight(), this.sliderHandleColorInactive.getColorInt());
                }
                return false;
            }
        }
        return true;
    }

    protected void renderVanillaHandle(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        de.keksuccino.fancymenu.util.rendering.RenderingUtils.setShaderColor(graphics, 1.0F, 1.0F, 1.0F, this.alpha);
        com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
        de.keksuccino.fancymenu.util.rendering.RenderingUtils.defaultBlendFunc();
        com.mojang.blaze3d.opengl.GlStateManager._enableDepthTest();
        graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, this.getHandleSprite(), this.getHandleX(), this.getY(), this.getHandleWidth(), this.getHeight());
        RenderingUtils.resetShaderColor(graphics);
    }

    protected void renderLabel(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
        int textColor = this.active ? this.labelColorNormal.getColorInt() : this.labelColorInactive.getColorInt();
        int finalTextColor = RenderingUtils.replaceAlphaInColor(textColor, this.alpha);
        boolean labelShadowFinal = this.labelShadow && GlobalCustomizationHandler.isGlobalSliderLabelShadowEnabled();
        if (this.renderLabelWithUiBase) {
            this.renderScrollingLabelUiBase(this, graphics, 2, finalTextColor);
        } else {
            this.renderScrollingLabel(this, graphics, Minecraft.getInstance().font, 2, labelShadowFinal, finalTextColor);
        }
    }

    public int getHandleX() {
        return this.getX() + (int)(this.value * (double)(this.getWidth() - this.getHandleWidth()));
    }

    public int getHandleWidth() {
        return 8;
    }

    @Override
    public void updateMessage() {
        Component label = this.labelSupplier.get(this);
        if (label == null) label = Component.empty();
        this.setMessage(label);
    }

    @Override
    protected void applyValue() {
        if (this.sliderValueUpdateListener != null) {
            this.sliderValueUpdateListener.update(this, this.getValueDisplayText(), this.value);
        }
    }

    @NotNull
    public abstract String getValueDisplayText();

    public AbstractExtendedSlider setSliderValueUpdateListener(@Nullable SliderValueUpdateListener listener) {
        this.sliderValueUpdateListener = listener;
        this.updateMessage();
        return this;
    }

    public void setValue(double value) {
        double d0 = this.value;
        this.value = Mth.clamp(value, 0.0D, 1.0D);
        if (d0 != this.value) {
            this.applyValue();
        }
        this.updateMessage();
    }

    public double getValue() {
        return this.value;
    }

    public AbstractExtendedSlider setIsActiveSupplier(@Nullable ConsumingSupplier<AbstractExtendedSlider, Boolean> supplier) {
        this.isActiveSupplier = supplier;
        return this;
    }

    @Nullable
    public RenderableResource getHandleTextureNormal() {
        return this.getAsCustomizableWidget().getCustomBackgroundNormalFancyMenu();
    }

    public AbstractExtendedSlider setHandleTextureNormal(@Nullable RenderableResource texture) {
        this.getAsCustomizableWidget().setCustomBackgroundNormalFancyMenu(texture);
        return this;
    }

    @Nullable
    public RenderableResource getHandleTextureHover() {
        return this.getAsCustomizableWidget().getCustomBackgroundHoverFancyMenu();
    }

    public AbstractExtendedSlider setHandleTextureHover(@Nullable RenderableResource texture) {
        this.getAsCustomizableWidget().setCustomBackgroundHoverFancyMenu(texture);
        return this;
    }

    @Nullable
    public RenderableResource getHandleTextureInactive() {
        return this.getAsCustomizableWidget().getCustomBackgroundInactiveFancyMenu();
    }

    public AbstractExtendedSlider setHandleTextureInactive(@Nullable RenderableResource texture) {
        this.getAsCustomizableWidget().setCustomBackgroundInactiveFancyMenu(texture);
        return this;
    }

    @Nullable
    public RenderableResource getBackgroundTextureNormal() {
        return this.getAsCustomizableSlider().getCustomSliderBackgroundNormalFancyMenu();
    }

    public AbstractExtendedSlider setBackgroundTextureNormal(@Nullable RenderableResource texture) {
        this.getAsCustomizableSlider().setCustomSliderBackgroundNormalFancyMenu(texture);
        return this;
    }

    @Nullable
    public RenderableResource getBackgroundTextureHighlighted() {
        return this.getAsCustomizableSlider().getCustomSliderBackgroundHighlightedFancyMenu();
    }

    public AbstractExtendedSlider setBackgroundTextureHighlighted(@Nullable RenderableResource texture) {
        this.getAsCustomizableSlider().setCustomSliderBackgroundHighlightedFancyMenu(texture);
        return this;
    }

    @Nullable
    public DrawableColor getSliderBackgroundColorNormal() {
        return sliderBackgroundColorNormal;
    }

    public AbstractExtendedSlider setSliderBackgroundColorNormal(@Nullable DrawableColor sliderBackgroundColorNormal) {
        this.sliderBackgroundColorNormal = sliderBackgroundColorNormal;
        return this;
    }

    @Nullable
    public DrawableColor getSliderBackgroundColorHighlighted() {
        return sliderBackgroundColorHighlighted;
    }

    public AbstractExtendedSlider setSliderBackgroundColorHighlighted(@Nullable DrawableColor sliderBackgroundColorHighlighted) {
        this.sliderBackgroundColorHighlighted = sliderBackgroundColorHighlighted;
        return this;
    }

    @Nullable
    public DrawableColor getSliderBorderColorNormal() {
        return sliderBorderColorNormal;
    }

    public AbstractExtendedSlider setSliderBorderColorNormal(@Nullable DrawableColor sliderBorderColorNormal) {
        this.sliderBorderColorNormal = sliderBorderColorNormal;
        return this;
    }

    @Nullable
    public DrawableColor getSliderBorderColorHighlighted() {
        return sliderBorderColorHighlighted;
    }

    public AbstractExtendedSlider setSliderBorderColorHighlighted(@Nullable DrawableColor sliderBorderColorHighlighted) {
        this.sliderBorderColorHighlighted = sliderBorderColorHighlighted;
        return this;
    }

    @Nullable
    public DrawableColor getSliderHandleColorNormal() {
        return sliderHandleColorNormal;
    }

    public AbstractExtendedSlider setSliderHandleColorNormal(@Nullable DrawableColor sliderHandleColorNormal) {
        this.sliderHandleColorNormal = sliderHandleColorNormal;
        return this;
    }

    @Nullable
    public DrawableColor getSliderHandleColorHover() {
        return sliderHandleColorHover;
    }

    public AbstractExtendedSlider setSliderHandleColorHover(@Nullable DrawableColor sliderHandleColorHover) {
        this.sliderHandleColorHover = sliderHandleColorHover;
        return this;
    }

    @Nullable
    public DrawableColor getSliderHandleColorInactive() {
        return sliderHandleColorInactive;
    }

    public AbstractExtendedSlider setSliderHandleColorInactive(@Nullable DrawableColor sliderHandleColorInactive) {
        this.sliderHandleColorInactive = sliderHandleColorInactive;
        return this;
    }

    @NotNull
    public DrawableColor getLabelColorNormal() {
        return this.labelColorNormal;
    }

    public AbstractExtendedSlider setLabelColorNormal(@NotNull DrawableColor labelColorNormal) {
        this.labelColorNormal = labelColorNormal;
        return this;
    }

    @NotNull
    public DrawableColor getLabelColorInactive() {
        return this.labelColorInactive;
    }

    public AbstractExtendedSlider setLabelColorInactive(@NotNull DrawableColor labelColorInactive) {
        this.labelColorInactive = labelColorInactive;
        return this;
    }

    public boolean isLabelShadow() {
        return labelShadow;
    }

    public AbstractExtendedSlider setLabelShadow(boolean labelShadow) {
        this.labelShadow = labelShadow;
        return this;
    }

    public boolean isLabelRenderedWithUiBase() {
        return this.renderLabelWithUiBase;
    }

    public AbstractExtendedSlider setLabelRenderedWithUiBase(boolean renderLabelWithUiBase) {
        this.renderLabelWithUiBase = renderLabelWithUiBase;
        return this;
    }

    public boolean isRoundedColorBackgroundEnabled() {
        return this.roundedColorBackground;
    }

    public AbstractExtendedSlider setRoundedColorBackgroundEnabled(boolean roundedColorBackground) {
        this.roundedColorBackground = roundedColorBackground;
        return this;
    }

    @NotNull
    public ConsumingSupplier<AbstractExtendedSlider, Component> getLabelSupplier() {
        return this.labelSupplier;
    }

    public AbstractExtendedSlider setLabelSupplier(@NotNull ConsumingSupplier<AbstractExtendedSlider, Component> labelSupplier) {
        this.labelSupplier = labelSupplier;
        return this;
    }

    public IMixinAbstractSliderButton getAccessor() {
        return (IMixinAbstractSliderButton) this;
    }

    public CustomizableSlider getAsCustomizableSlider() {
        return (CustomizableSlider) this;
    }

    public CustomizableWidget getAsCustomizableWidget() {
        return (CustomizableWidget) this;
    }

    @Override
    public boolean isFocusable() {
        return focusable;
    }

    @Override
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    @Override
    public boolean isNavigatable() {
        return navigatable;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        this.navigatable = navigatable;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (!this.canClick()) return false;
        boolean handled = super.mouseClicked(event, isDoubleClick);
        if (event.button() == 0) this.leftMouseDown = handled;
        return handled;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.mouseClicked(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button, 0), false);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean wasLeftMouseDown = this.leftMouseDown;
        this.leftMouseDown = false;
        if (!wasLeftMouseDown || (event.button() != 0)) return false;
        return super.mouseReleased(event);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.mouseReleased(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button, 0));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!this.leftMouseDown) return false;
        return super.mouseDragged(event, dragX, dragY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.mouseDragged(VanillaEvents.mouseButtonEvent(mouseX, mouseY, button, 0), dragX, dragY);
    }

    protected boolean canClick() {
        return (this.isHovered() && this.isActive() && this.visible);
    }

    @FunctionalInterface
    public interface SliderValueUpdateListener {
        void update(@NotNull AbstractExtendedSlider slider, @NotNull String valueDisplayText, double value);
    }

}
