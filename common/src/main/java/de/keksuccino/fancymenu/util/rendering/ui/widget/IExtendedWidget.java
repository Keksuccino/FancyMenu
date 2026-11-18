package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public interface IExtendedWidget {

    default void renderScrollingLabel(@NotNull AbstractWidget widget, @NotNull GuiGraphicsExtractor graphics, @NotNull Font font, int spaceLeftRight, boolean labelShadow, int textColor) {
        int xMin = widget.getX() + spaceLeftRight;
        int xMax = widget.getX() + widget.getWidth() - spaceLeftRight;
        float scale = this.resolveLabelScale(widget);
        if (scale == 0.0F) return;
        WidgetLabelAlignment alignment = this.resolveLabelAlignment(widget);
        //Use getMessage() here to not break custom label handling of CustomizableWidget
        if (scale == 1.0F) {
            this.renderScrollingLabelInternal(graphics, font, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), labelShadow, textColor, alignment);
        } else {
            this.renderScrollingLabelInternalScaled(graphics, font, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), labelShadow, textColor, scale, alignment);
        }
    }

    default void renderScrollingLabelInternal(@NotNull GuiGraphicsExtractor graphics, Font font, @NotNull Component text, int xMin, int yMin, int xMax, int yMax, boolean labelShadow, int textColor, @NotNull WidgetLabelAlignment alignment) {
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
            graphics.text(font, text, xMin - (int)textPosX, textPosY, textColor, labelShadow);
            graphics.disableScissor();
        } else {
            graphics.text(font, text, (int)alignment.getLabelX(xMin, xMax, textWidth, 1.0F), textPosY, textColor, labelShadow);
        }
    }

    default void renderScrollingLabelUiBase(@NotNull AbstractWidget widget, @NotNull GuiGraphicsExtractor graphics, int spaceLeftRight, int textColor) {
        int xMin = widget.getX() + spaceLeftRight;
        int xMax = widget.getX() + widget.getWidth() - spaceLeftRight;
        float scale = this.resolveLabelScale(widget);
        if (scale == 0.0F) return;
        WidgetLabelAlignment alignment = this.resolveLabelAlignment(widget);
        //Use getMessage() here to not break custom label handling of CustomizableWidget
        if (scale == 1.0F) {
            this.renderScrollingLabelInternalUiBase(graphics, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), textColor, alignment);
        } else if (UIBase.shouldUseMinecraftFontForUIRendering()) {
            this.renderScrollingLabelInternalScaled(graphics, Minecraft.getInstance().font, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), false, textColor, scale, alignment);
        } else {
            this.renderScrollingLabelInternalUiBaseScaled(graphics, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), textColor, scale, alignment);
        }
    }

    default void renderScrollingLabelInternalUiBase(@NotNull GuiGraphicsExtractor graphics, @NotNull Component text, int xMin, int yMin, int xMax, int yMax, int textColor, @NotNull WidgetLabelAlignment alignment) {
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
            UIBase.renderText(graphics, text, alignment.getLabelX(xMin, xMax, textWidth, 1.0F), textPosY, textColor);
        }
    }

    default void renderScrollingLabelInternalScaled(@NotNull GuiGraphicsExtractor graphics, @NotNull Font font, @NotNull Component text, int xMin, int yMin, int xMax, int yMax, boolean labelShadow, int textColor, float scale, @NotNull WidgetLabelAlignment alignment) {
        if (scale == 0.0F) return;
        float invScale = 1.0F / scale;
        float scaledMinX = xMin * invScale;
        float scaledMaxX = xMax * invScale;
        float scaledMinY = yMin * invScale;
        float scaledMaxY = yMax * invScale;
        int textWidth = font.width(text);
        float textPosY = (scaledMinY + scaledMaxY - font.lineHeight) / 2F + 1F;
        float maxTextWidth = scaledMaxX - scaledMinX;
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        if (textWidth > maxTextWidth) {
            float diffTextWidth = textWidth - maxTextWidth;
            double scrollTime = (double) Util.getMillis() / 1000.0D;
            double scrollDuration = Math.max(diffTextWidth * 0.5D, 3.0D);
            double scrollAlpha = Math.sin((Math.PI / 2D) * Math.cos((Math.PI * 2D) * scrollTime / scrollDuration)) / 2.0D + 0.5D;
            double textOffset = Mth.lerp(scrollAlpha, 0.0D, diffTextWidth);
            graphics.enableScissor(xMin, yMin, xMax, yMax);
            graphics.text(font, text, (int)(scaledMinX - (float) textOffset), (int)textPosY, textColor, labelShadow);
            graphics.disableScissor();
        } else {
            float textPosX = alignment.getLabelX(scaledMinX, scaledMaxX, textWidth, scale);
            graphics.text(font, text, (int)textPosX, (int)textPosY, textColor, labelShadow);
        }
        graphics.pose().popMatrix();
    }

    default void renderScrollingLabelInternalUiBaseScaled(@NotNull GuiGraphicsExtractor graphics, @NotNull Component text, int xMin, int yMin, int xMax, int yMax, int textColor, float scale, @NotNull WidgetLabelAlignment alignment) {
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
            UIBase.renderText(graphics, text, alignment.getLabelX(xMin, xMax, textWidth, 1.0F), textPosY, textColor, textSize);
        }
    }

    /**
     * Returns if the label gets rendered with the UI base, which only widgets of FancyMenu's own UI do.
     */
    default boolean isLabelRenderedWithUiBase() {
        return false;
    }

    default float resolveLabelScale(@NotNull AbstractWidget widget) {
        if (widget instanceof CustomizableWidget customizable) {
            return customizable.resolveLabelScaleFancyMenu();
        }
        return 1.0F;
    }

    @NotNull
    default WidgetLabelAlignment resolveLabelAlignment(@NotNull AbstractWidget widget) {
        if (widget instanceof CustomizableWidget customizable) {
            return customizable.resolveLabelAlignmentFancyMenu();
        }
        return WidgetLabelAlignment.CENTERED;
    }

}
