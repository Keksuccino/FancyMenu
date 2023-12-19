package de.keksuccino.fancymenu.util.rendering.ui.widget.slider.v1;

import com.mojang.blaze3d.systems.RenderSystem;
import de.keksuccino.fancymenu.mixin.mixins.common.client.IMixinAbstractSliderButton;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import de.keksuccino.fancymenu.util.rendering.ui.widget.NavigatableWidget;
import de.keksuccino.fancymenu.util.rendering.ui.widget.UniqueWidget;
import de.keksuccino.konkrete.input.MouseInput;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.awt.*;
import java.util.function.Consumer;

@Deprecated
public abstract class ExtendedSliderButton extends AbstractSliderButton implements UniqueWidget, NavigatableWidget {

    protected static final ResourceLocation SLIDER_LOCATION = new ResourceLocation("textures/gui/slider.png");

    protected static boolean leftDownGlobal = false;

    public boolean handleClick;
    public boolean enableRightClick = false;
    public boolean ignoreBlockedInput = false;
    public boolean ignoreGlobalLeftMouseDown = false;
    protected String messagePrefix = null;
    protected String messageSuffix = null;
    protected Consumer<ExtendedSliderButton> applyValueCallback;
    @Nullable
    protected DrawableColor backgroundColor = null;
    @Nullable
    protected DrawableColor borderColor = null;
    @Nullable
    protected DrawableColor handleColorNormal = null;
    @Nullable
    protected DrawableColor handleColorHover = null;
    @NotNull
    protected DrawableColor labelColorNormal = DrawableColor.of(new Color(16777215));
    @NotNull
    protected DrawableColor labelColorInactive = DrawableColor.of(new Color(10526880));
    protected boolean labelShadow = true;
    @Nullable
    protected String identifier;
    protected boolean focusable = true;
    protected boolean navigatable = true;

    protected boolean leftDownNotHovered = false;
    protected boolean leftDownThis = false;

    @Deprecated
    public ExtendedSliderButton(int x, int y, int width, int height, boolean handleClick, double value, Consumer<ExtendedSliderButton> applyValueCallback) {
        super(x, y, width, height, CommonComponents.EMPTY, value);
        this.handleClick = handleClick;
        this.applyValueCallback = applyValueCallback;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        this.renderBackground(graphics);
        this.renderHandle(graphics);
        int labelColorInt = this.active ? this.labelColorNormal.getColorInt() : this.labelColorInactive.getColorInt();
        this.renderScrollingLabel(graphics, Minecraft.getInstance().font, 2, labelColorInt | Mth.ceil(this.alpha * 255.0F) << 24);
        RenderingUtils.resetShaderColor(graphics);
    }

    protected void renderHandle(@NotNull GuiGraphics graphics) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        int handleX = this.getX() + (int)(this.value * (double)(this.width - 8));
        DrawableColor c = this.getHandleRenderColor();
        if (c == null) {
            graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
            graphics.blitNineSliced(SLIDER_LOCATION, handleX, this.getY(), 8, 20, 20, 4, 200, 20, 0, this.getHandleTextureY());
        } else {
            graphics.fill(handleX, this.getY(), handleX + 8, this.getY() + this.getHeight(), RenderingUtils.replaceAlphaInColor(c.getColorInt(), this.alpha));
        }
        RenderingUtils.resetShaderColor(graphics);
    }

    @Nullable
    protected DrawableColor getHandleRenderColor() {
        if (this.isHovered && (this.handleColorHover != null)) return this.handleColorHover;
        return this.handleColorNormal;
    }

    protected void renderBackground(@NotNull GuiGraphics graphics) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        if (this.backgroundColor == null) {
            graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
            graphics.blitNineSliced(SLIDER_LOCATION, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 20, 4, 200, 20, 0, this.getTextureY());
        } else {
            int borderOffset = (this.borderColor != null) ? 1 : 0;
            graphics.fill(this.getX() + borderOffset, this.getY() + borderOffset, this.getX() + this.getWidth() - borderOffset, this.getY() + this.getHeight() - borderOffset, RenderingUtils.replaceAlphaInColor(this.backgroundColor.getColorInt(), this.alpha));
            if (this.borderColor != null) {
                UIBase.renderBorder(graphics, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 1, this.borderColor, true, true, true, true);
            }
        }
        RenderingUtils.resetShaderColor(graphics);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {

        if (this.visible) {

            this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

            if (!this.isHoveredOrFocused() && MouseInput.isLeftMouseDown()) {
                this.leftDownNotHovered = true;
            }
            if (!MouseInput.isLeftMouseDown()) {
                this.leftDownNotHovered = false;
            }

            if (this.handleClick) {
                if (this.isHoveredOrFocused() && (MouseInput.isLeftMouseDown() || (this.enableRightClick && MouseInput.isRightMouseDown())) && (!leftDownGlobal || this.ignoreGlobalLeftMouseDown) && !leftDownNotHovered && !this.isInputBlocked() && this.active && this.visible) {
                    if (!this.leftDownThis) {
                        this.onClick(mouseX, mouseY);
                        leftDownGlobal = true;
                        this.leftDownThis = true;
                    }
                }
                if (!MouseInput.isLeftMouseDown() && !(MouseInput.isRightMouseDown() && this.enableRightClick)) {
                    leftDownGlobal = false;
                    if (this.leftDownThis) {
                        this.onRelease(mouseX, mouseY);
                    }
                    this.leftDownThis = false;
                }
                if (this.leftDownThis) {
                    this.onDrag(mouseX, mouseY, 0, 0);
                }
            }

        }

        super.render(graphics, mouseX, mouseY, partial);

    }

    protected void renderScrollingLabel(@NotNull GuiGraphics graphics, @NotNull Font font, int spaceLeftRight, int textColor) {
        int xMin = this.getX() + spaceLeftRight;
        int xMax = this.getX() + this.getWidth() - spaceLeftRight;
        this.renderScrollingLabelInternal(graphics, font, this.getMessage(), xMin, this.getY(), xMax, this.getY() + this.getHeight(), textColor);
    }

    protected void renderScrollingLabelInternal(@NotNull GuiGraphics graphics, Font font, @NotNull Component text, int xMin, int yMin, int xMax, int yMax, int textColor) {
        int textWidth = font.width(text);
        int textPosY = (yMin + yMax - 9) / 2 + 1;
        int maxTextWidth = xMax - xMin;
        if (textWidth > maxTextWidth) {
            int diffTextWidth = textWidth - maxTextWidth;
            double scrollTime = (double) Util.getMillis() / 1000.0D;
            double $$13 = Math.max((double)diffTextWidth * 0.5D, 3.0D);
            double $$14 = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * scrollTime / $$13)) / 2.0D + 0.5D;
            double textPosX = Mth.lerp($$14, 0.0D, diffTextWidth);
            graphics.enableScissor(xMin, yMin, xMax, yMax);
            graphics.drawString(font, text, xMin - (int)textPosX, textPosY, textColor, this.labelShadow);
            graphics.disableScissor();
        } else {
            graphics.drawString(font, text, (int)(((xMin + xMax) / 2F) - (font.width(text) / 2F)), textPosY, textColor, this.labelShadow);
        }
    }

    @Nullable
    public DrawableColor getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(@Nullable DrawableColor backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Nullable
    public DrawableColor getBorderColor() {
        return this.borderColor;
    }

    public void setBorderColor(@Nullable DrawableColor borderColor) {
        this.borderColor = borderColor;
    }

    @Nullable
    public DrawableColor getHandleColorNormal() {
        return this.handleColorNormal;
    }

    public void setHandleColorNormal(@Nullable DrawableColor handleColorNormal) {
        this.handleColorNormal = handleColorNormal;
    }

    @Nullable
    public DrawableColor getHandleColorHover() {
        return this.handleColorHover;
    }

    public void setHandleColorHover(@Nullable DrawableColor handleColorHover) {
        this.handleColorHover = handleColorHover;
    }

    @NotNull
    public DrawableColor getLabelColorNormal() {
        return this.labelColorNormal;
    }

    public void setLabelColorNormal(@NotNull DrawableColor labelColorNormal) {
        this.labelColorNormal = labelColorNormal;
    }

    @NotNull
    public DrawableColor getLabelColorInactive() {
        return this.labelColorInactive;
    }

    public void setLabelColorInactive(@NotNull DrawableColor labelColorInactive) {
        this.labelColorInactive = labelColorInactive;
    }

    public boolean isLabelShadow() {
        return this.labelShadow;
    }

    public void setLabelShadow(boolean labelShadow) {
        this.labelShadow = labelShadow;
    }

    public boolean canChangeValue() {
        return this.getAccessor().getCanChangeValueFancyMenu();
    }

    public IMixinAbstractSliderButton getAccessor() {
        return (IMixinAbstractSliderButton) this;
    }

    protected int getTextureY() {
        int $$0 = this.isFocused() && !this.canChangeValue() ? 1 : 0;
        return $$0 * 20;
    }

    protected int getHandleTextureY() {
        int $$0 = !this.isHovered && !this.canChangeValue() ? 2 : 3;
        return $$0 * 20;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double d1, double d2) {
        super.onDrag(mouseX, mouseY, d1, d2);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        super.onClick(mouseX, mouseY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        super.onRelease(mouseX, mouseY);
    }

    @Override
    protected void applyValue() {
        if (this.applyValueCallback != null) {
            this.applyValueCallback.accept(this);
        }
    }

    @Override
    public void updateMessage() {
        String s = "";
        if (this.messagePrefix != null) {
            s += this.messagePrefix;
        }
        s += this.getSliderMessageWithoutPrefixSuffix();
        if (this.messageSuffix != null) {
            s += this.messageSuffix;
        }
        this.setMessage(Component.literal(s));
    }

    public abstract String getSliderMessageWithoutPrefixSuffix();

    public void setLabelPrefix(String prefix) {
        this.messagePrefix = prefix;
        this.updateMessage();
    }

    public void setLabelSuffix(String suffix) {
        this.messageSuffix = suffix;
        this.updateMessage();
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

    protected boolean isInputBlocked() {
        if (this.ignoreBlockedInput) {
            return false;
        }
        return MouseInput.isVanillaInputBlocked();
    }

    @Override
    public @Nullable String getWidgetIdentifierFancyMenu() {
        return this.identifier;
    }

    @Override
    public ExtendedSliderButton setWidgetIdentifierFancyMenu(@Nullable String identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!this.focusable) {
            super.setFocused(false);
            return;
        }
        super.setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        if (!this.focusable) return false;
        return super.isFocused();
    }

    @Override
    public boolean isFocusable() {
        return this.focusable;
    }

    @Override
    public void setFocusable(boolean focusable) {
        this.focusable = focusable;
    }

    @Override
    public boolean isNavigatable() {
        return this.navigatable;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        this.navigatable = navigatable;
    }

}
