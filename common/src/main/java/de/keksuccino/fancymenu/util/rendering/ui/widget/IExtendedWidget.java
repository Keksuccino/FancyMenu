package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public interface IExtendedWidget {

    default void renderScrollingLabel(@NotNull AbstractWidget widget, @NotNull GuiGraphics graphics, @NotNull Font font, int spaceLeftRight, boolean labelShadow, int textColor) {
        int xMin = widget.getX() + spaceLeftRight;
        int xMax = widget.getX() + widget.getWidth() - spaceLeftRight;
        float scale = this.resolveLabelScale(widget);
        if (scale == 0.0F) return;
        //Use getMessage() here to not break custom label handling of CustomizableWidget
        if (scale == 1.0F) {
            this.renderScrollingLabelInternal(graphics, font, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), labelShadow, textColor);
        } else {
            this.renderScrollingLabelInternalScaled(graphics, font, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), labelShadow, textColor, scale);
        }
    }

    default void renderScrollingLabelInternal(@NotNull GuiGraphics graphics, Font font, @NotNull Component text, int xMin, int yMin, int xMax, int yMax, boolean labelShadow, int textColor) {
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
            graphics.drawString(font, text, xMin - (int)textPosX, textPosY, textColor, labelShadow);
            graphics.disableScissor();
        } else {
            graphics.drawString(font, text, (int)(((xMin + xMax) / 2F) - (font.width(text) / 2F)), textPosY, textColor, labelShadow);
        }
    }

    default void renderScrollingLabelUiBase(@NotNull AbstractWidget widget, @NotNull GuiGraphics graphics, int spaceLeftRight, int textColor) {
        int xMin = widget.getX() + spaceLeftRight;
        int xMax = widget.getX() + widget.getWidth() - spaceLeftRight;
        float scale = this.resolveLabelScale(widget);
        if (scale == 0.0F) return;
        //Use getMessage() here to not break custom label handling of CustomizableWidget
        if (scale == 1.0F) {
            this.renderScrollingLabelInternalUiBase(graphics, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), textColor);
        } else if (UIBase.shouldUseMinecraftFontForUIRendering()) {
            this.renderScrollingLabelInternalScaled(graphics, Minecraft.getInstance().font, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), false, textColor, scale);
        } else {
            this.renderScrollingLabelInternalUiBaseScaled(graphics, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), textColor, scale);
        }
    }

    default void renderScrollingLabelInternalUiBase(@NotNull GuiGraphics graphics, @NotNull Component text, int xMin, int yMin, int xMax, int yMax, int textColor) {
        float textWidth = UIBase.getUITextWidthNormal(text);
        float textHeight = UIBase.getUITextHeightNormal();
        float textPosY = (yMin + yMax - textHeight) / 2F;
        float maxTextWidth = xMax - xMin;
        if (textWidth > maxTextWidth) {
            float diffTextWidth = textWidth - maxTextWidth;
            double scrollTime = (double) Util.getMillis() / 1000.0D;
            double scrollDuration = Math.max(diffTextWidth * 0.5D, 3.0D);
            double scrollAlpha = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * scrollTime / scrollDuration)) / 2.0D + 0.5D;
            double textOffset = Mth.lerp(scrollAlpha, 0.0D, diffTextWidth);
            graphics.enableScissor(xMin, yMin, xMax, yMax);
            UIBase.renderText(graphics, text, xMin - (float) textOffset, textPosY, textColor);
            graphics.disableScissor();
        } else {
            float centerX = (xMin + xMax) / 2F;
            float textPosX = Mth.clamp(centerX, xMin + (textWidth / 2F), xMax - (textWidth / 2F)) - (textWidth / 2F);
            UIBase.renderText(graphics, text, textPosX, textPosY, textColor);
        }
    }

    default void renderScrollingLabelInternalScaled(@NotNull GuiGraphics graphics, @NotNull Font font, @NotNull Component text, int xMin, int yMin, int xMax, int yMax, boolean labelShadow, int textColor, float scale) {
        if (scale == 0.0F) return;
        float invScale = 1.0F / scale;
        float scaledMinX = xMin * invScale;
        float scaledMaxX = xMax * invScale;
        float scaledMinY = yMin * invScale;
        float scaledMaxY = yMax * invScale;
        int textWidth = font.width(text);
        float textPosY = (scaledMinY + scaledMaxY - font.lineHeight) / 2F + 1F;
        float maxTextWidth = scaledMaxX - scaledMinX;
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        if (textWidth > maxTextWidth) {
            float diffTextWidth = textWidth - maxTextWidth;
            double scrollTime = (double) Util.getMillis() / 1000.0D;
            double scrollDuration = Math.max(diffTextWidth * 0.5D, 3.0D);
            double scrollAlpha = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * scrollTime / scrollDuration)) / 2.0D + 0.5D;
            double textOffset = Mth.lerp(scrollAlpha, 0.0D, diffTextWidth);
            graphics.enableScissor(xMin, yMin, xMax, yMax);
            graphics.drawString(font, text, (int)(scaledMinX - (float) textOffset), (int)textPosY, textColor, labelShadow);
            graphics.disableScissor();
        } else {
            float textPosX = ((scaledMinX + scaledMaxX) / 2F) - (textWidth / 2F);
            graphics.drawString(font, text, (int)textPosX, (int)textPosY, textColor, labelShadow);
        }
        graphics.pose().popPose();
    }

    default void renderScrollingLabelInternalUiBaseScaled(@NotNull GuiGraphics graphics, @NotNull Component text, int xMin, int yMin, int xMax, int yMax, int textColor, float scale) {
        float textSize = UIBase.getUITextSizeNormal() * scale;
        float textWidth = UIBase.getUITextWidth(text, textSize);
        float textHeight = UIBase.getUITextHeight(textSize);
        float textPosY = (yMin + yMax - textHeight) / 2F;
        float maxTextWidth = xMax - xMin;
        if (textWidth > maxTextWidth) {
            float diffTextWidth = textWidth - maxTextWidth;
            double scrollTime = (double) Util.getMillis() / 1000.0D;
            double scrollDuration = Math.max(diffTextWidth * 0.5D, 3.0D);
            double scrollAlpha = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * scrollTime / scrollDuration)) / 2.0D + 0.5D;
            double textOffset = Mth.lerp(scrollAlpha, 0.0D, diffTextWidth);
            graphics.enableScissor(xMin, yMin, xMax, yMax);
            UIBase.renderText(graphics, text, xMin - (float) textOffset, textPosY, textColor, textSize);
            graphics.disableScissor();
        } else {
            float centerX = (xMin + xMax) / 2F;
            float textPosX = Mth.clamp(centerX, xMin + (textWidth / 2F), xMax - (textWidth / 2F)) - (textWidth / 2F);
            UIBase.renderText(graphics, text, textPosX, textPosY, textColor, textSize);
        }
    }

    default float resolveLabelScale(@NotNull AbstractWidget widget) {
        if (widget instanceof CustomizableWidget customizable) {
            return customizable.resolveLabelScaleFancyMenu();
        }
        return 1.0F;
    }

}
