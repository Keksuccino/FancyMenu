package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import de.keksuccino.fancymenu.util.rendering.gui.GuiGraphics;
import de.keksuccino.fancymenu.util.rendering.ui.widget.slider.FancyMenuWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TextWidget extends ModernAbstractWidget implements UniqueWidget, NavigatableWidget, FancyMenuWidget {

    @Nullable
    protected String widgetIdentifier;
    @NotNull
    protected TextAlignment alignment = TextAlignment.LEFT;
    @NotNull
    protected DrawableColor baseColor = DrawableColor.WHITE;
    protected boolean shadow = true;
    @NotNull
    protected Font font;
    protected float scale = 1.0F;

    @NotNull
    public static TextWidget empty(int x, int y, int width) {
        return new TextWidget(x, y, width, 9, Minecraft.getInstance().font, Component.empty());
    }

    @NotNull
    public static TextWidget of(@NotNull Component text, int x, int y, int width) {
        return new TextWidget(x, y, width, 9, Minecraft.getInstance().font, text);
    }

    @NotNull
    public static TextWidget of(@NotNull String text, int x, int y, int width) {
        return of(Component.literal(text), x, y, width);
    }

    public TextWidget(int x, int y, int width, int height, @NotNull Font font, @NotNull Component text) {
        super(x, y, width, height, text);
        this.font = font;
        this.updateIntrinsicSize();
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        double drawX = this.getRenderX();
        double drawY = this.getRenderY();
        float currentScale = this.scale;
        RenderingUtils.resetShaderColor(graphics);
        graphics.pose().pushPose();
        if (currentScale != 1.0F) {
            graphics.pose().scale(currentScale, currentScale, 1.0F);
            drawX /= currentScale;
            drawY /= currentScale;
        }
        graphics.drawString(this.font, this.getMessage(), Mth.floor(drawX), Mth.floor(drawY), this.baseColor.getColorInt(), this.shadow);
        graphics.pose().popPose();
        RenderingUtils.resetShaderColor(graphics);
    }

    public int getTextWidth() {
        return Mth.ceil(this.getScaledTextWidth());
    }

    public double getScaledTextWidth() {
        return this.font.width(this.getMessage().getVisualOrderText()) * this.scale;
    }

    public double getScaledTextHeight() {
        return this.font.lineHeight * this.scale;
    }

    public double getRenderX() {
        double textWidth = this.getScaledTextWidth();
        double x = this.getX();
        if (this.alignment == TextAlignment.CENTER) {
            x = this.getX() + (this.getWidth() / 2.0) - (textWidth / 2.0);
        } else if (this.alignment == TextAlignment.RIGHT) {
            x = this.getX() + this.getWidth() - textWidth;
        }
        return x;
    }

    public double getRenderY() {
        return this.getY();
    }

    public float getScale() {
        return this.scale;
    }

    public TextWidget setScale(float scale) {
        this.scale = Math.max(0.0001F, scale);
        this.updateIntrinsicSize();
        return this;
    }

    public @NotNull TextAlignment getTextAlignment() {
        return this.alignment;
    }

    public TextWidget setTextAlignment(@NotNull TextAlignment alignment) {
        this.alignment = alignment;
        return this;
    }

    public @NotNull DrawableColor getBaseColor() {
        return this.baseColor;
    }

    public TextWidget setBaseColor(@NotNull DrawableColor baseColor) {
        this.baseColor = baseColor;
        return this;
    }

    public boolean isShadowEnabled() {
        return this.shadow;
    }

    public TextWidget setShadowEnabled(boolean enabled) {
        this.shadow = enabled;
        return this;
    }

    public @NotNull Font getFont() {
        return this.font;
    }

    public TextWidget setFont(@NotNull Font font) {
        this.font = font;
        this.updateIntrinsicSize();
        return this;
    }

    public TextWidget centerWidget(@NotNull Screen parent) {
        this.x = (parent.width / 2) - (this.getWidth() / 2);
        return this;
    }

    public boolean isTextHovered(double mouseX, double mouseY) {
        double x = this.getRenderX();
        double y = this.getRenderY();
        double width = this.getScaledTextWidth();
        double height = this.getScaledTextHeight();
        return mouseX >= x && mouseX <= (x + width) && mouseY >= y && mouseY <= (y + height);
    }

    @Nullable
    public Style getStyleAtMouseX(double mouseX) {
        double left = this.getRenderX();
        double right = left + this.getScaledTextWidth();
        if (mouseX < left || mouseX > right) {
            return null;
        }
        float safeScale = Math.max(0.0001F, this.scale);
        double relative = (mouseX - left) / safeScale;
        return this.font.getSplitter().componentStyleAtWidth(this.getMessage(), Mth.floor(relative));
    }

    @Override
    public TextWidget setWidgetIdentifierFancyMenu(@Nullable String identifier) {
        this.widgetIdentifier = identifier;
        return this;
    }

    @Nullable
    @Override
    public String getWidgetIdentifierFancyMenu() {
        return this.widgetIdentifier;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput var1) {
    }

    @Override
    public boolean isFocusable() {
        return false;
    }

    @Override
    public void setFocusable(boolean focusable) {
        throw new RuntimeException("TextWidgets are not focusable!");
    }

    @Override
    public boolean isNavigatable() {
        return false;
    }

    @Override
    public void setNavigatable(boolean navigatable) {
        throw new RuntimeException("TextWidgets are not navigatable!");
    }

    @Override
    public void playDownSound(@NotNull SoundManager $$0) {
        //no click sound
    }

    protected void updateIntrinsicSize() {
        this.height = Math.max(1, Mth.ceil(this.font.lineHeight * this.scale));
    }

    public enum TextAlignment {
        LEFT,
        RIGHT,
        CENTER
    }

}
