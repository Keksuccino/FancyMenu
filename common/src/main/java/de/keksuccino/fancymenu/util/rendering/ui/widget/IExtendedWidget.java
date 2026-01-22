package de.keksuccino.fancymenu.util.rendering.ui.widget;

import de.keksuccino.fancymenu.util.rendering.ui.UIBase;
import net.minecraft.Util;
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
        //Use getMessage() here to not break custom label handling of CustomizableWidget
        this.renderScrollingLabelInternal(graphics, font, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), labelShadow, textColor);
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
        //Use getMessage() here to not break custom label handling of CustomizableWidget
        this.renderScrollingLabelInternalUiBase(graphics, widget.getMessage(), xMin, widget.getY(), xMax, widget.getY() + widget.getHeight(), textColor);
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

}
