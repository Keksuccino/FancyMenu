package de.keksuccino.fancymenu.menu.fancy.helper.ui.compat;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AbstractGui extends Gui {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void blit(int x, int y, float u, float v, int width, int height, float textureWidth, float textureHeight) {
        drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, textureWidth, textureHeight);
    }

    public static void fill(int left, int top, int right, int bottom, int color) {
        drawRect(left, top, right, bottom, color);
    }

    public static int drawFormattedString(FontRenderer font, ITextComponent text, int x, int y, int color, boolean shadow) {
        int xText = x;
        for (ITextComponent c : text) {
            int colorFinal = color;
            Style style = c.getStyle();
            if ((style instanceof TextStyle) && (((TextStyle)style).getColorRGB() != -1)) {
                colorFinal = ((TextStyle)style).getColorRGB();
            }
            String formatted = "";
            String s = c.getUnformattedComponentText();
            if (!s.isEmpty()) {
                formatted += style.getFormattingCode();
                formatted += s;
                formatted += TextFormatting.RESET;
            }
            xText = font.drawString(formatted, xText, y, colorFinal, shadow);
        }
        return xText;
    }

    public static int drawFormattedString(FontRenderer font, ITextComponent text, int x, int y, int color) {
        return drawFormattedString(font, text, x, y, color, false);
    }

    public static int drawFormattedStringWithShadow(FontRenderer font, ITextComponent text, int x, int y, int color) {
        return drawFormattedString(font, text, x, y, color, true);
    }

}
