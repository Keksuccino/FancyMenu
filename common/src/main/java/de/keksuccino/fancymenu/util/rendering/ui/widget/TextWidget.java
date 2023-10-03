package de.keksuccino.fancymenu.util.rendering.ui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import de.keksuccino.fancymenu.util.rendering.DrawableColor;
import de.keksuccino.fancymenu.util.rendering.RenderingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TextWidget extends AbstractWidget implements UniqueWidget, NavigatableWidget {

    @Nullable
    protected String widgetIdentifier;
    @NotNull
    protected TextAlignment alignment = TextAlignment.LEFT;
    @NotNull
    protected DrawableColor baseColor = DrawableColor.WHITE;
    protected boolean shadow = true;
    @NotNull
    protected Font font;

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
    }

    @Override
    public void renderWidget(@NotNull PoseStack pose, int mouseX, int mouseY, float partial) {
        int textWidth = this.getTextWidth();
        int textX = this.getX();
        int textY = this.getY();
        if (this.alignment == TextAlignment.CENTER) {
            textX = this.getX() + (this.getWidth() / 2) - (textWidth / 2);
        }
        if (this.alignment == TextAlignment.RIGHT) {
            textX = this.getX() + this.getWidth() - textWidth;
        }
        RenderingUtils.resetShaderColor();
        if (this.shadow) {
            this.font.drawShadow(pose, this.getMessage(), textX, textY, this.baseColor.getColorInt());
        } else {
            this.font.draw(pose, this.getMessage(), textX, textY, this.baseColor.getColorInt());
        }
        RenderingUtils.resetShaderColor();
    }

    public int getTextWidth() {
        return this.font.width(this.getMessage().getVisualOrderText());
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
        return this;
    }

    public TextWidget centerWidget(@NotNull Screen parent) {
        this.setX((parent.width / 2) - (this.getWidth() / 2));
        return this;
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
    protected void updateWidgetNarration(@NotNull NarrationElementOutput var1) {
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

    public enum TextAlignment {
        LEFT,
        RIGHT,
        CENTER
    }

}
